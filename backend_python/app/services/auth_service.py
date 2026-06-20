from datetime import timedelta
from uuid import UUID

from sqlmodel import delete, select
from sqlmodel.ext.asyncio.session import AsyncSession

from app.core.config import settings
from app.core.security import (
    blind_index,
    create_access_token,
    generate_qr_code,
    generate_recovery_codes,
    generate_totp_secret,
    get_totp_uri,
    hash_recovery_code,
    verify_recovery_code,
    verify_token,
    verify_totp_code,
)
from app.models.recovery_code import RecoveryCode
from app.models.user import User


class AuthService:
    def __init__(self, db: AsyncSession):
        self.db = db

    async def login_init(self, email: str) -> dict:
        email_hash = blind_index(email)
        result = await self.db.exec(
            select(User).where(User.email_hash == email_hash, not User.is_deleted)
        )
        user = result.first()

        if not user:
            return {"enrolled": False}

        if not user.is_active and user.totp_verified:
            return {"enrolled": True, "needs_confirm": True}

        if not user.totp_verified:
            totp_secret = generate_totp_secret()
            totp_uri = get_totp_uri(totp_secret, email)
            qr_data_url = generate_qr_code(totp_uri)

            user.totp_secret = totp_secret
            self.db.add(user)
            await self.db.flush()

            enroll_token = create_access_token(
                subject=str(user.id),
                email=user.email,
                role=user.role,
                expires_delta=timedelta(minutes=10),
            )

            return {
                "enrolled": False,
                "enroll_token": enroll_token,
                "qr_data_url": qr_data_url,
            }

        return {"enrolled": True}

    async def resume_setup(self, email: str, totp_code: str) -> dict:
        email_hash = blind_index(email)
        result = await self.db.exec(
            select(User).where(User.email_hash == email_hash, not User.is_deleted)
        )
        user = result.first()

        if not user:
            raise ValueError("User not found")

        if user.is_active:
            raise ValueError("User is already active")

        if not user.totp_verified or not user.totp_secret:
            raise ValueError("TOTP not enrolled")

        if not verify_totp_code(user.totp_secret, totp_code):
            raise ValueError("Invalid TOTP code")

        # Delete old unused recovery codes
        await self.db.exec(delete(RecoveryCode).where(RecoveryCode.user_id == user.id))

        # Generate fresh recovery codes
        recovery_codes = generate_recovery_codes(5)
        for code in recovery_codes:
            code_hash = hash_recovery_code(code)
            rc = RecoveryCode(user_id=user.id, code_hash=code_hash)
            self.db.add(rc)
        await self.db.flush()

        confirm_token = create_access_token(
            subject=str(user.id),
            email=user.email,
            role=user.role,
            expires_delta=timedelta(minutes=60),
        )

        return {
            "confirm_token": confirm_token,
            "recovery_codes": recovery_codes,
            "user": {
                "id": str(user.id),
                "email": user.email,
                "full_name": user.full_name,
                "role": user.role,
            },
        }

    async def login(self, email: str, totp_code: str) -> dict:
        email_hash = blind_index(email)
        result = await self.db.exec(
            select(User).where(User.email_hash == email_hash, not User.is_deleted)
        )
        user = result.first()

        if not user:
            raise ValueError("User not found")

        if not user.is_active:
            raise ValueError("Account is deactivated")

        if not user.totp_verified or not user.totp_secret:
            raise ValueError("TOTP not enrolled")

        if not verify_totp_code(user.totp_secret, totp_code):
            raise ValueError("Invalid TOTP code")

        token = create_access_token(
            subject=str(user.id),
            email=user.email,
            role=user.role,
            expires_delta=timedelta(minutes=settings.JWT_ACCESS_TOKEN_EXPIRE_MINUTES),
        )

        recovery_codes_result = await self.db.exec(
            select(RecoveryCode).where(
                RecoveryCode.user_id == user.id, not RecoveryCode.is_used
            )
        )
        recovery_codes_missing = len(recovery_codes_result.all()) < 2

        return {
            "token": token,
            "user": {
                "id": str(user.id),
                "email": user.email,
                "full_name": user.full_name,
                "role": user.role,
            },
            "recovery_codes_missing": recovery_codes_missing,
        }

    async def recovery_login(self, email: str, recovery_code: str) -> dict:
        email_hash = blind_index(email)
        result = await self.db.exec(
            select(User).where(User.email_hash == email_hash, not User.is_deleted)
        )
        user = result.first()

        if not user:
            raise ValueError("User not found")

        if not user.is_active:
            raise ValueError("Account is deactivated")

        codes_result = await self.db.exec(
            select(RecoveryCode).where(
                RecoveryCode.user_id == user.id, not RecoveryCode.is_used
            )
        )
        codes = codes_result.all()

        matched = None
        for code in codes:
            if verify_recovery_code(recovery_code, code.code_hash):
                matched = code
                break

        if not matched:
            raise ValueError("Invalid recovery code")

        matched.is_used = True
        self.db.add(matched)
        await self.db.flush()

        token = create_access_token(
            subject=str(user.id),
            email=user.email,
            role=user.role,
            expires_delta=timedelta(minutes=settings.JWT_ACCESS_TOKEN_EXPIRE_MINUTES),
        )

        remaining = await self.db.exec(
            select(RecoveryCode).where(
                RecoveryCode.user_id == user.id, not RecoveryCode.is_used
            )
        )
        recovery_codes_missing = len(remaining.all()) < 2

        return {
            "token": token,
            "user": {
                "id": str(user.id),
                "email": user.email,
                "full_name": user.full_name,
                "role": user.role,
            },
            "recovery_codes_missing": recovery_codes_missing,
        }

    async def totp_enroll(self, enroll_token: str, totp_code: str) -> dict:
        payload = verify_token(enroll_token)
        if not payload:
            raise ValueError("Invalid or expired enrollment token")

        user_id = payload.get("sub")
        if not user_id:
            raise ValueError("Invalid enrollment token")

        result = await self.db.exec(select(User).where(User.id == UUID(user_id)))
        user = result.first()

        if not user:
            raise ValueError("User not found")

        if not user.totp_secret:
            raise ValueError("TOTP not initialized")

        if not verify_totp_code(user.totp_secret, totp_code):
            raise ValueError("Invalid TOTP code")

        recovery_codes = generate_recovery_codes(5)
        for code in recovery_codes:
            code_hash = hash_recovery_code(code)
            rc = RecoveryCode(user_id=user.id, code_hash=code_hash)
            self.db.add(rc)

        user.totp_verified = True
        user.is_active = True
        self.db.add(user)
        await self.db.flush()

        token = create_access_token(
            subject=str(user.id),
            email=user.email,
            role=user.role,
            expires_delta=timedelta(minutes=settings.JWT_ACCESS_TOKEN_EXPIRE_MINUTES),
        )

        return {
            "token": token,
            "user": {
                "id": str(user.id),
                "email": user.email,
                "full_name": user.full_name,
                "role": user.role,
            },
            "recovery_codes": recovery_codes,
        }

    async def get_qr(self, email: str) -> str:
        email_hash = blind_index(email)
        result = await self.db.exec(
            select(User).where(User.email_hash == email_hash, not User.is_deleted)
        )
        user = result.first()

        if not user:
            raise ValueError("User not found")

        if not user.totp_secret:
            totp_secret = generate_totp_secret()
            user.totp_secret = totp_secret
            self.db.add(user)
            await self.db.flush()

        totp_uri = get_totp_uri(user.totp_secret, email)
        return generate_qr_code(totp_uri)

    async def get_recovery_codes(self, user_id: UUID) -> list[str]:
        result = await self.db.exec(
            select(RecoveryCode).where(
                RecoveryCode.user_id == user_id, not RecoveryCode.is_used
            )
        )
        return [code.code_hash for code in result.all()]

    async def generate_new_recovery_codes(self, user_id: UUID) -> list[str]:
        await self.db.exec(delete(RecoveryCode).where(RecoveryCode.user_id == user_id))

        codes = generate_recovery_codes(5)
        for code in codes:
            code_hash = hash_recovery_code(code)
            rc = RecoveryCode(user_id=user_id, code_hash=code_hash)
            self.db.add(rc)

        await self.db.flush()
        return codes
