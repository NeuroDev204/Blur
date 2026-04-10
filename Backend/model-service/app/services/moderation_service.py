"""
Moderation Service — Orchestrate PhoBERT inference + Kafka publish.

Flow:
1. Nhận message từ Kafka consumer: {commentId, postId, userId, content}
2. Chạy PhoBERT predict trên content
3. Map kết quả sang APPROVED / REJECTED / FLAGGED dựa trên toxic_score:
   - APPROVED  : score < 0.5  — comment an toàn
   - REJECTED  : score >= 0.5 — ẩn comment
   - FLAGGED   : score >= 0.7 — ẩn + báo admin
4. Publish kết quả lên Kafka topic comment-moderation-results
"""

import asyncio
import logging
from datetime import datetime
from app.kafka_producer import KafkaResultProducer
from app.config import settings

logger = logging.getLogger(__name__)


class ModerationService:
    def __init__(self, predictor, producer: KafkaResultProducer):
        self.predictor = predictor
        self.producer = producer

        # Metrics
        self._total_processed = 0
        self._total_rejected = 0
        self._total_flagged = 0

    @property
    def stats(self) -> dict:
        total = max(1, self._total_processed)
        return {
            "total_processed": self._total_processed,
            "total_rejected": self._total_rejected,
            "total_flagged": self._total_flagged,
            "total_approved": self._total_processed - self._total_rejected - self._total_flagged,
            "rejection_rate": round(self._total_rejected / total * 100, 2),
            "flag_rate": round(self._total_flagged / total * 100, 2),
        }

    async def moderate_comment(self, message: dict):
        comment_id = message.get("commentId", "unknown")
        content = message.get("content", "")

        if not content.strip():
            logger.warning("Empty content for comment %s, skipping", comment_id)
            return

        # PhoBERT inference qua thread pool (tránh block event loop)
        loop = asyncio.get_event_loop()
        analysis = await loop.run_in_executor(
            None, self.predictor.predict, content
        )

        # moderation_action đã được tính trong predictor dựa trên thresholds
        status = analysis.moderation_action.value  # APPROVED / REJECTED / FLAGGED

        result = {
            "commentId": comment_id,
            "postId": message.get("postId"),
            "userId": message.get("userId"),
            "status": status,
            "confidence": analysis.confidence,
            "toxicScore": analysis.toxic_score,
            "toxicLevel": analysis.toxic_level.value,
            "modelVersion": settings.MODEL_VERSION,
            "moderatedAt": datetime.now().isoformat(),
        }

        await self.producer.send_result(result)

        # Update metrics
        self._total_processed += 1
        if status == "FLAGGED":
            self._total_flagged += 1
        elif status == "REJECTED":
            self._total_rejected += 1

        logger.info(
            "comment=%s status=%s confidence=%.4f toxic_score=%.4f",
            comment_id, status, analysis.confidence, analysis.toxic_score,
        )
