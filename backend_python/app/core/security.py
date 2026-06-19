import base64
import secrets
from datetime import UTC, datetime, timedelta
from io import BytesIO

import bcrypt
import qrcode
from jose import JWTError, jwt
from pyotp import TOTP

from app.core.config import settings
from app.core.encryption import field_encryption


def create_access_token(
    subject: str,
    email: str,
    role: str,
    expires_delta: timedelta | None = None,
) -> str:
    if expires_delta:
        expire = datetime.now(UTC) + expires_delta
    else:
        expire = datetime.now(UTC) + timedelta(
            minutes=settings.JWT_ACCESS_TOKEN_EXPIRE_MINUTES
        )

    to_encode = {
        "sub": subject,
        "email": email,
        "role": role,
        "exp": expire,
        "iat": datetime.now(UTC),
    }
    return jwt.encode(
        to_encode, settings.JWT_SECRET_KEY, algorithm=settings.JWT_ALGORITHM
    )


def verify_token(token: str) -> dict | None:
    try:
        return jwt.decode(
            token,
            settings.JWT_SECRET_KEY,
            algorithms=[settings.JWT_ALGORITHM],
        )
    except JWTError:
        return None


def generate_totp_secret() -> str:
    return base64.b32encode(secrets.token_bytes(20)).decode()


def get_totp_uri(secret: str, email: str, issuer: str = "KNAPS Docs") -> str:
    totp = TOTP(secret)
    return totp.provisioning_uri(name=email, issuer_name=issuer)


def verify_totp_code(secret: str, code: str, valid_window: int = 1) -> bool:
    totp = TOTP(secret)
    return totp.verify(code, valid_window=valid_window)


def generate_qr_code(data: str) -> str:
    qr = qrcode.QRCode(
        version=1,
        error_correction=qrcode.constants.ERROR_CORRECT_L,
        box_size=10,
        border=4,
    )
    qr.add_data(data)
    qr.make(fit=True)

    img = qr.make_image(fill_color="black", back_color="white")
    buffer = BytesIO()
    img.save(buffer, format="PNG")
    return f"data:image/png;base64,{base64.b64encode(buffer.getvalue()).decode()}"


def hash_recovery_code(code: str) -> str:
    return bcrypt.hashpw(code.encode(), bcrypt.gensalt()).decode()


def verify_recovery_code(plain_code: str, hashed_code: str) -> bool:
    return bcrypt.checkpw(plain_code.encode(), hashed_code.encode())


def generate_recovery_codes(count: int = 5) -> list[str]:
    return [secrets.token_hex(4).upper() for _ in range(count)]


def encrypt_field(value: str) -> tuple[str, int]:
    return field_encryption.encrypt(value)


def decrypt_field(ciphertext: str, version: int) -> str:
    return field_encryption.decrypt(ciphertext, version)


def blind_index(value: str) -> str:
    return field_encryption.blind_index(value)
