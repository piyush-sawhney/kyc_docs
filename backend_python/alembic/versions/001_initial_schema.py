"""initial schema

Revision ID: 001
Revises: 
Create Date: 2024-01-01 00:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision: str = "001"
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Create users table
    op.create_table(
        "users",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("email_encrypted", sa.String(length=500), nullable=False),
        sa.Column("full_name_encrypted", sa.String(length=500), nullable=False),
        sa.Column("totp_secret_encrypted", sa.String(length=500), nullable=True),
        sa.Column("role_encrypted", sa.String(length=500), nullable=False),
        sa.Column("created_by_encrypted", sa.String(length=500), nullable=True),
        sa.Column("modified_by_encrypted", sa.String(length=500), nullable=True),
        sa.Column("email_hash", sa.String(length=64), nullable=False),
        sa.Column("dek_encrypted", sa.String(length=500), nullable=True),
        sa.Column("is_active", sa.Boolean(), nullable=False, default=True),
        sa.Column("is_deleted", sa.Boolean(), nullable=False, default=False),
        sa.Column("deleted_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("totp_verified", sa.Boolean(), nullable=False, default=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
        sa.PrimaryKeyConstraint("id", name="pk_users"),
    )
    op.create_index("ix_users_email_hash", "users", ["email_hash"], unique=True)

    # Create permissions table
    op.create_table(
        "permissions",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("key", sa.String(length=100), nullable=False),
        sa.Column("label", sa.String(length=200), nullable=False),
        sa.Column("group", sa.String(length=100), nullable=False),
        sa.PrimaryKeyConstraint("id", name="pk_permissions"),
    )
    op.create_index("ix_permissions_key", "permissions", ["key"], unique=True)
    op.create_index("ix_permissions_group", "permissions", ["group"], unique=False)

    # Create user_permissions table
    op.create_table(
        "user_permissions",
        sa.Column("user_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("permission_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.ForeignKeyConstraint(["permission_id"], ["permissions.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("user_id", "permission_id", name="pk_user_permissions"),
    )
    op.create_index("ix_user_permissions_user_id", "user_permissions", ["user_id"], unique=False)
    op.create_index("ix_user_permissions_permission_id", "user_permissions", ["permission_id"], unique=False)

    # Create recovery_codes table
    op.create_table(
        "recovery_codes",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("user_id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("code_hash", sa.String(length=255), nullable=False),
        sa.Column("is_used", sa.Boolean(), nullable=False, default=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
        sa.PrimaryKeyConstraint("id", name="pk_recovery_codes"),
    )
    op.create_index("ix_recovery_codes_user_id", "recovery_codes", ["user_id"], unique=False)
    op.create_index("ix_recovery_codes_is_used", "recovery_codes", ["is_used"], unique=False)

    # Create document_types table
    op.create_table(
        "document_types",
        sa.Column("id", postgresql.UUID(as_uuid=True), nullable=False),
        sa.Column("name", sa.String(length=100), nullable=False),
        sa.Column("is_active", sa.Boolean(), nullable=False, default=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.PrimaryKeyConstraint("id", name="pk_document_types"),
    )
    op.create_index("ix_document_types_name", "document_types", ["name"], unique=True)
    op.create_index("ix_document_types_is_active", "document_types", ["is_active"], unique=False)


def downgrade() -> None:
    op.drop_index("ix_document_types_is_active", table_name="document_types")
    op.drop_index("ix_document_types_name", table_name="document_types")
    op.drop_table("document_types")

    op.drop_index("ix_recovery_codes_is_used", table_name="recovery_codes")
    op.drop_index("ix_recovery_codes_user_id", table_name="recovery_codes")
    op.drop_table("recovery_codes")

    op.drop_index("ix_user_permissions_permission_id", table_name="user_permissions")
    op.drop_index("ix_user_permissions_user_id", table_name="user_permissions")
    op.drop_table("user_permissions")

    op.drop_index("ix_permissions_group", table_name="permissions")
    op.drop_index("ix_permissions_key", table_name="permissions")
    op.drop_table("permissions")

    op.drop_index("ix_users_email_hash", table_name="users")
    op.drop_table("users")