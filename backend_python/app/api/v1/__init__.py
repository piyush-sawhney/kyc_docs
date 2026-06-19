from app.api.v1.auth import router as auth_router
from app.api.v1.permissions import router as permissions_router
from app.api.v1.setup import router as setup_router
from app.api.v1.users import router as users_router

__all__ = ["setup_router", "auth_router", "users_router", "permissions_router"]
