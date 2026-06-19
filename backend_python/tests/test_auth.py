import pytest


@pytest.mark.asyncio
async def test_login_init_no_user(client):
    response = await client.post(
        "/api/v1/auth/login/init",
        json={"email": "nonexistent@example.com"},
    )
    assert response.status_code == 200
    data = response.json()
    assert data["enrolled"] is False
    assert data["enroll_token"] is None
    assert data["qr_data_url"] is None


@pytest.mark.asyncio
async def test_login_invalid_totp(client):
    # First init setup to create a user
    await client.post(
        "/api/v1/setup/init",
        json={
            "email": "admin@example.com",
            "full_name": "Admin User",
            "username": "admin",
        },
    )

    response = await client.post(
        "/api/v1/auth/login",
        json={"email": "admin@example.com", "totp_code": "123456"},
    )
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_login_recovery_no_user(client):
    response = await client.post(
        "/api/v1/auth/login/recovery",
        json={"email": "nonexistent@example.com", "recovery_code": "ABCD1234"},
    )
    assert response.status_code == 401
