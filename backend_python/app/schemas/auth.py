from datetime import datetime

from pydantic import BaseModel, EmailStr, Field


class LoginInitRequest(BaseModel):
    email: EmailStr


class LoginInitResponse(BaseModel):
    enrolled: bool
    needs_confirm: bool = False
    enroll_token: str | None = None
    qr_data_url: str | None = None


class ResumeSetupRequest(BaseModel):
    email: EmailStr
    totp_code: str = Field(min_length=6, max_length=6, pattern=r"^\d{6}$")


class ResumeSetupResponse(BaseModel):
    confirm_token: str
    recovery_codes: list[str]
    user: "UserResponse"


class LoginRequest(BaseModel):
    email: EmailStr
    totp_code: str = Field(min_length=6, max_length=6, pattern=r"^\d{6}$")


class LoginResponse(BaseModel):
    token: str
    user: "UserResponse"
    recovery_codes_missing: bool = False


class RecoveryLoginRequest(BaseModel):
    email: EmailStr
    recovery_code: str = Field(min_length=8, max_length=8, pattern=r"^[A-F0-9]{8}$")


class TOTPEnrollRequest(BaseModel):
    enroll_token: str
    totp_code: str = Field(min_length=6, max_length=6, pattern=r"^\d{6}$")


class TOTPEnrollResponse(BaseModel):
    token: str
    user: "UserResponse"
    recovery_codes: list[str] | None = None


class QRCodeResponse(BaseModel):
    qr_data_url: str


class ReEnrollInitResponse(BaseModel):
    qr_data_url: str


class ReEnrollVerifyRequest(BaseModel):
    totp_code: str = Field(min_length=6, max_length=6, pattern=r"^\d{6}$")


class ReEnrollVerifyResponse(BaseModel):
    message: str


class RecoveryCodeStatus(BaseModel):
    id: str
    is_used: bool
    created_at: datetime


class RecoveryCodeListResponse(BaseModel):
    codes: list[RecoveryCodeStatus]


class RecoveryCodesResponse(BaseModel):
    recovery_codes: list[str]


class UserResponse(BaseModel):
    id: str
    email: str
    full_name: str
    role: str


LoginResponse.model_rebuild()
TOTPEnrollResponse.model_rebuild()
ResumeSetupResponse.model_rebuild()
