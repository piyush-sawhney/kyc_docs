import uuid
from datetime import UTC, datetime
from typing import TYPE_CHECKING, ClassVar

from pydantic import ConfigDict
from sqlalchemy import UUID, Column, DateTime, Index, String
from sqlalchemy.ext.hybrid import hybrid_property
from sqlmodel import Field, Relationship, SQLModel, select

from app.core.database import get_db_session
from app.core.encryption import field_encryption
from app.models.permission import Permission
from app.models.user_permission import UserPermission

if TYPE_CHECKING:
    from app.models.recovery_code import RecoveryCode
    from app.models.user_permission import UserPermission


class User(SQLModel, table=True):
    model_config = ConfigDict(
        arbitrary_types_allowed=True, ignored_types=(hybrid_property,)
    )
    __tablename__ = "users"
    __table_args__ = (
        Index("ix_users_email_hash", "email_hash", unique=True),
    )

    HYBRID_PROPS: ClassVar[set[str]] = {
        "email", "full_name", "role",
        "totp_secret", "created_by", "modified_by",
    }
    HASH_FIELDS: ClassVar[set[str]] = {
        "email",
    }

    def __init__(self, **kwargs):
        hybrid_kwargs = {
            k: kwargs.pop(k) for k in list(kwargs) if k in self.HYBRID_PROPS
        }
        super().__init__(**kwargs)
        for k, v in hybrid_kwargs.items():
            if v is not None:
                enc, ver = field_encryption.encrypt(str(v))
                object.__setattr__(self, f"{k}_encrypted", enc)
                object.__setattr__(self, "encryption_version", ver)
                if k in self.HASH_FIELDS:
                    object.__setattr__(
                        self, f"{k}_hash", field_encryption.blind_index(str(v))
                    )

    id: uuid.UUID = Field(
        default_factory=uuid.uuid4,
        sa_column=Column(UUID(as_uuid=True), primary_key=True, nullable=False),
    )

    email_encrypted: str = Field(
        sa_column=Column("email_encrypted", String(500), nullable=False)
    )
    full_name_encrypted: str = Field(
        sa_column=Column("full_name_encrypted", String(500), nullable=False)
    )
    totp_secret_encrypted: str | None = Field(
        default=None, sa_column=Column("totp_secret_encrypted", String(500))
    )
    role_encrypted: str = Field(
        default="user", sa_column=Column("role_encrypted", String(500), nullable=False)
    )
    created_by_encrypted: str | None = Field(
        default=None, sa_column=Column("created_by_encrypted", String(500))
    )
    modified_by_encrypted: str | None = Field(
        default=None, sa_column=Column("modified_by_encrypted", String(500))
    )

    email_hash: str = Field(
        sa_column=Column(
            "email_hash", String(64), nullable=False, unique=True
        )
    )

    encryption_version: int = Field(default=1, nullable=False)

    is_active: bool = Field(default=True, nullable=False)
    is_deleted: bool = Field(default=False, nullable=False)
    deleted_at: datetime | None = Field(
        default=None, sa_column=Column(DateTime(timezone=True))
    )
    totp_verified: bool = Field(default=False, nullable=False)

    created_at: datetime = Field(
        default_factory=lambda: datetime.now(UTC),
        sa_column=Column(DateTime(timezone=True), nullable=False),
    )
    updated_at: datetime = Field(
        default_factory=lambda: datetime.now(UTC),
        sa_column=Column(DateTime(timezone=True), nullable=False),
    )

    user_permissions: list["UserPermission"] = Relationship(
        back_populates="user", sa_relationship_kwargs={"cascade": "all, delete-orphan"}
    )
    recovery_codes: list["RecoveryCode"] = Relationship(
        back_populates="user", sa_relationship_kwargs={"cascade": "all, delete-orphan"}
    )

    @hybrid_property
    def email(self) -> str:
        return field_encryption.decrypt(self.email_encrypted, self.encryption_version)

    @email.setter
    def email(self, value: str) -> None:
        enc, ver = field_encryption.encrypt(value)
        self.email_encrypted = enc
        self.encryption_version = ver
        self.email_hash = field_encryption.blind_index(value)

    @hybrid_property
    def full_name(self) -> str:
        return field_encryption.decrypt(
            self.full_name_encrypted, self.encryption_version
        )

    @full_name.setter
    def full_name(self, value: str) -> None:
        enc, ver = field_encryption.encrypt(value)
        self.full_name_encrypted = enc
        self.encryption_version = ver

    @hybrid_property
    def role(self) -> str:
        return field_encryption.decrypt(self.role_encrypted, self.encryption_version)

    @role.setter
    def role(self, value: str) -> None:
        enc, ver = field_encryption.encrypt(value)
        self.role_encrypted = enc
        self.encryption_version = ver

    @hybrid_property
    def totp_secret(self) -> str | None:
        if self.totp_secret_encrypted:
            return field_encryption.decrypt(
                self.totp_secret_encrypted, self.encryption_version
            )
        return None

    @totp_secret.setter
    def totp_secret(self, value: str | None) -> None:
        if value:
            enc, ver = field_encryption.encrypt(value)
            self.totp_secret_encrypted = enc
            self.encryption_version = ver
        else:
            self.totp_secret_encrypted = None

    @hybrid_property
    def created_by(self) -> uuid.UUID | None:
        if self.created_by_encrypted:
            decrypted = field_encryption.decrypt(
                self.created_by_encrypted, self.encryption_version
            )
            return uuid.UUID(decrypted)
        return None

    @created_by.setter
    def created_by(self, value: uuid.UUID | None) -> None:
        if value:
            enc, ver = field_encryption.encrypt(str(value))
            self.created_by_encrypted = enc
            self.encryption_version = ver
        else:
            self.created_by_encrypted = None

    @hybrid_property
    def modified_by(self) -> uuid.UUID | None:
        if self.modified_by_encrypted:
            decrypted = field_encryption.decrypt(
                self.modified_by_encrypted, self.encryption_version
            )
            return uuid.UUID(decrypted)
        return None

    @modified_by.setter
    def modified_by(self, value: uuid.UUID | None) -> None:
        if value:
            enc, ver = field_encryption.encrypt(str(value))
            self.modified_by_encrypted = enc
            self.encryption_version = ver
        else:
            self.modified_by_encrypted = None

    async def get_permissions(self) -> list[str]:
        async for session in get_db_session():
            result = await session.exec(
                select(Permission.key)
                .join(UserPermission, Permission.id == UserPermission.permission_id)
                .where(UserPermission.user_id == self.id)
            )
            return list(result.all())
        return []
