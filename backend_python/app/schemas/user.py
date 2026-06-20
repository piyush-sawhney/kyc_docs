from datetime import datetime

from pydantic import BaseModel, EmailStr, Field


class UserCreate(BaseModel):
    email: EmailStr
    full_name: str = Field(min_length=1, max_length=255)
    role: str = Field(default="user", pattern=r"^(admin|user)$")


class UserUpdate(BaseModel):
    full_name: str | None = Field(default=None, min_length=1, max_length=255)
    role: str | None = Field(default=None, pattern=r"^(admin|user)$")
    is_active: bool | None = None


class UserResponse(BaseModel):
    id: str
    email: str
    full_name: str
    role: str
    is_active: bool
    is_deleted: bool
    totp_verified: bool
    deleted_at: datetime | None = None
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True


class UserListResponse(BaseModel):
    users: list[UserResponse]
    total: int


class UserRoleUpdate(BaseModel):
    role: str = Field(pattern=r"^(admin|user)$")


class DeletedUserResponse(BaseModel):
    id: str
    email: str
    full_name: str
    role: str
    is_active: bool
    is_deleted: bool
    deleted_at: datetime | None

    class Config:
        from_attributes = True


class DeletedUserListResponse(BaseModel):
    users: list[DeletedUserResponse]
    total: int


class UserDeactivateResponse(BaseModel):
    id: str
    email: str
    full_name: str
    role: str
    is_active: bool


class ReEnrollResponse(BaseModel):
    qr_data_url: str


class AdminRecoveryCodesResponse(BaseModel):
    recovery_codes: list[str]
