from pydantic import BaseModel, EmailStr, Field


class SetupStatusResponse(BaseModel):
    needs_setup: bool


class SetupInitRequest(BaseModel):
    email: EmailStr
    full_name: str = Field(min_length=1, max_length=255)


class SetupInitResponse(BaseModel):
    qr_data_url: str
    setup_token: str


class SetupVerifyRequest(BaseModel):
    setup_token: str
    totp_code: str = Field(min_length=6, max_length=6, pattern=r"^\d{6}$")


class SetupVerifyResponse(BaseModel):
    user: "UserResponse"
    recovery_codes: list[str]
    confirm_token: str


class SetupConfirmRequest(BaseModel):
    confirm_token: str


class SetupConfirmResponse(BaseModel):
    token: str
    user: "UserResponse"


class UserResponse(BaseModel):
    id: str
    email: str
    full_name: str
    role: str


SetupVerifyResponse.model_rebuild()
SetupConfirmResponse.model_rebuild()
