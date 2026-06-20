import pytest
import pytest_asyncio

from app.core.encryption import field_encryption
from app.core.security import create_access_token
from app.models.user import User


@pytest_asyncio.fixture
async def auth_token(db_session):
    dek, dek_encrypted = field_encryption.create_row_key()

    user = User(
        dek_encrypted=dek_encrypted,
        email_encrypted=field_encryption.encrypt("admin@example.com", dek),
        full_name_encrypted=field_encryption.encrypt("Admin User", dek),
        role_encrypted=field_encryption.encrypt("admin", dek),
        email_hash=field_encryption.blind_index("admin@example.com"),
        is_active=True,
        totp_verified=False,
    )
    db_session.add(user)
    await db_session.commit()

    return create_access_token(
        subject=str(user.id),
        email="admin@example.com",
        role="admin",
    )


@pytest.mark.asyncio
async def test_list_users_empty(client, auth_token):
    response = await client.get(
        "/api/v1/users",
        headers={"Authorization": f"Bearer {auth_token}"},
    )
    assert response.status_code == 200
    data = response.json()
    assert data["total"] == 0
    assert data["users"] == []
