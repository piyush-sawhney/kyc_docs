from uuid import UUID

from fastapi import APIRouter, Depends, status
from pydantic import BaseModel
from sqlmodel.ext.asyncio.session import AsyncSession

from app.core.deps import get_current_active_user, get_db
from app.models.permission import Permission
from app.models.user import User
from app.services.permission_service import PermissionService
from app.services.user_service import UserService

router = APIRouter(tags=["permissions"])


class PermissionResponse(BaseModel):
    id: str
    key: str
    label: str
    group: str


class UserPermissionsResponse(BaseModel):
    permissions: list[PermissionResponse]


class SetUserPermissionsRequest(BaseModel):
    permission_ids: list[UUID]


def _perm_to_response(p: Permission) -> PermissionResponse:
    return PermissionResponse(
        id=str(p.id),
        key=p.key,
        label=p.label,
        group=p.group,
    )


@router.get("/permissions", response_model=list[PermissionResponse])
async def list_permissions(
    current_user: User = Depends(get_current_active_user),
    db: AsyncSession = Depends(get_db),
):
    service = PermissionService(db)
    perms = await service.get_all_permissions()
    return [_perm_to_response(p) for p in perms]


@router.get(
    "/users/{user_id}/permissions",
    response_model=UserPermissionsResponse,
)
async def get_user_permissions(
    user_id: UUID,
    current_user: User = Depends(get_current_active_user),
    db: AsyncSession = Depends(get_db),
):
    service = UserService(db)
    perms = await service.get_user_permissions(user_id)
    return UserPermissionsResponse(permissions=[_perm_to_response(p) for p in perms])


@router.put(
    "/users/{user_id}/permissions",
    status_code=status.HTTP_204_NO_CONTENT,
)
async def set_user_permissions(
    user_id: UUID,
    request: SetUserPermissionsRequest,
    current_user: User = Depends(get_current_active_user),
    db: AsyncSession = Depends(get_db),
):
    service = UserService(db)
    await service.set_user_permissions(user_id, request.permission_ids)
