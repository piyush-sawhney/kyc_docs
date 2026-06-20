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
    __table_args__ = (Index("ix_users_email_hash", "email_hash", unique=True),)

    HYBRID_PROPS: ClassVar[set[str]] = {
        "email",
        "full_name",
        "role",
        "totp_secret",
        "created_by",
        "modified_by",
    }
    HASH_FIELDS: ClassVar[set[str]] = {
        "email",
    }

    def _ensure_dek(self) -> bytes:
        dek = getattr(self, "_dek", None)
        if dek is not None:
            return dek
        if self.dek_encrypted:
            dek = field_encryption.unwrap_row_key(self.dek_encrypted)
        else:
            dek, dek_encrypted = field_encryption.create_row_key()
            object.__setattr__(self, "dek_encrypted", dek_encrypted)
        object.__setattr__(self, "_dek", dek)
        return dek

    def __init__(self, **kwargs):
        object.__setattr__(self, "_dek", None)
        hybrid_kwargs = {
            k: kwargs.pop(k) for k in list(kwargs) if k in self.HYBRID_PROPS
        }
        super().__init__(**kwargs)
        if hybrid_kwargs:
            dek, dek_encrypted = field_encryption.create_row_key()
            object.__setattr__(self, "_dek", dek)
            object.__setattr__(self, "dek_encrypted", dek_encrypted)
            for k, v in hybrid_kwargs.items():
                if v is not None:
                    enc = field_encryption.encrypt(str(v), dek)
                    object.__setattr__(self, f"{k}_encrypted", enc)
                    if k in self.HASH_FIELDS:
                        object.__setattr__(
                            self,
                            f"{k}_hash",
                            field_encryption.blind_index(str(v)),
                        )

    id: uuid.UUID = Field(
        default_factory=uuid.uuid4,
        sa_column=Column(UUID(as_uuid=True), primary_key=True, nullable=False),
    )

    dek_encrypted: str | None = Field(
        default=None,
        sa_column=Column("dek_encrypted", String(500)),
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
        sa_column=Column("email_hash", String(64), nullable=False, unique=True)
    )

    is_active: bool = Field(default=True, nullable=False)
    is_deleted: bool = Field(default=False, nullable=False)
    deleted_at: datetime | None = Field(
        default=None, sa_column=Column(DateTime(timezone=True))
    )
    totp_verified: bool = Field(default=False, nullable=False)
    admin_onboarding_complete: bool = Field(default=True, nullable=False)

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
        return field_encryption.decrypt(self.email_encrypted, self._ensure_dek())

    @email.expression
    def email(cls):
        return cls.email_encrypted

    @email.setter
    def email(self, value: str) -> None:
        dek = self._ensure_dek()
        self.email_encrypted = field_encryption.encrypt(value, dek)
        self.email_hash = field_encryption.blind_index(value)

    @hybrid_property
    def full_name(self) -> str:
        return field_encryption.decrypt(self.full_name_encrypted, self._ensure_dek())

    @full_name.expression
    def full_name(cls):
        return cls.full_name_encrypted

    @full_name.setter
    def full_name(self, value: str) -> None:
        dek = self._ensure_dek()
        self.full_name_encrypted = field_encryption.encrypt(value, dek)

    @hybrid_property
    def role(self) -> str:
        return field_encryption.decrypt(self.role_encrypted, self._ensure_dek())

    @role.expression
    def role(cls):
        return cls.role_encrypted

    @role.setter
    def role(self, value: str) -> None:
        dek = self._ensure_dek()
        self.role_encrypted = field_encryption.encrypt(value, dek)

    @hybrid_property
    def totp_secret(self) -> str | None:
        if self.totp_secret_encrypted:
            return field_encryption.decrypt(
                self.totp_secret_encrypted, self._ensure_dek()
            )
        return None

    @totp_secret.expression
    def totp_secret(cls):
        return cls.totp_secret_encrypted

    @totp_secret.setter
    def totp_secret(self, value: str | None) -> None:
        dek = self._ensure_dek()
        if value:
            self.totp_secret_encrypted = field_encryption.encrypt(value, dek)
        else:
            self.totp_secret_encrypted = None

    @hybrid_property
    def created_by(self) -> uuid.UUID | None:
        if self.created_by_encrypted:
            decrypted = field_encryption.decrypt(
                self.created_by_encrypted, self._ensure_dek()
            )
            return uuid.UUID(decrypted)
        return None

    @created_by.expression
    def created_by(cls):
        return cls.created_by_encrypted

    @created_by.setter
    def created_by(self, value: uuid.UUID | None) -> None:
        dek = self._ensure_dek()
        if value:
            self.created_by_encrypted = field_encryption.encrypt(str(value), dek)
        else:
            self.created_by_encrypted = None

    @hybrid_property
    def modified_by(self) -> uuid.UUID | None:
        if self.modified_by_encrypted:
            decrypted = field_encryption.decrypt(
                self.modified_by_encrypted, self._ensure_dek()
            )
            return uuid.UUID(decrypted)
        return None

    @modified_by.expression
    def modified_by(cls):
        return cls.modified_by_encrypted

    @modified_by.setter
    def modified_by(self, value: uuid.UUID | None) -> None:
        dek = self._ensure_dek()
        if value:
            self.modified_by_encrypted = field_encryption.encrypt(str(value), dek)
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
