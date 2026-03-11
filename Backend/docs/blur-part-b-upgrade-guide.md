# PHẦN B: NÂNG CẤP KIẾN TRÚC BLUR PLATFORM - HƯỚNG DẪN ĐẦY ĐỦ

> **Mục tiêu:** Tài liệu hướng dẫn từng bước nâng cấp Blur Platform lên production-grade.
> Mỗi file code được viết ĐẦY ĐỦ 100%, không viết tắt, không pseudocode.
> Bắt đầu từ refactor model-service, sau đó đến các giai đoạn upgrade khác.

---

## MỤC LỤC

1. [GĐ 1: Refactor Model Service](#gd-1-refactor-model-service)
2. [GĐ 2: Async AI Moderation Pipeline](#gd-2-async-ai-moderation-pipeline)
3. [GĐ 3: Neo4j Follow Recommendation](#gd-3-neo4j-follow-recommendation)
4. [GĐ 4: Outbox Pattern + Event-Driven](#gd-4-outbox-pattern--event-driven)
5. [GĐ 5: Saga Choreography - Delete User](#gd-5-saga-choreography---delete-user)
6. [GĐ 6: CQRS + Feed Read Model](#gd-6-cqrs--feed-read-model)
7. [GĐ 7: Redis Cache Nâng Cao](#gd-7-redis-cache-nâng-cao)
8. [GĐ 8: Keycloak Migration](#gd-8-keycloak-migration)
9. [GĐ 9: Resilience4j](#gd-9-resilience4j)
10. [GĐ 10: Rate Limiting + Frontend Upgrades](#gd-10-rate-limiting--frontend-upgrades)

---

## GĐ 1: REFACTOR MODEL SERVICE

> **Mục tiêu:** Refactor model-service từ code cũ (global state, deprecated API, bugs) thành
> cấu trúc production-grade với Kafka async pipeline, tách biệt concerns rõ ràng.
>
> **Vấn đề hiện tại:**
> 1. `app/config.py`: import sai (`pydantic_core` thay vì `pydantic_settings`), path model sai
> 2. `app/schemas.py`: `CommentRequest.texts` sai (phải là `text: str`), `toxic_level = ToxicLevel` thiếu annotation
> 3. `app/kafka_consumer.py`: chưa hoàn chỉnh (thiếu `await consumer.start()`, thiếu try/finally)
> 4. `api/server.py`: dùng `@app.on_event("startup")` deprecated, global state, trùng lặp với `app/`
> 5. Thiếu `kafka_producer.py`, `moderation_service.py`, `main.py` với lifespan
> 6. Thư mục `legacy/` và `inference/` trùng lặp cần xóa

### 1.1 Cấu trúc thư mục MỚI (sau refactor)

```
model-service/
├── app/                              # PRODUCTION RUNTIME
│   ├── __init__.py
│   ├── main.py                       # FastAPI app + lifespan (load model, start Kafka)
│   ├── config.py                     # Pydantic Settings từ environment
│   ├── schemas.py                    # Tất cả Pydantic request/response DTOs
│   ├── predictor.py                  # ToxicPredictor class (PhoBERT inference)
│   ├── kafka_consumer.py             # aiokafka consumer loop
│   ├── kafka_producer.py             # aiokafka producer wrapper  ← MỚI
│   └── services/
│       ├── __init__.py
│       └── moderation_service.py     # Orchestrate predict + publish  ← MỚI
│
├── scraper/                          # Thu thập dữ liệu (giữ nguyên)
├── training/                         # Training scripts (giữ nguyên)
├── models/                           # Trained model weights (giữ nguyên)
├── data/                             # Datasets (giữ nguyên)
├── config/                           # URL lists cho scraping (giữ nguyên)
├── collect_dataset.py                # Entry point thu thập data
├── run_pipeline.py                   # Pipeline orchestrator
├── requirements.txt                  # Dependencies (cập nhật)
├── Dockerfile                        # Docker build
└── .env                              # Environment variables
```

**XÓA:**
- `legacy/` → code cũ deprecated
- `inference/` → trùng chức năng với `app/predictor.py`
- `api/models.py` → trùng 100% với schemas
- `api/server.py` → thay thế bằng `app/main.py`
- `api/__init__.py`
- `api/__pycache__/`

### 1.2 File 1: `app/config.py` (SỬA LỖI + CẢI TIẾN)

**Lỗi cũ:**
- Import sai: `from pydantic_core import BaseSettings` → phải là `from pydantic_settings import BaseSettings`
- Path model sai: `model/phobert_toxic_FINAL_2025111_232134` (thiếu `s` ở `models`, thiếu `1` ở ngày)
- Tên biến không nhất quán: `KAFKA_BOOTSTRAP_SERVER` (thiếu `S`)

**File mới đầy đủ:**

```python
"""
Configuration module cho Model Service.
Đọc settings từ environment variables hoặc .env file.
Dùng pydantic-settings v2 để validate và type-safe.
"""

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """
    Tất cả config cho model-service.
    Mỗi field map 1:1 với environment variable cùng tên.
    Ví dụ: KAFKA_BOOTSTRAP_SERVERS=kafka:9093 trong .env
    """

    # ==================== KAFKA ====================
    KAFKA_BOOTSTRAP_SERVERS: str = "localhost:9092"
    KAFKA_CONSUMER_GROUP: str = "model-service"
    KAFKA_REQUEST_TOPIC: str = "comment-moderation-requests"
    KAFKA_RESULT_TOPIC: str = "comment-moderation-results"

    # ==================== PHOBERT MODEL ====================
    MODEL_PATH: str = "models/phobert_toxic_FINAL_20251111_232134"
    MODEL_VERSION: str = "phobert_toxic_FINAL"
    MAX_TOKEN_LENGTH: int = 256
    TOXIC_THRESHOLD: float = 0.5

    # ==================== SERVER ====================
    HOST: str = "0.0.0.0"
    PORT: int = 8000

    class Config:
        env_file = ".env"
        # Cho phép đọc từ .env file, environment variables override .env
        extra = "ignore"


# Singleton instance - import settings từ bất kỳ module nào
settings = Settings()
```

**Giải thích thay đổi:**
1. `pydantic_settings` là package riêng từ Pydantic v2 (cần install: `pip install pydantic-settings`)
2. `KAFKA_BOOTSTRAP_SERVERS` (có `S`) khớp với convention của Kafka config
3. `KAFKA_REQUEST_TOPIC` đổi thành `comment-moderation-requests` khớp với content-service producer
4. `MODEL_PATH` sửa đúng path: `models/` (có `s`), ngày `20251111` (đủ 8 chữ số)
5. `extra = "ignore"` để không crash khi có env vars thừa

### 1.3 File 2: `app/schemas.py` (SỬA LỖI + HOÀN CHỈNH)

**Lỗi cũ:**
- `CommentRequest.texts` phải là `text: str` (single comment, không phải list)
- `BatchCommentRequest` có lỗi cú pháp: `texts = List[str]` (thiếu type annotation `:`)
- `CommentAnalysis.toxic_level = ToxicLevel` thiếu `:` (assignment thay vì annotation)

**File mới đầy đủ:**

```python
"""
Pydantic schemas (DTOs) cho Model Service.
Định nghĩa tất cả request/response models.
"""

from pydantic import BaseModel, Field
from typing import List, Dict, Any
from enum import Enum


class ToxicLevel(str, Enum):
    """Mức độ toxic: low < 0.4 < medium < 0.7 < high"""
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"


# ==================== REQUEST DTOs ====================

class CommentRequest(BaseModel):
    """
    Request body cho single comment prediction.
    Dùng cho HTTP endpoint POST /api/v1/predict
    """
    text: str = Field(
        ...,
        min_length=1,
        max_length=5000,
        description="Nội dung comment cần phân tích"
    )


class BatchCommentRequest(BaseModel):
    """
    Request body cho batch prediction.
    Dùng cho HTTP endpoint POST /api/v1/predict/batch
    Tối đa 100 comments/request.
    """
    texts: List[str] = Field(
        ...,
        min_length=1,
        max_length=100,
        description="Danh sách comments cần phân tích"
    )


# ==================== RESPONSE DTOs ====================

class CommentAnalysis(BaseModel):
    """
    Kết quả phân tích 1 comment.
    Được dùng trong cả HTTP response và Kafka result message.
    """
    text: str
    is_toxic: bool
    toxic_score: float = Field(..., ge=0, le=1, description="Điểm toxic (0.0 = sạch, 1.0 = rất toxic)")
    toxic_level: ToxicLevel = Field(..., description="Mức độ: low/medium/high")
    confidence: float = Field(..., ge=0, le=1, description="Độ tin cậy của model")
    probabilities: Dict[str, float] = Field(..., description="Xác suất từng class: {clean, toxic}")


class PredictionResponse(BaseModel):
    """Response wrapper cho single prediction qua HTTP"""
    success: bool = True
    data: CommentAnalysis
    timestamp: str


class BatchPredictionResponse(BaseModel):
    """Response wrapper cho batch prediction qua HTTP"""
    success: bool = True
    data: Dict[str, Any]
    timestamp: str


class HealthResponse(BaseModel):
    """Health check response - tương thích Spring Boot Actuator format"""
    status: str = Field(..., description="healthy / unhealthy / degraded")
    model_loaded: bool
    device: str = Field(..., description="cpu hoặc cuda")
    version: str
    uptime_seconds: float
    timestamp: str
```

### 1.4 File 3: `app/predictor.py` (HOÀN CHỈNH - format lại)

**Thay đổi:** Format code chuẩn PEP8, thêm docstring chi tiết, giữ nguyên logic.

```python
"""
PhoBERT Toxic Comment Predictor.
Load model PhoBERT đã fine-tune và chạy inference.
Đây là engine duy nhất - không có Gemini, không có canary routing.
"""

import torch
from transformers import AutoTokenizer, AutoModelForSequenceClassification
from typing import List
from app.schemas import CommentAnalysis, ToxicLevel


class ToxicPredictor:
    """
    PhoBERT-based Vietnamese toxic comment predictor.

    Model: vinai/phobert-base fine-tuned trên dataset toxic comments tiếng Việt.
    Input:  text string (tối đa 256 tokens sau tokenize)
    Output: CommentAnalysis với is_toxic, toxic_score, confidence, probabilities

    Performance: ~50-200ms/comment trên CPU, ~10-30ms trên GPU
    """

    def __init__(self, model_path: str):
        """
        Load model và tokenizer từ local path.

        Args:
            model_path: đường dẫn đến thư mục chứa model weights
                        VD: "models/phobert_toxic_FINAL_20251111_232134"
        """
        self.device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
        self.tokenizer = AutoTokenizer.from_pretrained(model_path)
        self.model = AutoModelForSequenceClassification.from_pretrained(model_path)
        self.model.to(self.device)
        self.model.eval()  # Tắt dropout, batchnorm training mode

    def _get_toxic_level(self, score: float) -> ToxicLevel:
        """
        Chuyển toxic_score thành mức độ.
        < 0.4 = LOW, 0.4-0.7 = MEDIUM, > 0.7 = HIGH
        """
        if score < 0.4:
            return ToxicLevel.LOW
        elif score < 0.7:
            return ToxicLevel.MEDIUM
        return ToxicLevel.HIGH

    def predict(self, text: str) -> CommentAnalysis:
        """
        Predict single comment.

        Flow:
        1. Tokenize text → input_ids, attention_mask (max 256 tokens)
        2. Forward pass qua PhoBERT → logits [batch=1, classes=2]
        3. Softmax → probabilities [clean_prob, toxic_prob]
        4. toxic_prob > 0.5 → is_toxic = True

        Args:
            text: nội dung comment cần phân tích

        Returns:
            CommentAnalysis với đầy đủ thông tin
        """
        inputs = self.tokenizer(
            text,
            padding='max_length',
            truncation=True,
            max_length=256,
            return_tensors='pt'
        )
        inputs = {k: v.to(self.device) for k, v in inputs.items()}

        with torch.no_grad():
            outputs = self.model(**inputs)
            probs = torch.softmax(outputs.logits, dim=1)

        toxic_prob = probs[0][1].item()
        clean_prob = probs[0][0].item()

        return CommentAnalysis(
            text=text,
            is_toxic=toxic_prob > 0.5,
            toxic_score=round(toxic_prob, 4),
            toxic_level=self._get_toxic_level(toxic_prob),
            confidence=round(max(toxic_prob, clean_prob), 4),
            probabilities={"clean": round(clean_prob, 4), "toxic": round(toxic_prob, 4)}
        )

    def predict_batch(self, texts: List[str]) -> List[CommentAnalysis]:
        """
        Predict batch of comments.
        Hiệu quả hơn predict() lặp vì dùng batch inference.

        Args:
            texts: danh sách comments (tối đa 100)

        Returns:
            List[CommentAnalysis] cùng thứ tự với input
        """
        inputs = self.tokenizer(
            texts,
            padding='max_length',
            truncation=True,
            max_length=256,
            return_tensors='pt'
        )
        inputs = {k: v.to(self.device) for k, v in inputs.items()}

        with torch.no_grad():
            outputs = self.model(**inputs)
            probs = torch.softmax(outputs.logits, dim=1)

        results = []
        for i, text in enumerate(texts):
            toxic_prob = probs[i][1].item()
            clean_prob = probs[i][0].item()
            results.append(CommentAnalysis(
                text=text,
                is_toxic=toxic_prob > 0.5,
                toxic_score=round(toxic_prob, 4),
                toxic_level=self._get_toxic_level(toxic_prob),
                confidence=round(max(toxic_prob, clean_prob), 4),
                probabilities={"clean": round(clean_prob, 4), "toxic": round(toxic_prob, 4)}
            ))
        return results
```

### 1.5 File 4: `app/kafka_consumer.py` (VIẾT LẠI HOÀN CHỈNH)

**Lỗi cũ:** File chỉ có 22 dòng, thiếu `await consumer.start()`, thiếu `try/finally`, thiếu xử lý message loop.

**File mới đầy đủ:**

```python
"""
Kafka Consumer cho Model Service.
Consume từ topic 'comment-moderation-requests' và gọi ModerationService xử lý.
Dùng aiokafka cho async I/O tương thích với FastAPI event loop.
"""

import json
import logging
from aiokafka import AIOKafkaConsumer
from app.config import settings

logger = logging.getLogger(__name__)


async def start_consumer(moderation_service):
    """
    Khởi tạo và chạy Kafka consumer loop.

    Consume từ topic 'comment-moderation-requests'.
    Mỗi message chứa: { commentId, postId, userId, content }

    Flow:
    1. Tạo AIOKafkaConsumer với group_id = "model-service"
    2. Start consumer (kết nối Kafka broker)
    3. Infinite loop: nhận message → gọi moderation_service.moderate_comment()
    4. Nếu lỗi 1 message → log error, tiếp tục message tiếp theo
    5. Khi shutdown (cancel task) → stop consumer

    Args:
        moderation_service: instance của ModerationService chứa predictor + producer
    """
    consumer = AIOKafkaConsumer(
        settings.KAFKA_REQUEST_TOPIC,
        bootstrap_servers=settings.KAFKA_BOOTSTRAP_SERVERS,
        group_id=settings.KAFKA_CONSUMER_GROUP,
        value_deserializer=lambda m: json.loads(m.decode('utf-8')),
        auto_offset_reset='earliest',
        enable_auto_commit=True,
    )

    await consumer.start()
    logger.info(
        "Kafka consumer started | topic=%s | group=%s | servers=%s",
        settings.KAFKA_REQUEST_TOPIC,
        settings.KAFKA_CONSUMER_GROUP,
        settings.KAFKA_BOOTSTRAP_SERVERS
    )

    try:
        async for msg in consumer:
            try:
                logger.debug(
                    "Received message | topic=%s | partition=%s | offset=%s",
                    msg.topic, msg.partition, msg.offset
                )
                await moderation_service.moderate_comment(msg.value)
            except Exception as e:
                # Log lỗi nhưng KHÔNG crash consumer
                # Message lỗi sẽ bị skip (auto commit offset)
                logger.error(
                    "Error processing message | offset=%s | error=%s",
                    msg.offset, str(e),
                    exc_info=True
                )
    finally:
        await consumer.stop()
        logger.info("Kafka consumer stopped")
```

### 1.6 File 5: `app/kafka_producer.py` (MỚI HOÀN TOÀN)

**Mục đích:** Gửi kết quả moderation về topic `comment-moderation-results` cho content-service consume.

```python
"""
Kafka Producer cho Model Service.
Gửi kết quả moderation tới topic 'comment-moderation-results'.
Content-service sẽ consume và update Comment entity.
"""

import json
import logging
from aiokafka import AIOKafkaProducer
from app.config import settings

logger = logging.getLogger(__name__)


class KafkaResultProducer:
    """
    Wrapper cho AIOKafkaProducer.
    Quản lý lifecycle (start/stop) và serialize message.

    Sử dụng:
        producer = KafkaResultProducer()
        await producer.start()       # Gọi 1 lần khi startup
        await producer.send_result(result_dict)  # Gọi mỗi khi có kết quả
        await producer.stop()        # Gọi khi shutdown
    """

    def __init__(self):
        self._producer = None

    async def start(self):
        """
        Kết nối đến Kafka broker.
        Phải gọi trước khi send_result().
        """
        self._producer = AIOKafkaProducer(
            bootstrap_servers=settings.KAFKA_BOOTSTRAP_SERVERS,
            value_serializer=lambda v: json.dumps(v).encode('utf-8'),
        )
        await self._producer.start()
        logger.info(
            "Kafka producer started | servers=%s | topic=%s",
            settings.KAFKA_BOOTSTRAP_SERVERS,
            settings.KAFKA_RESULT_TOPIC
        )

    async def send_result(self, result: dict):
        """
        Gửi kết quả moderation tới Kafka topic.

        Args:
            result: dict chứa {commentId, postId, status, confidence, toxicScore,
                    toxicLevel, modelVersion, moderatedAt}

        Message được gửi tới topic: comment-moderation-results
        Content-service (Java) sẽ consume bằng @KafkaListener
        """
        if self._producer is None:
            raise RuntimeError("Producer not started. Call start() first.")

        await self._producer.send_and_wait(
            settings.KAFKA_RESULT_TOPIC,
            value=result
        )
        logger.debug(
            "Result sent | topic=%s | commentId=%s",
            settings.KAFKA_RESULT_TOPIC,
            result.get("commentId", "unknown")
        )

    async def stop(self):
        """Đóng kết nối đến Kafka broker."""
        if self._producer:
            await self._producer.stop()
            logger.info("Kafka producer stopped")
```

### 1.7 File 6: `app/services/__init__.py`

```python
# Services package
```

### 1.8 File 7: `app/services/moderation_service.py` (MỚI HOÀN TOÀN)

**Mục đích:** Orchestrate PhoBERT prediction và publish kết quả. Đây là "brain" của pipeline.

```python
"""
Moderation Service - Orchestrate PhoBERT inference + Kafka publish.

Flow:
1. Nhận message từ Kafka consumer: {commentId, postId, userId, content}
2. Chạy PhoBERT predict trên content
3. Map kết quả sang format mà content-service hiểu:
   - APPROVED = comment an toàn, hiển thị bình thường
   - REJECTED = comment toxic, ẩn đi
4. Publish kết quả lên Kafka topic comment-moderation-results
"""

import asyncio
import logging
from datetime import datetime
from app.predictor import ToxicPredictor
from app.kafka_producer import KafkaResultProducer
from app.config import settings

logger = logging.getLogger(__name__)


class ModerationService:
    """
    Service xử lý moderation request.

    Tại sao dùng run_in_executor?
    - PhoBERT inference là SYNCHRONOUS (PyTorch forward pass)
    - Mất 50-200ms trên CPU cho mỗi comment
    - Nếu chạy trực tiếp trên event loop → BLOCK toàn bộ async operations
    - Block = Kafka consumer không heartbeat → bị kick khỏi group
    - run_in_executor chạy inference trên thread pool, event loop vẫn free
    """

    def __init__(self, predictor: ToxicPredictor, producer: KafkaResultProducer):
        """
        Args:
            predictor: ToxicPredictor instance đã load model
            producer: KafkaResultProducer instance đã start
        """
        self.predictor = predictor
        self.producer = producer

    async def moderate_comment(self, message: dict):
        """
        Xử lý 1 moderation request.

        Input message (từ content-service):
        {
            "commentId": "65f8a1b2c3d4e5f6a7b8c9d0",
            "postId": "65f8a1b2c3d4e5f6a7b8c9d1",
            "userId": "user-uuid-123",
            "content": "Nội dung bình luận cần kiểm duyệt"
        }

        Output message (gửi về content-service):
        {
            "commentId": "65f8a1b2c3d4e5f6a7b8c9d0",
            "postId": "65f8a1b2c3d4e5f6a7b8c9d1",
            "status": "APPROVED",          # hoặc "REJECTED"
            "confidence": 0.9512,
            "toxicScore": 0.0488,
            "toxicLevel": "low",           # low / medium / high
            "modelVersion": "phobert_toxic_FINAL",
            "moderatedAt": "2026-03-04T10:30:00"
        }
        """
        comment_id = message.get("commentId", "unknown")
        content = message.get("content", "")

        if not content.strip():
            logger.warning("Empty content for comment %s, skipping", comment_id)
            return

        logger.info("Moderating comment %s (length=%d)", comment_id, len(content))

        # Chạy PhoBERT inference qua thread pool executor
        # Tránh block event loop (giải thích ở class docstring)
        loop = asyncio.get_event_loop()
        analysis = await loop.run_in_executor(
            None,  # Dùng default ThreadPoolExecutor
            self.predictor.predict,
            content
        )

        # Map sang ModerationStatus enum khớp với Java ModerationStatus.java
        # APPROVED = an toàn, REJECTED = toxic
        status = "REJECTED" if analysis.is_toxic else "APPROVED"

        result = {
            "commentId": comment_id,
            "postId": message.get("postId"),
            "status": status,
            "confidence": analysis.confidence,
            "toxicScore": analysis.toxic_score,
            "toxicLevel": analysis.toxic_level.value,
            "modelVersion": settings.MODEL_VERSION,
            "moderatedAt": datetime.now().isoformat(),
        }

        # Publish kết quả lên Kafka
        await self.producer.send_result(result)

        logger.info(
            "Comment %s → %s (confidence=%.4f, toxicScore=%.4f)",
            comment_id, status, analysis.confidence, analysis.toxic_score
        )
```

### 1.9 File 8: `app/main.py` (VIẾT LẠI HOÀN TOÀN - thay thế `api/server.py`)

**Tại sao viết lại?**
- `api/server.py` dùng `@app.on_event("startup")` → deprecated từ FastAPI 0.103+
- Global state (`predictor`, `nlp_labeler`, `start_time`) → khó test, không thread-safe
- Không có Kafka integration
- Code 630 dòng nhưng 60% là docstring/example trong OpenAPI

**File mới dùng `lifespan` context manager (best practice FastAPI):**

```python
"""
FastAPI Application cho Blur Model Service.
Vietnamese Toxic Comment Detection with PhoBERT + Kafka Pipeline.

Startup flow:
1. Load PhoBERT model từ disk → ToxicPredictor
2. Start Kafka producer → KafkaResultProducer
3. Create ModerationService (predictor + producer)
4. Start Kafka consumer as background task

Shutdown flow:
1. Cancel consumer task
2. Stop producer

HTTP endpoints chỉ dùng cho testing/debugging,
production traffic đi qua Kafka pipeline.
"""

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
```

### 1.10 File 9: `app/__init__.py`

```python
# Model Service App Package
```

### 1.11 File 10: `requirements.txt` (CẬP NHẬT)

```txt
# ==================== CORE ====================
fastapi>=0.109.0
uvicorn[standard]>=0.25.0

# ==================== ML ====================
torch>=2.0.0
transformers>=4.30.0

# ==================== KAFKA ====================
aiokafka>=0.10.0

# ==================== CONFIG ====================
pydantic>=2.0.0
pydantic-settings>=2.0.0

# ==================== DATA COLLECTION (giữ nguyên) ====================
yt-dlp>=2024.1.1
google-api-python-client>=2.100.0
TikTokApi>=6.3.0
playwright>=1.40.0
pandas>=2.0.0
openpyxl>=3.1.0
python-dotenv>=1.0.0

# ==================== TRAINING (giữ nguyên) ====================
scikit-learn>=1.3.0
matplotlib>=3.8.0
seaborn>=0.13.0
```

### 1.12 File 11: `.env` (MẪU)

```env
# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_CONSUMER_GROUP=model-service
KAFKA_REQUEST_TOPIC=comment-moderation-requests
KAFKA_RESULT_TOPIC=comment-moderation-results

# PhoBERT Model
MODEL_PATH=models/phobert_toxic_FINAL_20251111_232134
MODEL_VERSION=phobert_toxic_FINAL

# Server
HOST=0.0.0.0
PORT=8000
```

### 1.13 File 12: `Dockerfile`

```dockerfile
FROM python:3.11-slim

WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc g++ && \
    rm -rf /var/lib/apt/lists/*

# Install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy source code
COPY app/ ./app/
COPY models/ ./models/
COPY .env .env

# Expose port
EXPOSE 8000

# Run FastAPI with uvicorn
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
```

### 1.14 Sequence Diagram - Kafka Pipeline

```
┌──────────────────────────────────────────────────────────────────────────┐
│                    ASYNC AI MODERATION PIPELINE                          │
├──────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  Content Service (Java)              Model Service (Python)              │
│  ─────────────────────              ─────────────────────               │
│        │                                    │                            │
│  1. User comment                            │                            │
│  2. Save (PENDING_MODERATION)               │                            │
│  3. Return 202 + jobId                      │                            │
│        │                                    │                            │
│  4. Publish → Kafka                         │                            │
│     "comment-moderation-requests"  ────────>│                            │
│        │                                    │                            │
│        │                              5. Consumer nhận msg               │
│        │                              6. PhoBERT predict()               │
│        │                                 (run_in_executor)               │
│        │                              7. Map → APPROVED/REJECTED         │
│        │                                    │                            │
│        │                              8. Publish → Kafka                 │
│  9. @KafkaListener nhận    <────────────    │                            │
│     "comment-moderation-results"     "comment-moderation-results"        │
│        │                                    │                            │
│  10. Update Comment:                        │                            │
│      status=APPROVED/REJECTED               │                            │
│      moderationConfidence                   │                            │
│      modelVersion                           │                            │
│      moderatedAt=now()                      │                            │
│                                                                          │
└──────────────────────────────────────────────────────────────────────────┘
```

### 1.15 Checklist Refactor Model Service

- [ ] Xóa thư mục `legacy/`
- [ ] Xóa thư mục `inference/`
- [ ] Xóa thư mục `api/` (thay bằng `app/`)
- [ ] Sửa `app/config.py` (import, path, tên biến)
- [ ] Sửa `app/schemas.py` (CommentRequest.text, type annotations)
- [ ] Format lại `app/predictor.py` (PEP8, docstrings)
- [ ] Viết lại `app/kafka_consumer.py` (hoàn chỉnh try/finally)
- [ ] Tạo mới `app/kafka_producer.py`
- [ ] Tạo mới `app/services/__init__.py`
- [ ] Tạo mới `app/services/moderation_service.py`
- [ ] Tạo mới `app/main.py` (lifespan thay on_event)
- [ ] Cập nhật `requirements.txt` (thêm aiokafka, pydantic-settings)
- [ ] Cập nhật `.env`
- [ ] Test: `uvicorn app.main:app --reload` khởi động không lỗi
- [ ] Test: `POST /api/v1/predict` trả kết quả đúng
- [ ] Test: Kafka consumer nhận message và publish result

### 1.16 Kafka Topic Schema (Contract giữa Java ↔ Python)

**Request topic: `comment-moderation-requests`** (content-service → model-service)

```json
{
    "commentId": "65f8a1b2c3d4e5f6a7b8c9d0",
    "postId": "65f8a1b2c3d4e5f6a7b8c9d1",
    "userId": "user-uuid-123",
    "content": "Nội dung bình luận cần kiểm duyệt"
}
```

**Result topic: `comment-moderation-results`** (model-service → content-service)

```json
{
    "commentId": "65f8a1b2c3d4e5f6a7b8c9d0",
    "postId": "65f8a1b2c3d4e5f6a7b8c9d1",
    "status": "APPROVED",
    "confidence": 0.9512,
    "toxicScore": 0.0488,
    "toxicLevel": "low",
    "modelVersion": "phobert_toxic_FINAL",
    "moderatedAt": "2026-03-04T10:30:00"
}
```

---

*Tiếp theo: GĐ 2 - Async AI Moderation Pipeline (Content Service Java side)*
*Xem file: `blur-part-b-gd2-onwards.md`*
