from functools import lru_cache

from pydantic import Field, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=True,
        extra="ignore",
    )

    DATABASE_URL: str = Field(
        default="",
        description="PostgreSQL connection string",
    )

    ENCRYPTION_KEYS: str = Field(
        default="",
        description="Semicolon-separated version:base64url-key pairs"
        " (e.g. '1:<key>;2:<key>')",
    )

    JWT_SECRET_KEY: str = Field(
        default="",
        description="Secret key for JWT signing",
    )
    JWT_ALGORITHM: str = Field(default="HS256")
    JWT_ACCESS_TOKEN_EXPIRE_MINUTES: int = Field(default=1440)

    APP_ENV: str = Field(default="development")
    APP_HOST: str = Field(default="0.0.0.0")
    APP_PORT: int = Field(default=8000)

    CORS_ORIGINS: str = Field(default="http://localhost:5173,http://localhost:3000")

    @model_validator(mode="after")
    def check_required(self) -> "Settings":
        if not self.DATABASE_URL:
            raise ValueError("DATABASE_URL must be set in environment or .env file")
        if not self.JWT_SECRET_KEY:
            raise ValueError("JWT_SECRET_KEY must be set in environment or .env file")
        if not self.ENCRYPTION_KEYS:
            raise ValueError("ENCRYPTION_KEYS must be set in environment or .env file")
        return self

    @property
    def cors_origins_list(self) -> list[str]:
        return [origin.strip() for origin in self.CORS_ORIGINS.split(",")]


@lru_cache
def get_settings() -> Settings:
    return Settings()


settings = get_settings()
