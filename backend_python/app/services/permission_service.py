from uuid import UUID

from sqlmodel import delete as sqldelete
from sqlmodel import select
from sqlmodel.ext.asyncio.session import AsyncSession

from app.models.permission import Permission
from app.models.user_permission import UserPermission


class PermissionService:
    def __init__(self, db: AsyncSession):
        self.db = db

    async def seed_defaults(self) -> None:
        existing = await self.db.exec(select(Permission).limit(1))
        if existing.first():
            return

        default_permissions = [
            ("client:create", "Create Clients", "Clients"),
            ("client:read", "View Clients", "Clients"),
            ("client:update", "Edit Clients", "Clients"),
            ("client:delete", "Delete Clients", "Clients"),
            ("document:upload", "Upload Documents", "Documents"),
            ("document:view", "View Documents", "Documents"),
            ("document:view_number", "View Document Numbers", "Documents"),
            ("document:view_file", "Open Document File", "Documents"),
            ("document:update", "Replace Document File", "Documents"),
            ("document:edit_image", "Edit Images (Rotate/Crop/Optimize)", "Documents"),
            ("document:delete", "Delete Documents", "Documents"),
            ("user:view", "View Users", "Users"),
            ("user:create", "Create Users", "Users"),
            ("user:manage", "Edit / Activate Users", "Users"),
            ("user:reset_password", "Reset User Passwords", "Users"),
            ("audit:view", "View Audit Logs", "System"),
            ("permission:manage", "Manage Permissions", "System"),
        ]

        for key, label, group in default_permissions:
            perm = Permission(key=key, label=label, group=group)
            self.db.add(perm)

        await self.db.flush()

    async def get_all_permissions(self) -> list[Permission]:
        result = await self.db.exec(
            select(Permission).order_by(Permission.group, Permission.key)
        )
        return list(result.all())

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
            sqldelete(UserPermission).where(UserPermission.user_id == user_id)
        )

        for perm_id in permission_ids:
            up = UserPermission(user_id=user_id, permission_id=perm_id)
            self.db.add(up)

        await self.db.flush()

    async def assign_all_to_user(self, user_id: UUID) -> None:
        result = await self.db.exec(select(Permission.id))
        rows = result.all()

        for row in rows:
            perm_id = row[0] if isinstance(row, tuple) else row
            up = UserPermission(user_id=user_id, permission_id=perm_id)
            self.db.add(up)

        await self.db.flush()
