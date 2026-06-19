import pytest
import pytest_asyncio

from app.core.encryption import field_encryption
from app.core.security import create_access_token
from app.models.user import User


@pytest_asyncio.fixture
async def auth_token(db_session):

    enc_email, ver = field_encryption.encrypt("admin@example.com")
    enc_name, _ = field_encryption.encrypt("Admin User")
    enc_username, _ = field_encryption.encrypt("admin")
    enc_role, _ = field_encryption.encrypt("admin")

    user = User(
        email_encrypted=enc_email,
        full_name_encrypted=enc_name,
        username_encrypted=enc_username,
        role_encrypted=enc_role,
        email_hash=field_encryption.blind_index("admin@example.com"),
        username_hash=field_encryption.blind_index("admin"),
        role_hash=field_encryption.blind_index("admin"),
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
