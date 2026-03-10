

import asyncio
import logging
from contextlib import asynccontextmanager
from datetime import datetime
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.schemas import (
    CommentRequest, BatchCommentRequest,
    PredictionResponse, BatchPredictionResponse,
    HealthResponse
)
from app.predictor import ToxicPredictor
from app.kafka_consumer import start_consumer
from app.kafka_producer import KafkaResultProducer
from app.services.moderation_service import ModerationService

# Cấu hình logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s"
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    Lifespan context manager - thay thế @app.on_event("startup")/"shutdown").

    Ưu điểm so với on_event:
    1. Không deprecated (FastAPI 0.103+)
    2. Cleanup đảm bảo chạy (finally block)
    3. Share state qua app.state thay vì global vars
    4. Dễ test (inject mock dependencies)
    """
    # ==================== STARTUP ====================

    # 1. Load PhoBERT model
    logger.info("Loading PhoBERT model from: %s", settings.MODEL_PATH)
    predictor = ToxicPredictor(settings.MODEL_PATH)
    logger.info("PhoBERT loaded successfully on device: %s", predictor.device)

    # 2. Start Kafka producer
    producer = KafkaResultProducer()
    await producer.start()

    # 3. Create moderation service
    moderation_service = ModerationService(predictor, producer)

    # 4. Start Kafka consumer as background task
    consumer_task = asyncio.create_task(start_consumer(moderation_service))
    logger.info("Kafka consumer task started")

    # Lưu vào app.state cho HTTP endpoints truy cập
    app.state.predictor = predictor
    app.state.start_time = datetime.now()

    yield  # App đang chạy, xử lý requests

    # ==================== SHUTDOWN ====================
    logger.info("Shutting down...")
    consumer_task.cancel()
    try:
        await consumer_task
    except asyncio.CancelledError:
        pass
    await producer.stop()
    logger.info("Shutdown complete")


# ==================== FASTAPI APP ====================

app = FastAPI(
    title="Blur Model Service",
    description="Vietnamese Toxic Comment Detection with PhoBERT + Kafka Pipeline",
    version="2.0.0",
    lifespan=lifespan
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


# ==================== HTTP ENDPOINTS ====================
# Chỉ dùng cho testing/debugging
# Production traffic đi qua Kafka pipeline

@app.get("/api/v1/health", response_model=HealthResponse, tags=["health"])
async def health():
    """Health check - Spring Boot Actuator compatible."""
    uptime = (datetime.now() - app.state.start_time).total_seconds()
    return HealthResponse(
        status="healthy" if hasattr(app.state, 'predictor') else "unhealthy",
        model_loaded=hasattr(app.state, 'predictor'),
        device=str(app.state.predictor.device),
        version="2.0.0",
        uptime_seconds=round(uptime, 2),
        timestamp=datetime.now().isoformat()
    )


@app.post("/api/v1/predict", response_model=PredictionResponse, tags=["prediction"])
async def predict(request: CommentRequest):
    """
    HTTP endpoint cho testing trực tiếp (ngoài Kafka pipeline).

    Dùng Postman/curl để test:
    POST http://localhost:8000/api/v1/predict
    Body: {"text": "Nội dung comment cần phân tích"}
    """
    if not hasattr(app.state, 'predictor'):
        raise HTTPException(status_code=503, detail="Model not loaded")

    analysis = app.state.predictor.predict(request.text)
    return PredictionResponse(
        success=True,
        data=analysis,
        timestamp=datetime.now().isoformat()
    )


@app.post("/api/v1/predict/batch", response_model=BatchPredictionResponse, tags=["prediction"])
async def predict_batch(request: BatchCommentRequest):
    """
    Batch prediction (tối đa 100 comments).
    Hiệu quả hơn gọi /predict nhiều lần vì dùng batch inference.
    """
    if not hasattr(app.state, 'predictor'):
        raise HTTPException(status_code=503, detail="Model not loaded")

    results = app.state.predictor.predict_batch(request.texts)

    toxic_count = sum(1 for r in results if r.is_toxic)
    total = len(results)

    return BatchPredictionResponse(
        success=True,
        data={
            "predictions": [r.model_dump() for r in results],
            "summary": {
                "total": total,
                "toxic_count": toxic_count,
                "clean_count": total - toxic_count,
                "toxic_percentage": round(toxic_count / total * 100, 2) if total > 0 else 0
            }
        },
        timestamp=datetime.now().isoformat()
    )


# ==================== BACKWARD COMPATIBILITY ====================

@app.get("/", tags=["health"])
async def root():
    """Root endpoint - API info."""
    return {
        "service": "Blur Model Service",
        "version": "2.0.0",
        "docs": "/docs",
        "health": "/api/v1/health"
    }