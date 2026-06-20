from fastapi import APIRouter, Depends, HTTPException, status
from sqlmodel.ext.asyncio.session import AsyncSession

from app.core.deps import get_db
from app.schemas.setup import (
    SetupCompleteRequest,
    SetupCompleteResponse,
    SetupInitRequest,
    SetupInitResponse,
    SetupStatusResponse,
)
from app.services.setup_service import SetupService

router = APIRouter(tags=["setup"])


@router.get("/setup/status", response_model=SetupStatusResponse)
async def setup_status(db: AsyncSession = Depends(get_db)):
    service = SetupService(db)
    needs_setup = await service.needs_setup()
    return SetupStatusResponse(needs_setup=needs_setup)


@router.post("/setup/init", response_model=SetupInitResponse)
async def setup_init(
    request: SetupInitRequest,
    db: AsyncSession = Depends(get_db),
):
    service = SetupService(db)
    try:
        result = await service.init_setup(
            email=request.email,
            full_name=request.full_name,
        )
        return SetupInitResponse(
            qr_data_url=result["qr_data_url"],
            setup_token=result["setup_token"],
        )
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e),
        ) from None


@router.post("/setup/complete", response_model=SetupCompleteResponse)
async def setup_complete(
    request: SetupCompleteRequest,
    db: AsyncSession = Depends(get_db),
):
    service = SetupService(db)
    try:
        result = await service.complete_setup(
            setup_token=request.setup_token,
            totp_code=request.totp_code,
        )
        return SetupCompleteResponse(
            token=result["token"],
            user=result["user"],
            recovery_codes=result["recovery_codes"],
        )
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e),
        ) from None
