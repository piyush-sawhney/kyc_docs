from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Query, Request, status
from sqlmodel.ext.asyncio.session import AsyncSession

from app.core.deps import (
    get_current_active_user,
    get_db,
    prevent_self_action,
    require_permission,
    require_role,
)
from app.models.user import User
from app.schemas.user import (
    AdminRecoveryCodesResponse,
    DeletedUserListResponse,
    DeletedUserResponse,
    ReEnrollResponse,
    UserCreate,
    UserDeactivateResponse,
    UserListResponse,
    UserResponse,
    UserRoleUpdate,
    UserUpdate,
)
from app.services.user_service import UserService

router = APIRouter(tags=["users"])


def _user_to_response(user: User) -> UserResponse:
    return UserResponse(
        id=str(user.id),
        email=user.email,
        full_name=user.full_name,
        role=user.role,
        is_active=user.is_active,
        is_deleted=user.is_deleted,
        totp_verified=user.totp_verified,
        deleted_at=user.deleted_at,
        created_at=user.created_at,
        updated_at=user.updated_at,
    )


def _deleted_user_to_response(user: User) -> DeletedUserResponse:
    return DeletedUserResponse(
        id=str(user.id),
        email=user.email,
        full_name=user.full_name,
        role=user.role,
        is_active=user.is_active,
        is_deleted=user.is_deleted,
        deleted_at=user.deleted_at,
    )


def _user_to_deactivate_response(user: User) -> UserDeactivateResponse:
    return UserDeactivateResponse(
        id=str(user.id),
        email=user.email,
        full_name=user.full_name,
        role=user.role,
        is_active=user.is_active,
    )


@router.get("/users", response_model=UserListResponse)
async def list_users(
    include_deleted: bool = Query(default=False),
    skip: int = Query(default=0, ge=0),
    limit: int = Query(default=100, ge=1, le=200),
    current_user: User = Depends(get_current_active_user),
    db: AsyncSession = Depends(get_db),
):
    if include_deleted:
        user_perms = await current_user.get_permissions()
        if "user:view" not in user_perms and "permission:manage" not in user_perms:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Permission 'user:view' required",
            )
    service = UserService(db)
    users, total = await service.get_users(
        skip=skip, limit=limit, include_deleted=include_deleted
    )
    return UserListResponse(
        users=[_user_to_response(u) for u in users],
        total=total,
    )


@router.post("/users", response_model=UserResponse, status_code=status.HTTP_201_CREATED)
async def create_user(
    data: UserCreate,
    current_user: User = Depends(get_current_active_user),
    db: AsyncSession = Depends(get_db),
):
    service = UserService(db)
    try:
        user = await service.create_user(data)
        return _user_to_response(user)
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=str(e),
        ) from None


@router.get("/users/deleted", response_model=DeletedUserListResponse)
async def list_deleted_users(
    skip: int = Query(default=0, ge=0),
    limit: int = Query(default=100, ge=1, le=200),
    current_user: User = Depends(require_permission("user:view")),
    db: AsyncSession = Depends(get_db),
):
    service = UserService(db)
    users, total = await service.get_deleted_users(skip=skip, limit=limit)
    return DeletedUserListResponse(
        users=[_deleted_user_to_response(u) for u in users],
        total=total,
    )


@router.get("/users/{user_id}", response_model=UserResponse)
async def get_user(
    user_id: UUID,
    current_user: User = Depends(get_current_active_user),
    db: AsyncSession = Depends(get_db),
):
    service = UserService(db)
    user = await service.get_user(user_id)
    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User not found",
        )
    return _user_to_response(user)


@router.put("/users/{user_id}", response_model=UserResponse)
async def update_user(
    user_id: UUID,
    data: UserUpdate,
    current_user: User = Depends(get_current_active_user),
    db: AsyncSession = Depends(get_db),
):
    service = UserService(db)
    try:
        user = await service.update_user(user_id, data)
        if not user:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="User not found",
            )
        return _user_to_response(user)
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e),
        ) from None


@router.delete("/users/{user_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_user(
    user_id: UUID,
    current_user: User = Depends(get_current_active_user),
    db: AsyncSession = Depends(get_db),
):
    service = UserService(db)
    deleted = await service.delete_user(user_id)
    if not deleted:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User not found",
        )


@router.post("/users/{user_id}/deactivate", response_model=UserDeactivateResponse)
async def deactivate_user(
    request: Request,
    user_id: UUID = Depends(prevent_self_action("deactivate")),
    current_user: User = Depends(require_role("admin")),
    db: AsyncSession = Depends(get_db),
):
    service = UserService(db)
    try:
        user = await service.deactivate_user(user_id, current_user.id, request=request)
        return _user_to_deactivate_response(user)
    except ValueError as e:
        if "User not found" in str(e):
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=str(e),
            ) from None
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=str(e),
        ) from None


@router.post("/users/{user_id}/reactivate", response_model=UserResponse)
async def reactivate_user(
    request: Request,
    user_id: UUID,
    current_user: User = Depends(require_role("admin")),
    db: AsyncSession = Depends(get_db),
):
    service = UserService(db)
    try:
        user = await service.reactivate_user(user_id, current_user.id, request=request)
        return _user_to_response(user)
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(e),
        ) from None


@router.post("/users/{user_id}/restore", response_model=UserResponse)
async def restore_user(
    request: Request,
    user_id: UUID,
    current_user: User = Depends(require_permission("user:manage")),
    db: AsyncSession = Depends(get_db),
):
    service = UserService(db)
    try:
        user = await service.restore_user(user_id, current_user.id, request=request)
        return _user_to_response(user)
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(e),
        ) from None


@router.patch("/users/{user_id}/role", response_model=UserResponse)
async def update_user_role(
    request: Request,
    data: UserRoleUpdate,
    user_id: UUID = Depends(prevent_self_action("change role of")),
    current_user: User = Depends(require_permission("user:manage")),
    db: AsyncSession = Depends(get_db),
):
    service = UserService(db)
    try:
        user = await service.update_user_role(
            user_id, data.role, current_user.id, request=request
        )
        return _user_to_response(user)
    except ValueError as e:
        if "User not found" in str(e):
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=str(e),
            ) from None
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=str(e),
        ) from None


@router.post(
    "/users/{user_id}/re-enroll",
    response_model=ReEnrollResponse,
)
async def admin_re_enroll(
    request: Request,
    user_id: UUID = Depends(prevent_self_action("re-enroll")),
    current_user: User = Depends(require_role("admin")),
    db: AsyncSession = Depends(get_db),
):
    service = UserService(db)
    try:
        qr_data_url = await service.admin_re_enroll_totp(
            user_id, current_user.id, request=request
        )
        return ReEnrollResponse(qr_data_url=qr_data_url)
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(e),
        ) from None


@router.post(
    "/users/{user_id}/recovery-codes",
    response_model=AdminRecoveryCodesResponse,
)
async def admin_generate_recovery_codes(
    request: Request,
    user_id: UUID,
    current_user: User = Depends(require_role("admin")),
    db: AsyncSession = Depends(get_db),
):
    service = UserService(db)
    try:
        codes = await service.admin_generate_recovery_codes(
            user_id, current_user.id, request=request
        )
        return AdminRecoveryCodesResponse(recovery_codes=codes)
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e),
        ) from None
