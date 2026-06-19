import uuid
from typing import TYPE_CHECKING

from sqlalchemy import UUID, Column, ForeignKey, Index, PrimaryKeyConstraint
from sqlmodel import Field, Relationship, SQLModel

if TYPE_CHECKING:
    from app.models.permission import Permission
    from app.models.user import User


class UserPermission(SQLModel, table=True):
    __tablename__ = "user_permissions"
    __table_args__ = (
        PrimaryKeyConstraint("user_id", "permission_id", name="pk_user_permissions"),
        Index("ix_user_permissions_user_id", "user_id"),
        Index("ix_user_permissions_permission_id", "permission_id"),
    )

    user_id: uuid.UUID = Field(
        sa_column=Column(
            UUID(as_uuid=True),
            ForeignKey("users.id", ondelete="CASCADE"),
            nullable=False,
        )
    )
    permission_id: uuid.UUID = Field(
        sa_column=Column(
            UUID(as_uuid=True),
            ForeignKey("permissions.id", ondelete="CASCADE"),
            nullable=False,
        )
    )

    user: "User" = Relationship(back_populates="user_permissions")
    permission: "Permission" = Relationship(back_populates="user_permissions")
