import uuid
from typing import TYPE_CHECKING

from sqlalchemy import UUID, Column, Index, String
from sqlmodel import Field, Relationship, SQLModel

if TYPE_CHECKING:
    from app.models.user_permission import UserPermission


class Permission(SQLModel, table=True):
    __tablename__ = "permissions"
    __table_args__ = (
        Index("ix_permissions_key", "key", unique=True),
        Index("ix_permissions_group", "group"),
    )

    id: uuid.UUID = Field(
        default_factory=uuid.uuid4,
        sa_column=Column(UUID(as_uuid=True), primary_key=True, nullable=False),
    )

    key: str = Field(sa_column=Column(String(100), nullable=False, unique=True))
    label: str = Field(sa_column=Column(String(200), nullable=False))
    group: str = Field(sa_column=Column(String(100), nullable=False))

    user_permissions: list["UserPermission"] = Relationship(
        back_populates="permission",
        sa_relationship_kwargs={"cascade": "all, delete-orphan"},
    )
