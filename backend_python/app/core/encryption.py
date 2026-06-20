import base64
import hashlib
import hmac
import secrets

from cryptography.fernet import Fernet

from app.core.config import settings


class FieldEncryption:
    def __init__(self) -> None:
        keys_str = settings.ENCRYPTION_KEYS
        if not keys_str:
            raise ValueError("ENCRYPTION_KEYS must be set in environment")

        self._key_ring: dict[int, Fernet] = {}
        self._hmac_keys: dict[int, bytes] = {}

        for pair in keys_str.split(";"):
            clean = pair.strip()
            if not clean:
                continue
            try:
                version_str, key_b64 = clean.split(":", 1)
                version = int(version_str.strip())
                key_bytes = base64.urlsafe_b64decode(key_b64.strip())
                if len(key_bytes) != 32:
                    raise ValueError(f"Key for version {version} must be 32 bytes")
                self._key_ring[version] = Fernet(base64.urlsafe_b64encode(key_bytes))
                self._hmac_keys[version] = hashlib.sha256(
                    key_bytes + b"blind-index-v1"
                ).digest()
            except (ValueError, IndexError) as e:
                raise ValueError(f"Invalid ENCRYPTION_KEYS entry: {clean}") from e

        if not self._key_ring:
            raise ValueError("No valid keys found in ENCRYPTION_KEYS")

        self.active_version = max(self._key_ring.keys())
        self._active_kek = self._key_ring[self.active_version]

    def create_row_key(self) -> tuple[bytes, str]:
        dek = secrets.token_bytes(32)
        ciphertext = self._active_kek.encrypt(dek).decode()
        return dek, f"{self.active_version}:{ciphertext}"

    def unwrap_row_key(self, dek_encrypted: str) -> bytes:
        version_str, ciphertext = dek_encrypted.split(":", 1)
        version = int(version_str)
        kek = self._key_ring.get(version)
        if not kek:
            raise ValueError(f"Unsupported encryption version: {version}")
        return kek.decrypt(ciphertext.encode())

    def encrypt(self, plaintext: str, dek: bytes) -> str:
        fernet = Fernet(base64.urlsafe_b64encode(dek))
        return fernet.encrypt(plaintext.encode()).decode()

    def decrypt(self, ciphertext: str, dek: bytes) -> str:
        fernet = Fernet(base64.urlsafe_b64encode(dek))
        return fernet.decrypt(ciphertext.encode()).decode()

    def blind_index(self, plaintext: str) -> str:
        normalized = plaintext.lower().strip()
        hmac_key = self._hmac_keys[self.active_version]
        return hmac.new(hmac_key, normalized.encode(), hashlib.sha256).hexdigest()


field_encryption = FieldEncryption()
