from datetime import timedelta
from uuid import UUID

from sqlmodel import select
from sqlmodel.ext.asyncio.session import AsyncSession

from app.core.config import settings
from app.core.security import (
    create_access_token,
    generate_qr_code,
    generate_recovery_codes,
    generate_totp_secret,
    get_totp_uri,
    hash_recovery_code,
    verify_token,
    verify_totp_code,
)
from app.models.document_type import DocumentType
from app.models.permission import Permission
from app.models.recovery_code import RecoveryCode
from app.models.user import User
from app.models.user_permission import UserPermission

DEFAULT_DOCUMENT_TYPES = [
    "PAN",
    "Aadhar",
    "Passport",
    "Driving License",
    "Voter ID",
    "OCI",
]


class SetupService:
    def __init__(self, db: AsyncSession):
        self.db = db

    async def needs_setup(self) -> bool:
        result = await self.db.exec(select(User.id).limit(1))
        return result.first() is None

    async def init_setup(self, email: str, full_name: str) -> dict:
        if not await self.needs_setup():
            raise ValueError("System is already set up")

        # Seed default permissions
        await self._seed_default_permissions()

        # Seed default document types
        await self._seed_document_types()

        # Create admin user
        totp_secret = generate_totp_secret()
        totp_uri = get_totp_uri(totp_secret, email)
        qr_data_url = generate_qr_code(totp_uri)

        admin = User(
            email=email,
            full_name=full_name,
            role="admin",
            totp_secret=totp_secret,
            is_active=False,
            totp_verified=False,
        )
        self.db.add(admin)
        await self.db.flush()

        # Create setup token
        setup_token = create_access_token(
            subject=str(admin.id),
            email=admin.email,
            role=admin.role,
            expires_delta=timedelta(minutes=30),
        )

        return {
            "qr_data_url": qr_data_url,
            "setup_token": setup_token,
        }

    async def verify_setup(self, setup_token: str, totp_code: str) -> dict:
        payload = verify_token(setup_token)
        if not payload:
            raise ValueError("Invalid or expired setup token")

        user_id = payload.get("sub")
        if not user_id:
            raise ValueError("Invalid setup token")

        result = await self.db.exec(select(User).where(User.id == UUID(user_id)))
        admin = result.first()

        if not admin:
            raise ValueError("User not found")

        if not admin.totp_secret:
            raise ValueError("TOTP not initialized")

        if not verify_totp_code(admin.totp_secret, totp_code):
            raise ValueError("Invalid verification code")

        # Generate recovery codes
        recovery_codes = generate_recovery_codes(5)
        for code in recovery_codes:
            code_hash = hash_recovery_code(code)
            recovery_code = RecoveryCode(user_id=admin.id, code_hash=code_hash)
            self.db.add(recovery_code)

        # Mark TOTP as verified (but stay inactive until confirm)
        admin.totp_verified = True
        self.db.add(admin)
        await self.db.flush()

        # Create confirm token (short-lived, for the confirm step)
        confirm_token = create_access_token(
            subject=str(admin.id),
            email=admin.email,
            role=admin.role,
            expires_delta=timedelta(minutes=60),
        )

        return {
            "confirm_token": confirm_token,
            "user": {
                "id": str(admin.id),
                "email": admin.email,
                "full_name": admin.full_name,
                "role": admin.role,
            },
            "recovery_codes": recovery_codes,
        }

    async def confirm_setup(self, confirm_token: str) -> dict:
        payload = verify_token(confirm_token)
        if not payload:
            raise ValueError("Invalid or expired confirmation token")

        user_id = payload.get("sub")
        if not user_id:
            raise ValueError("Invalid confirmation token")

        result = await self.db.exec(select(User).where(User.id == UUID(user_id)))
        admin = result.first()

        if not admin:
            raise ValueError("User not found")

        if admin.is_active:
            raise ValueError("User is already active")

        if not admin.totp_verified:
            raise ValueError("TOTP not verified")

        # Assign all permissions
        await self._assign_all_permissions(admin.id)

        # Activate user
        admin.is_active = True
        self.db.add(admin)
        await self.db.flush()

        # Create access token
        token = create_access_token(
            subject=str(admin.id),
            email=admin.email,
            role=admin.role,
            expires_delta=timedelta(minutes=settings.JWT_ACCESS_TOKEN_EXPIRE_MINUTES),
        )

        return {
            "token": token,
            "user": {
                "id": str(admin.id),
                "email": admin.email,
                "full_name": admin.full_name,
                "role": admin.role,
            },
        }

    async def _seed_default_permissions(self) -> None:
        existing = await self.db.exec(select(Permission).limit(1))
        if existing.first():
            return

        default_permissions = [
            # Clients
            ("client:create", "Create Clients", "Clients"),
            ("client:read", "View Clients", "Clients"),
            ("client:update", "Edit Clients", "Clients"),
            ("client:delete", "Delete Clients", "Clients"),
            # Documents
            ("document:upload", "Upload Documents", "Documents"),
            ("document:view", "View Documents", "Documents"),
            ("document:view_number", "View Document Numbers", "Documents"),
            ("document:view_file", "Open Document File", "Documents"),
            ("document:update", "Replace Document File", "Documents"),
            ("document:edit_image", "Edit Images (Rotate/Crop/Optimize)", "Documents"),
            ("document:delete", "Delete Documents", "Documents"),
            # Users
            ("user:view", "View Users", "Users"),
            ("user:create", "Create Users", "Users"),
            ("user:manage", "Edit / Activate Users", "Users"),
            ("user:reset_password", "Reset User Passwords", "Users"),
            # System
            ("audit:view", "View Audit Logs", "System"),
            ("permission:manage", "Manage Permissions", "System"),
        ]

        for key, label, group in default_permissions:
            perm = Permission(key=key, label=label, group=group)
            self.db.add(perm)

        await self.db.flush()

    async def _seed_document_types(self) -> None:
        existing = await self.db.exec(select(DocumentType).limit(1))
        if existing.first():
            return

        for name in DEFAULT_DOCUMENT_TYPES:
            dt = DocumentType(name=name, is_active=True)
            self.db.add(dt)

        await self.db.flush()

    async def _assign_all_permissions(self, user_id: UUID) -> None:
        result = await self.db.exec(select(Permission.id))
        permission_ids = result.all()

        for perm_id in permission_ids:
            up = UserPermission(user_id=user_id, permission_id=perm_id)
            self.db.add(up)

        await self.db.flush()
