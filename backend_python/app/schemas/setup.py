from pydantic import BaseModel, EmailStr, Field


class SetupStatusResponse(BaseModel):
    needs_setup: bool


class SetupInitRequest(BaseModel):
    email: EmailStr
    full_name: str = Field(min_length=1, max_length=255)


class SetupInitResponse(BaseModel):
    qr_data_url: str
    setup_token: str


class SetupCompleteRequest(BaseModel):
    setup_token: str
    totp_code: str = Field(min_length=6, max_length=6, pattern=r"^\d{6}$")


class SetupCompleteResponse(BaseModel):
    token: str
    user: "UserResponse"
    recovery_codes: list[str]


class UserResponse(BaseModel):
    id: str
    email: str
    full_name: str
    role: str


SetupCompleteResponse.model_rebuild()
