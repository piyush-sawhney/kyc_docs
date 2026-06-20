import uuid
from datetime import UTC, datetime
from typing import TYPE_CHECKING, Optional

from sqlalchemy import UUID, Column, DateTime, ForeignKey, Index, String, Text
from sqlalchemy.dialects.postgresql import JSONB
from sqlmodel import Field, Relationship, SQLModel

if TYPE_CHECKING:
    from app.models.user import User


class AuditLog(SQLModel, table=True):
    __tablename__ = "audit_logs"
    __table_args__ = (
        Index("ix_audit_logs_user_id", "user_id"),
        Index("ix_audit_logs_entity", "entity_type", "entity_id"),
        Index("ix_audit_logs_created_at", "created_at"),
    )

    id: uuid.UUID = Field(
        default_factory=uuid.uuid4,
        sa_column=Column(UUID(as_uuid=True), primary_key=True, nullable=False),
    )

    user_id: uuid.UUID | None = Field(
        default=None,
        sa_column=Column(
            UUID(as_uuid=True),
            ForeignKey("users.id", ondelete="SET NULL"),
            nullable=True,
        ),
    )
    action: str = Field(sa_column=Column(String(50), nullable=False))
    entity_type: str = Field(sa_column=Column(String(50), nullable=False))
    entity_id: uuid.UUID = Field(sa_column=Column(UUID(as_uuid=True), nullable=False))
    description: str = Field(sa_column=Column(Text(), nullable=False))
    old_values: dict | None = Field(default=None, sa_column=Column(JSONB))
    new_values: dict | None = Field(default=None, sa_column=Column(JSONB))
    ip_address: str | None = Field(default=None, sa_column=Column(String(45)))
    user_agent: str | None = Field(default=None, sa_column=Column(Text()))
    created_at: datetime = Field(
        default_factory=lambda: datetime.now(UTC),
        sa_column=Column(DateTime(timezone=True), nullable=False),
    )

    user: Optional["User"] = Relationship()
