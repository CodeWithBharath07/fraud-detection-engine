from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
import logging

from app.routers import predict
from app.model_loader import model_loader

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Loading fraud detection ML model...")
    model_loader.load()
    logger.info(f"Model loaded. Precision: {model_loader.precision:.4f}")
    yield
    logger.info("Shutting down inference service")


app = FastAPI(
    title="Fraud Detection Inference Service",
    description="Scikit-learn anomaly detection model serving 10,000+ events/sec with sub-100ms latency",
    version="1.0.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(predict.router, tags=["predict"])


@app.get("/health")
async def health():
    return {
        "status": "ok",
        "model_loaded": model_loader.is_loaded,
        "model_precision": model_loader.precision,
    }
