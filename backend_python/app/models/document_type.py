import uuid
from datetime import UTC, datetime
from typing import TYPE_CHECKING

from sqlalchemy import UUID, Column, DateTime, Index, String
from sqlmodel import Field, SQLModel

if TYPE_CHECKING:
    pass


class DocumentType(SQLModel, table=True):
    __tablename__ = "document_types"
    __table_args__ = (
        Index("ix_document_types_name", "name", unique=True),
        Index("ix_document_types_is_active", "is_active"),
    )

    id: uuid.UUID = Field(
        default_factory=uuid.uuid4,
        sa_column=Column(UUID(as_uuid=True), primary_key=True, nullable=False),
    )

    name: str = Field(sa_column=Column(String(100), nullable=False, unique=True))
    is_active: bool = Field(default=True, nullable=False)

    created_at: datetime = Field(
        default_factory=lambda: datetime.now(UTC),
        sa_column=Column(DateTime(timezone=True), nullable=False),
    )
