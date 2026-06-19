import base64
import hashlib
import hmac

from cryptography.fernet import Fernet

from app.core.config import settings


class FieldEncryption:
    def __init__(self) -> None:
        if not settings.ENCRYPTION_MASTER_KEY:
            raise ValueError("ENCRYPTION_MASTER_KEY must be set in environment")

        master_key = base64.urlsafe_b64decode(settings.ENCRYPTION_MASTER_KEY)
        if len(master_key) != 32:
            raise ValueError(
                "ENCRYPTION_MASTER_KEY must be 32 bytes (base64url-encoded)"
            )

        self.fernet = Fernet(base64.urlsafe_b64encode(master_key))
        self.hmac_key = hashlib.sha256(master_key + b"blind-index-v1").digest()
        self.current_version = 1

    def encrypt(self, plaintext: str) -> tuple[str, int]:
        ciphertext = self.fernet.encrypt(plaintext.encode()).decode()
        return ciphertext, self.current_version

    def decrypt(self, ciphertext: str, version: int) -> str:
        if version != self.current_version:
            raise ValueError(f"Unsupported encryption version: {version}")
        return self.fernet.decrypt(ciphertext.encode()).decode()

    def blind_index(self, plaintext: str) -> str:
        normalized = plaintext.lower().strip()
        return hmac.new(self.hmac_key, normalized.encode(), hashlib.sha256).hexdigest()

    def rotate_key(self, new_master_key: str) -> None:
        new_key = base64.urlsafe_b64decode(new_master_key)
        if len(new_key) != 32:
            raise ValueError("New master key must be 32 bytes")
        self.fernet = Fernet(base64.urlsafe_b64encode(new_key))
        self.hmac_key = hashlib.sha256(new_key + b"blind-index-v1").digest()
        self.current_version += 1


field_encryption = FieldEncryption()
