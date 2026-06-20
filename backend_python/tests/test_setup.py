import pytest


@pytest.mark.asyncio
async def test_setup_status_needs_setup(client):
    response = await client.get("/api/v1/setup/status")
    assert response.status_code == 200
    data = response.json()
    assert data["needs_setup"] is True


@pytest.mark.asyncio
async def test_setup_init(client):
    response = await client.post(
        "/api/v1/setup/init",
        json={
            "email": "admin@example.com",
            "full_name": "Admin User",
        },
    )
    assert response.status_code == 200
    data = response.json()
    assert "qr_data_url" in data
    assert "setup_token" in data


@pytest.mark.asyncio
async def test_setup_init_twice_fails(client):
    await client.post(
        "/api/v1/setup/init",
        json={
            "email": "admin@example.com",
            "full_name": "Admin User",
        },
    )
    response = await client.post(
        "/api/v1/setup/init",
        json={
            "email": "admin2@example.com",
            "full_name": "Admin Two",
        },
    )
    assert response.status_code == 400
    assert "already set up" in response.json()["detail"]


@pytest.mark.asyncio
async def test_setup_status_after_init(client):
    await client.post(
        "/api/v1/setup/init",
        json={
            "email": "admin@example.com",
            "full_name": "Admin User",
        },
    )
    response = await client.get("/api/v1/setup/status")
    assert response.status_code == 200
    data = response.json()
    assert data["needs_setup"] is False
