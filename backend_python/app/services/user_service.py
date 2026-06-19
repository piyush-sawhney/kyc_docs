from uuid import UUID

from sqlmodel import delete, func, select
from sqlmodel.ext.asyncio.session import AsyncSession

from app.core.security import (
    blind_index,
    create_access_token,
    generate_totp_secret,
)
from app.models.permission import Permission
from app.models.user import User
from app.models.user_permission import UserPermission
from app.schemas.user import UserCreate, UserUpdate


class UserService:
    def __init__(self, db: AsyncSession):
        self.db = db

    async def create_user(self, data: UserCreate) -> User:
        email_hash = blind_index(data.email)
        existing = await self.db.exec(
            select(User).where(User.email_hash == email_hash, not User.is_deleted)
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
            query = query.where(not User.is_deleted)
            count_query = count_query.where(not User.is_deleted)

        total_result = await self.db.exec(count_query)
        total = total_result.one()

        result = await self.db.exec(query.offset(skip).limit(limit))
        users = list(result.all())

        return users, total

    async def get_user(self, user_id: UUID) -> User | None:
        result = await self.db.exec(
            select(User).where(User.id == user_id, not User.is_deleted)
        )
        return result.first()

    async def update_user(self, user_id: UUID, data: UserUpdate) -> User | None:
        result = await self.db.exec(
            select(User).where(User.id == user_id, not User.is_deleted)
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
            select(User).where(User.id == user_id, not User.is_deleted)
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
            select(User).where(User.id == user_id, not User.is_deleted)
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
