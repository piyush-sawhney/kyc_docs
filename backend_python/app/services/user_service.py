from uuid import UUID

from fastapi import Request
from sqlmodel import delete, func, select
from sqlmodel.ext.asyncio.session import AsyncSession

from app.core.security import (
    blind_index,
    create_access_token,
    generate_qr_code,
    generate_recovery_codes,
    generate_totp_secret,
    get_totp_uri,
    hash_recovery_code,
)
from app.models.permission import Permission
from app.models.recovery_code import RecoveryCode
from app.models.user import User
from app.models.user_permission import UserPermission
from app.schemas.user import UserCreate, UserUpdate
from app.services.audit_service import AuditService


class UserService:
    def __init__(self, db: AsyncSession):
        self.db = db

    @staticmethod
    def _get_client_info(
        request: Request | None,
    ) -> tuple[str | None, str | None]:
        if request is None:
            return None, None
        ip = request.client.host if request.client else None
        ua = request.headers.get("user-agent")
        return ip, ua

    async def create_user(self, data: UserCreate) -> User:
        email_hash = blind_index(data.email)
        existing = await self.db.exec(
            select(User).where(User.email_hash == email_hash, User.is_deleted == False)
        )
        if existing.first():
            raise ValueError("A user with this email already exists")

        totp_secret = generate_totp_secret()
        user = User(
            email=data.email,
            full_name=data.full_name,
            role=data.role,
            totp_secret=totp_secret,
            is_active=False,
            totp_verified=False,
        )
        self.db.add(user)
        await self.db.flush()
        return user

    async def get_users(
        self, skip: int = 0, limit: int = 100, include_deleted: bool = False
    ) -> tuple[list[User], int]:
        query = select(User)
        count_query = select(func.count(User.id))

        if not include_deleted:
            query = query.where(User.is_deleted == False)
            count_query = count_query.where(User.is_deleted == False)

        total_result = await self.db.exec(count_query)
        total = total_result.one()

        result = await self.db.exec(query.offset(skip).limit(limit))
        users = list(result.all())

        return users, total

    async def get_user(self, user_id: UUID) -> User | None:
        result = await self.db.exec(
            select(User).where(User.id == user_id, User.is_deleted == False)
        )
        return result.first()

    async def get_user_any_status(self, user_id: UUID) -> User | None:
        result = await self.db.exec(select(User).where(User.id == user_id))
        return result.first()

    async def update_user(self, user_id: UUID, data: UserUpdate) -> User | None:
        result = await self.db.exec(
            select(User).where(User.id == user_id, User.is_deleted == False)
        )
        user = result.first()
        if not user:
            return None

        update_data = data.model_dump(exclude_unset=True)
        for field, value in update_data.items():
            setattr(user, field, value)

        self.db.add(user)
        await self.db.flush()
        return user

    async def delete_user(self, user_id: UUID) -> bool:
        result = await self.db.exec(
            select(User).where(User.id == user_id, User.is_deleted == False)
        )
        user = result.first()
        if not user:
            return False

        user.is_deleted = True
        user.is_active = False
        self.db.add(user)
        await self.db.flush()
        return True

    async def get_user_permissions(self, user_id: UUID) -> list[Permission]:
        result = await self.db.exec(
            select(Permission)
            .join(UserPermission, Permission.id == UserPermission.permission_id)
            .where(UserPermission.user_id == user_id)
        )
        return list(result.all())

    async def set_user_permissions(
        self, user_id: UUID, permission_ids: list[UUID]
    ) -> None:
        await self.db.exec(
            delete(UserPermission).where(UserPermission.user_id == user_id)
        )

        for perm_id in permission_ids:
            up = UserPermission(user_id=user_id, permission_id=perm_id)
            self.db.add(up)

        await self.db.flush()

    async def get_enroll_token(self, user_id: UUID) -> str:
        result = await self.db.exec(
            select(User).where(User.id == user_id, User.is_deleted == False)
        )
        user = result.first()
        if not user:
            raise ValueError("User not found")

        return create_access_token(
            subject=str(user.id),
            email=user.email,
            role=user.role,
            expires_delta=None,
        )

    async def get_deleted_users(
        self, skip: int = 0, limit: int = 100
    ) -> tuple[list[User], int]:
        query = select(User).where(User.is_deleted)
        count_query = select(func.count(User.id)).where(User.is_deleted)

        total_result = await self.db.exec(count_query)
        total = total_result.one()

        result = await self.db.exec(query.offset(skip).limit(limit))
        users = list(result.all())

        return users, total

    async def deactivate_user(
        self,
        user_id: UUID,
        current_user_id: UUID,
        request: Request | None = None,
    ) -> User:
        result = await self.db.exec(
            select(User).where(User.id == user_id, User.is_deleted == False)
        )
        target = result.first()
        if not target:
            raise ValueError("User not found")

        if target.id == current_user_id:
            raise ValueError("Cannot deactivate yourself")

        if target.role == "admin":
            active_admins_result = await self.db.exec(
                select(User).where(
                    User.is_deleted == False,
                    User.is_active,
                    User.role == "admin",
                )
            )
            active_admins = list(active_admins_result.all())
            if len(active_admins) <= 1:
                raise ValueError("Cannot deactivate the last active admin")

        old_values = {"is_active": target.is_active}
        target.is_active = False
        target.modified_by = current_user_id
        self.db.add(target)
        await self.db.flush()

        ip, ua = self._get_client_info(request)
        audit = AuditService(self.db)
        await audit.log(
            action="DEACTIVATE",
            entity_type="user",
            entity_id=user_id,
            description=f"Deactivated user '{target.full_name}' ({target.email})",
            user_id=current_user_id,
            old_values=old_values,
            new_values={"is_active": False},
            ip_address=ip,
            user_agent=ua,
        )

        return target

    async def reactivate_user(
        self,
        user_id: UUID,
        current_user_id: UUID,
        request: Request | None = None,
    ) -> User:
        result = await self.db.exec(
            select(User).where(User.id == user_id, User.is_deleted == False)
        )
        target = result.first()
        if not target:
            raise ValueError("User not found")

        old_values = {"is_active": target.is_active}
        target.is_active = True
        target.modified_by = current_user_id
        self.db.add(target)
        await self.db.flush()

        ip, ua = self._get_client_info(request)
        audit = AuditService(self.db)
        await audit.log(
            action="REACTIVATE",
            entity_type="user",
            entity_id=user_id,
            description=f"Reactivated user '{target.full_name}' ({target.email})",
            user_id=current_user_id,
            old_values=old_values,
            new_values={"is_active": True},
            ip_address=ip,
            user_agent=ua,
        )

        return target

    async def restore_user(
        self,
        user_id: UUID,
        current_user_id: UUID,
        request: Request | None = None,
    ) -> User:
        result = await self.db.exec(
            select(User).where(User.id == user_id, User.is_deleted)
        )
        target = result.first()
        if not target:
            raise ValueError("Deleted user not found")

        old_values = {
            "is_deleted": target.is_deleted,
            "deleted_at": (
                target.deleted_at.isoformat() if target.deleted_at else None
            ),
        }
        target.is_deleted = False
        target.deleted_at = None
        target.is_active = True
        target.modified_by = current_user_id
        self.db.add(target)
        await self.db.flush()

        ip, ua = self._get_client_info(request)
        audit = AuditService(self.db)
        await audit.log(
            action="RESTORE",
            entity_type="user",
            entity_id=user_id,
            description=f"Restored user '{target.full_name}' ({target.email})",
            user_id=current_user_id,
            old_values=old_values,
            new_values={
                "is_deleted": False,
                "is_active": True,
                "deleted_at": None,
            },
            ip_address=ip,
            user_agent=ua,
        )

        return target

    async def update_user_role(
        self,
        user_id: UUID,
        role: str,
        current_user_id: UUID,
        request: Request | None = None,
    ) -> User:
        result = await self.db.exec(
            select(User).where(User.id == user_id, User.is_deleted == False)
        )
        target = result.first()
        if not target:
            raise ValueError("User not found")

        if target.id == current_user_id:
            raise ValueError("Cannot change your own role")

        old_role = target.role
        if old_role == "admin" and role == "user":
            active_admins_result = await self.db.exec(
                select(User).where(
                    User.is_deleted == False,
                    User.is_active,
                    User.role == "admin",
                )
            )
            active_admins = list(active_admins_result.all())
            if len(active_admins) <= 1:
                raise ValueError("Cannot demote the last active admin")

        target.role = role
        target.modified_by = current_user_id
        self.db.add(target)
        await self.db.flush()

        ip, ua = self._get_client_info(request)
        audit = AuditService(self.db)
        await audit.log(
            action="ROLE_CHANGE",
            entity_type="user",
            entity_id=user_id,
            description=(
                f"Changed role of '{target.full_name}' from {old_role} to {role}"
            ),
            user_id=current_user_id,
            old_values={"role": old_role},
            new_values={"role": role},
            ip_address=ip,
            user_agent=ua,
        )

        return target

    async def admin_re_enroll_totp(
        self,
        user_id: UUID,
        current_user_id: UUID,
        request: Request | None = None,
    ) -> str:
        result = await self.db.exec(
            select(User).where(User.id == user_id, User.is_deleted == False)
        )
        target = result.first()
        if not target:
            raise ValueError("User not found")

        if target.id == current_user_id:
            raise ValueError("Cannot re-enroll yourself")

        secret = generate_totp_secret()
        target.totp_secret = secret
        target.totp_verified = False
        target.is_active = False
        target.modified_by = current_user_id
        self.db.add(target)
        await self.db.flush()

        totp_uri = get_totp_uri(secret, target.email)
        qr_data_url = generate_qr_code(totp_uri)

        ip, ua = self._get_client_info(request)
        audit = AuditService(self.db)
        await audit.log(
            action="TOTP_REENROLL",
            entity_type="user",
            entity_id=user_id,
            description=(
                f"Admin re-enrolled authenticator for user "
                f"'{target.full_name}' ({target.email})"
            ),
            user_id=current_user_id,
            new_values={"totp_verified": False, "is_active": False},
            ip_address=ip,
            user_agent=ua,
        )

        return qr_data_url

    async def admin_generate_recovery_codes(
        self,
        user_id: UUID,
        current_user_id: UUID,
        request: Request | None = None,
    ) -> list[str]:
        result = await self.db.exec(
            select(User).where(User.id == user_id, User.is_deleted == False)
        )
        target = result.first()
        if not target:
            raise ValueError("User not found")

        if target.role != "admin":
            raise ValueError("Recovery codes are only available for admin users")

        await self.db.exec(delete(RecoveryCode).where(RecoveryCode.user_id == user_id))

        codes = generate_recovery_codes(5)
        for code in codes:
            code_hash = hash_recovery_code(code)
            rc = RecoveryCode(user_id=user_id, code_hash=code_hash)
            self.db.add(rc)

        await self.db.flush()

        ip, ua = self._get_client_info(request)
        audit = AuditService(self.db)
        await audit.log(
            action="RECOVERY_CODES",
            entity_type="user",
            entity_id=user_id,
            description=(
                f"Admin generated recovery codes for "
                f"'{target.full_name}' ({target.email})"
            ),
            user_id=current_user_id,
            new_values={"generated": True},
            ip_address=ip,
            user_agent=ua,
        )

        return codes
