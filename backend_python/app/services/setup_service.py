import uuid
from datetime import timedelta

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

        # Seed default permissions (idempotent)
        await self._seed_default_permissions()

        # Seed default document types (idempotent)
        await self._seed_document_types()

        # Generate TOTP secret + QR — no DB writes
        setup_id = uuid.uuid4()
        totp_secret = generate_totp_secret()
        totp_uri = get_totp_uri(totp_secret, email)
        qr_data_url = generate_qr_code(totp_uri)

        setup_token = create_access_token(
            subject=str(setup_id),
            email=email,
            full_name=full_name,
            role="admin",
            totp_secret=totp_secret,
            expires_delta=timedelta(minutes=30),
        )

        return {
            "qr_data_url": qr_data_url,
            "setup_token": setup_token,
        }

    async def complete_setup(self, setup_token: str, totp_code: str) -> dict:
        payload = verify_token(setup_token)
        if not payload:
            raise ValueError("Invalid or expired setup token")

        setup_id = payload.get("sub")
        email = payload.get("email")
        full_name = payload.get("full_name")
        role = payload.get("role")
        totp_secret = payload.get("totp_secret")

        if not all([setup_id, email, full_name, role, totp_secret]):
            raise ValueError("Invalid setup token")

        if not verify_totp_code(totp_secret, totp_code):
            raise ValueError("Invalid verification code")

        # Generate recovery codes
        recovery_codes = generate_recovery_codes(5)

        # Create user atomically
        admin = User(
            id=uuid.UUID(setup_id),
            email=email,
            full_name=full_name,
            role=role,
            totp_secret=totp_secret,
            created_by=uuid.UUID(setup_id),
            is_active=True,
            totp_verified=True,
        )
        self.db.add(admin)
        await self.db.flush()

        # Create recovery codes
        for code in recovery_codes:
            code_hash = hash_recovery_code(code)
            rc = RecoveryCode(user_id=admin.id, code_hash=code_hash)
            self.db.add(rc)

        # Assign all permissions
        await self._assign_all_permissions(admin.id)

        await self.db.commit()

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
            "recovery_codes": recovery_codes,
        }

    async def _seed_default_permissions(self) -> None:
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
            ("user:deleted", "View Deleted Users", "Users"),
            ("user:create", "Create Users", "Users"),
            ("user:edit", "Edit Users", "Users"),
            ("user:change_role", "Change User Roles", "Users"),
            ("user:deactivate", "Deactivate Users", "Users"),
            ("user:reactivate", "Reactivate Users", "Users"),
            ("user:delete", "Delete Users", "Users"),
            ("user:restore", "Restore Users", "Users"),
            ("user:re-enroll", "Re-enroll Users", "Users"),
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

    async def _assign_all_permissions(self, user_id: uuid.UUID) -> None:
        result = await self.db.exec(select(Permission.id))
        permission_ids = result.all()

        for perm_id in permission_ids:
            up = UserPermission(user_id=user_id, permission_id=perm_id)
            self.db.add(up)

        await self.db.flush()
