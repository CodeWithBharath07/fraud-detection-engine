from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import Dict, Any
import logging

from app.model_loader import model_loader, MERCHANT_CATEGORIES, COUNTRIES, CURRENCIES

logger = logging.getLogger(__name__)
router = APIRouter()


class PredictRequest(BaseModel):
    transaction_id: str
    features: Dict[str, Any]


class PredictResponse(BaseModel):
    transaction_id: str
    fraud_probability: float
    is_fraud: bool
    reason: str
    risk_level: str


def encode_categorical(value: str, categories: list) -> int:
    try:
        return categories.index(value.lower())
    except (ValueError, AttributeError):
        return len(categories) - 1  # unknown


@router.post("/predict", response_model=PredictResponse)
async def predict(request: PredictRequest):
    if not model_loader.is_loaded:
        raise HTTPException(status_code=503, detail="Model not loaded yet")

    f = request.features
    feature_vector = [
        float(f.get("amount", 0)),
        int(f.get("hour_of_day", 12)),
        int(f.get("day_of_week", 1)),
        int(f.get("card_present", 1)),
        encode_categorical(str(f.get("merchant_category", "unknown")), MERCHANT_CATEGORIES),
        encode_categorical(str(f.get("country", "US")), COUNTRIES),
        encode_categorical(str(f.get("currency", "USD")), CURRENCIES),
        int(f.get("recent_txn_count", 0)),
    ]

    fraud_prob, reason = model_loader.predict(feature_vector)
    risk_level = (
        "CRITICAL" if fraud_prob >= 0.9
        else "HIGH" if fraud_prob >= 0.7
        else "MEDIUM" if fraud_prob >= 0.5
        else "LOW"
    )

    return PredictResponse(
        transaction_id=request.transaction_id,
        fraud_probability=fraud_prob,
        is_fraud=fraud_prob >= 0.7,
        reason=reason,
        risk_level=risk_level,
    )
