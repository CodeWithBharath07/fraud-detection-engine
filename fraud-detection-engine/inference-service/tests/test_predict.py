import pytest
from unittest.mock import patch, MagicMock
from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health_returns_ok():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "ok"


def test_predict_returns_fraud_for_high_risk():
    with patch("app.routers.predict.model_loader") as mock_loader:
        mock_loader.is_loaded = True
        mock_loader.predict.return_value = (0.95, "high_amount_foreign_country")

        response = client.post("/predict", json={
            "transaction_id": "txn-001",
            "features": {
                "amount": 15000.0,
                "hour_of_day": 2,
                "day_of_week": 6,
                "card_present": 0,
                "merchant_category": "online",
                "country": "CN",
                "currency": "CNY",
                "recent_txn_count": 12
            }
        })

    assert response.status_code == 200
    data = response.json()
    assert data["is_fraud"] is True
    assert data["risk_level"] == "CRITICAL"
    assert data["fraud_probability"] >= 0.9


def test_predict_returns_clean_for_low_risk():
    with patch("app.routers.predict.model_loader") as mock_loader:
        mock_loader.is_loaded = True
        mock_loader.predict.return_value = (0.05, "normal_pattern")

        response = client.post("/predict", json={
            "transaction_id": "txn-002",
            "features": {
                "amount": 42.50,
                "hour_of_day": 14,
                "day_of_week": 2,
                "card_present": 1,
                "merchant_category": "retail",
                "country": "US",
                "currency": "USD",
                "recent_txn_count": 1
            }
        })

    assert response.status_code == 200
    data = response.json()
    assert data["is_fraud"] is False
    assert data["risk_level"] == "LOW"


def test_predict_returns_503_when_model_not_loaded():
    with patch("app.routers.predict.model_loader") as mock_loader:
        mock_loader.is_loaded = False

        response = client.post("/predict", json={
            "transaction_id": "txn-003",
            "features": {"amount": 100.0}
        })

    assert response.status_code == 503
