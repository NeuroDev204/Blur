import asyncio
import logging
import threading
from contextlib import asynccontextmanager
from datetime import datetime
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from app.config import settings
from app.schemas import (
    CommentRequest, BatchCommentRequest,
    PredictionResponse, BatchPredictionResponse,
    HealthResponse,
)
from app.kafka_consumer import start_consumer
from app.kafka_producer import KafkaResultProducer
from app.services.moderation_service import ModerationService

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)


def _create_predictor():
    """Factory: tạo predictor dựa trên MODEL_BACKEND config."""
    if settings.MODEL_BACKEND == "onnx":
        from app.predictor_onnx import ToxicPredictorONNX
        return ToxicPredictorONNX()
    from app.predictor import ToxicPredictor
    return ToxicPredictor()


def _load_model_in_background(predictor, model_path: str):
    """
    Load model trong background thread.
    FastAPI sẵn sàng nhận request ngay lập tức.
    Health endpoint trả về status="starting" cho đến khi model ready.
    """
    try:
        predictor.load_model(model_path)
    except Exception as e:
        logger.error("Failed to load model: %s", e, exc_info=True)


@asynccontextmanager
async def lifespan(app: FastAPI):
    # ==================== STARTUP ====================
    app.state.start_time = datetime.now()

    # 1. Tạo predictor — KHÔNG load model, instant
    predictor = _create_predictor()
    app.state.predictor = predictor

    # 2. Load model trong background thread
    model_path = (
        settings.ONNX_MODEL_PATH
        if settings.MODEL_BACKEND == "onnx"
        else settings.MODEL_PATH
    )
    logger.info("Starting model load in background (backend=%s, path=%s)...",
                settings.MODEL_BACKEND, model_path)
    model_thread = threading.Thread(
        target=_load_model_in_background,
        args=(predictor, model_path),
        daemon=True,
    )
    model_thread.start()

    # 3. Start Kafka producer
    producer = KafkaResultProducer()
    await producer.start()

    # 4. Create moderation service
    moderation_service = ModerationService(predictor, producer)
    app.state.moderation_service = moderation_service

    # 5. Start Kafka consumer — tự chờ model ready trước khi xử lý
    consumer_task = asyncio.create_task(start_consumer(moderation_service))

    elapsed = (datetime.now() - app.state.start_time).total_seconds()
    logger.info("FastAPI ready in %.1fs (model loading in background)", elapsed)

    yield  # App đang chạy

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
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


# ==================== HTTP ENDPOINTS ====================

@app.get("/api/v1/health", response_model=HealthResponse, tags=["health"])
async def health():
    """
    Health check — Spring Boot Actuator compatible.

    status:
      - "starting": model đang load (1-2 phút đầu)
      - "healthy": model ready, sẵn sàng xử lý
      - "unhealthy": model load thất bại (timeout sau 3 phút)
    """
    uptime = (datetime.now() - app.state.start_time).total_seconds()
    predictor = app.state.predictor

    if predictor.is_ready:
        status = "healthy"
    elif uptime < 180:  # 3 phút để load
        status = "starting"
    else:
        status = "unhealthy"

    return HealthResponse(
        status=status,
        model_loaded=predictor.is_ready,
        device=str(predictor.device) if predictor.device else "loading...",
        version=settings.MODEL_VERSION,
        uptime_seconds=round(uptime, 2),
        timestamp=datetime.now().isoformat(),
    )


@app.get("/api/v1/metrics", tags=["monitoring"])
async def metrics():
    """Moderation metrics — cho Prometheus/Grafana."""
    predictor = app.state.predictor
    moderation = app.state.moderation_service
    return {
        "model_version": settings.MODEL_VERSION,
        "model_backend": settings.MODEL_BACKEND,
        "device": str(predictor.device) if predictor.device else "loading...",
        "model_ready": predictor.is_ready,
        "moderation_stats": moderation.stats,
        "thresholds": {
            "toxic": settings.TOXIC_THRESHOLD,
            "borderline": settings.BORDERLINE_THRESHOLD,
            "high_toxic": settings.HIGH_TOXIC_THRESHOLD,
        },
        "uptime_seconds": round(
            (datetime.now() - app.state.start_time).total_seconds(), 2
        ),
    }


@app.post("/api/v1/predict", response_model=PredictionResponse, tags=["prediction"])
async def predict(request: CommentRequest):
    """HTTP endpoint cho testing trực tiếp (ngoài Kafka pipeline)."""
    if not app.state.predictor.is_ready:
        raise HTTPException(
            status_code=503,
            detail="Model is still loading. Please retry in a few seconds.",
        )
    analysis = app.state.predictor.predict(request.text)
    return PredictionResponse(
        success=True,
        data=analysis,
        timestamp=datetime.now().isoformat(),
    )


@app.post("/api/v1/predict/batch", response_model=BatchPredictionResponse, tags=["prediction"])
async def predict_batch(request: BatchCommentRequest):
    """Batch prediction (tối đa 100 comments)."""
    if not app.state.predictor.is_ready:
        raise HTTPException(
            status_code=503,
            detail="Model is still loading. Please retry in a few seconds.",
        )
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
                "toxic_percentage": round(toxic_count / total * 100, 2) if total > 0 else 0,
            },
        },
        timestamp=datetime.now().isoformat(),
    )


@app.get("/", tags=["health"])
async def root():
    return {
        "service": "Blur Model Service",
        "version": "2.0.0",
        "docs": "/docs",
        "health": "/api/v1/health",
        "metrics": "/api/v1/metrics",
        "model_ready": app.state.predictor.is_ready,
    }
