from uuid import UUID

from sqlmodel.ext.asyncio.session import AsyncSession

from app.models.audit_log import AuditLog


class AuditService:
    def __init__(self, db: AsyncSession):
        self.db = db

    async def log(  # noqa: PLR0913
        self,
        *,
        action: str,
        entity_type: str,
        entity_id: UUID,
        description: str,
        user_id: UUID | None = None,
        old_values: dict | None = None,
        new_values: dict | None = None,
        ip_address: str | None = None,
        user_agent: str | None = None,
    ) -> None:
        try:
            log_entry = AuditLog(
                user_id=user_id,
                action=action,
                entity_type=entity_type,
                entity_id=entity_id,
                description=description,
                old_values=old_values,
                new_values=new_values,
                ip_address=ip_address,
                user_agent=user_agent,
            )
            self.db.add(log_entry)
            await self.db.flush()
        except Exception:
            pass
