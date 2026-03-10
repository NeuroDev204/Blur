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
    def __init__(self, predictor: ToxicPredictor, producer: KafkaResultProducer):
        self.predictor = predictor
        self.producer = producer

    async def moderate_comment(self, message: dict):
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