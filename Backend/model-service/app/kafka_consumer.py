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