from fastapi import APIRouter, Depends, HTTPException, status
from sqlmodel.ext.asyncio.session import AsyncSession

from app.core.deps import get_current_active_user, get_db
from app.models.user import User
from app.schemas.auth import (
    LoginInitRequest,
    LoginInitResponse,
    LoginRequest,
    LoginResponse,
    QRCodeResponse,
    RecoveryCodeListResponse,
    RecoveryCodesResponse,
    RecoveryCodeStatus,
    RecoveryLoginRequest,
    ReEnrollInitResponse,
    ReEnrollVerifyRequest,
    ReEnrollVerifyResponse,
    ResumeSetupRequest,
    ResumeSetupResponse,
    TOTPEnrollRequest,
    TOTPEnrollResponse,
    UserResponse,
)
from app.services.auth_service import AuthService

router = APIRouter(tags=["auth"])


@router.post("/auth/login/init", response_model=LoginInitResponse)
async def login_init(
    request: LoginInitRequest,
    db: AsyncSession = Depends(get_db),
):
    service = AuthService(db)
    result = await service.login_init(email=request.email)
    return LoginInitResponse(
        enrolled=result["enrolled"],
        enroll_token=result.get("enroll_token"),
        qr_data_url=result.get("qr_data_url"),
    )


@router.post("/auth/login", response_model=LoginResponse)
async def login(
    request: LoginRequest,
    db: AsyncSession = Depends(get_db),
):
    service = AuthService(db)
    try:
        result = await service.login(email=request.email, totp_code=request.totp_code)
        return LoginResponse(
            token=result["token"],
            user=result["user"],
            recovery_codes_missing=result["recovery_codes_missing"],
        )
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=str(e),
        ) from None


@router.post("/auth/login/recovery", response_model=LoginResponse)
async def recovery_login(
    request: RecoveryLoginRequest,
    db: AsyncSession = Depends(get_db),
):
    service = AuthService(db)
    try:
        result = await service.recovery_login(
            email=request.email, recovery_code=request.recovery_code
        )
        return LoginResponse(
            token=result["token"],
            user=result["user"],
            recovery_codes_missing=result["recovery_codes_missing"],
        )
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=str(e),
        ) from None


@router.post("/auth/resume-setup", response_model=ResumeSetupResponse)
async def resume_setup(
    request: ResumeSetupRequest,
    db: AsyncSession = Depends(get_db),
):
    service = AuthService(db)
    try:
        result = await service.resume_setup(
            email=request.email, totp_code=request.totp_code
        )
        return ResumeSetupResponse(
            confirm_token=result["confirm_token"],
            recovery_codes=result["recovery_codes"],
            user=result["user"],
        )
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=str(e),
        ) from None


@router.post("/auth/enroll", response_model=TOTPEnrollResponse)
async def totp_enroll(
    request: TOTPEnrollRequest,
    db: AsyncSession = Depends(get_db),
):
    service = AuthService(db)
    try:
        result = await service.totp_enroll(
            enroll_token=request.enroll_token, totp_code=request.totp_code
        )
        return TOTPEnrollResponse(
            token=result["token"],
            user=result["user"],
            recovery_codes=result.get("recovery_codes"),
        )
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e),
        ) from None


@router.get("/auth/qr", response_model=QRCodeResponse)
async def get_qr_code(
    email: str,
    db: AsyncSession = Depends(get_db),
):
    service = AuthService(db)
    try:
        qr_data_url = await service.get_qr(email=email)
        return QRCodeResponse(qr_data_url=qr_data_url)
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=str(e),
        ) from None


@router.post("/auth/totp/re-enroll", response_model=ReEnrollInitResponse)
async def re_enroll_init(
    current_user: User = Depends(get_current_active_user),
    db: AsyncSession = Depends(get_db),
):
    service = AuthService(db)
    try:
        qr_data_url = await service.re_enroll(user_id=current_user.id)
        return ReEnrollInitResponse(qr_data_url=qr_data_url)
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e),
        ) from None


@router.post("/auth/totp/re-enroll/verify", response_model=ReEnrollVerifyResponse)
async def re_enroll_verify(
    request: ReEnrollVerifyRequest,
    current_user: User = Depends(get_current_active_user),
    db: AsyncSession = Depends(get_db),
):
    service = AuthService(db)
    try:
        await service.re_enroll_verify(
            user_id=current_user.id, totp_code=request.totp_code
        )
        return ReEnrollVerifyResponse(message="Authenticator re-enrolled successfully")
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e),
        ) from None


@router.get("/auth/recovery-codes", response_model=RecoveryCodeListResponse)
async def list_recovery_codes(
    current_user: User = Depends(get_current_active_user),
    db: AsyncSession = Depends(get_db),
):
    service = AuthService(db)
    codes = await service.get_recovery_codes(user_id=current_user.id)
    return RecoveryCodeListResponse(
        codes=[
            RecoveryCodeStatus(
                id=str(c.id),
                is_used=c.is_used,
                created_at=c.created_at,
            )
            for c in codes
        ]
    )


@router.post("/auth/recovery-codes/generate", response_model=RecoveryCodesResponse)
async def generate_recovery_codes(
    current_user: User = Depends(get_current_active_user),
    db: AsyncSession = Depends(get_db),
):
    service = AuthService(db)
    codes = await service.generate_new_recovery_codes(user_id=current_user.id)
    return RecoveryCodesResponse(recovery_codes=codes)


@router.get("/auth/me", response_model=UserResponse)
async def auth_me(
    current_user: User = Depends(get_current_active_user),
):
    return UserResponse(
        id=str(current_user.id),
        email=current_user.email,
        full_name=current_user.full_name,
        role=current_user.role,
    )


@router.post("/auth/logout")
async def logout():
    return {"message": "Logged out"}
