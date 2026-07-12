"""
Kafka Producer cho Model Service.
Gửi kết quả moderation tới topic 'comment-moderation-results'.
Content-service sẽ consume và update Comment entity.
"""

import json
import logging
import asyncio
from aiokafka import AIOKafkaProducer
from aiokafka.errors import KafkaConnectionError
from app.config import settings

logger = logging.getLogger(__name__)

class KafkaResultProducer:
    def __init__(self):
        self._producer = None

    async def start(self, max_retries: int = 5):
        self._producer = AIOKafkaProducer(
            bootstrap_servers=settings.KAFKA_BOOTSTRAP_SERVERS,
            value_serializer=lambda v: json.dumps(v).encode('utf-8'),
        )
        
        retry_count = 0
        while retry_count < max_retries:
            try:
                await self._producer.start()
                logger.info(
                    "Kafka producer started | servers=%s | topic=%s",
                    settings.KAFKA_BOOTSTRAP_SERVERS,
                    settings.KAFKA_RESULT_TOPIC
                )
                return
            except KafkaConnectionError as e:
                retry_count += 1
                if retry_count >= max_retries:
                    logger.error("Failed to start Kafka producer after %d retries", max_retries)
                    raise e
                wait = min(30, 2 ** retry_count)
                logger.warning("Kafka producer connection failed, retrying in %ds (%d/%d): %s", wait, retry_count, max_retries, e)
                await asyncio.sleep(wait)
            except Exception as e:
                retry_count += 1
                if retry_count >= max_retries:
                    logger.error("Failed to start Kafka producer due to unexpected error after %d retries", max_retries)
                    raise e
                wait = min(30, 2 ** retry_count)
                logger.warning("Kafka producer error, retrying in %ds (%d/%d): %s", wait, retry_count, max_retries, e)
                await asyncio.sleep(wait)

    async def send_result(self, result: dict):
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