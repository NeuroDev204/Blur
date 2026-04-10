"""
Kafka Consumer cho Model Service.

Improvements v2:
1. Chờ model ready trước khi consume (tránh crash khi model load ngầm)
2. Manual commit offset — tránh mất message khi xử lý lỗi
3. Reconnect với exponential backoff khi Kafka disconnect
"""

import json
import asyncio
import logging
from aiokafka import AIOKafkaConsumer
from aiokafka.errors import KafkaConnectionError
from app.config import settings

logger = logging.getLogger(__name__)


async def start_consumer(moderation_service, max_retries: int = 5):
    """
    Kafka consumer với:
    1. Chờ model ready trước khi consume
    2. Manual commit (tránh mất message)
    3. Graceful reconnect khi Kafka disconnect
    """

    # ── Chờ model load xong ──
    logger.info("Kafka consumer waiting for model to be ready...")
    while not moderation_service.predictor.is_ready:
        await asyncio.sleep(2)
    logger.info("Model ready! Starting Kafka consumer...")

    # ── Connect & consume với retry ──
    retry_count = 0
    while retry_count < max_retries:
        try:
            consumer = AIOKafkaConsumer(
                settings.KAFKA_REQUEST_TOPIC,
                bootstrap_servers=settings.KAFKA_BOOTSTRAP_SERVERS,
                group_id=settings.KAFKA_CONSUMER_GROUP,
                value_deserializer=lambda m: json.loads(m.decode('utf-8')),
                auto_offset_reset='earliest',
                enable_auto_commit=False,       # Manual commit
                max_poll_interval_ms=300000,    # 5 phút — đủ cho inference
            )

            await consumer.start()
            retry_count = 0  # reset retry khi connect thành công
            logger.info(
                "Kafka consumer started | topic=%s | group=%s",
                settings.KAFKA_REQUEST_TOPIC,
                settings.KAFKA_CONSUMER_GROUP,
            )

            try:
                async for msg in consumer:
                    try:
                        logger.debug(
                            "Received message | topic=%s | partition=%s | offset=%s",
                            msg.topic, msg.partition, msg.offset,
                        )
                        await moderation_service.moderate_comment(msg.value)
                        # Commit SAU KHI xử lý thành công
                        await consumer.commit()
                    except Exception as e:
                        logger.error(
                            "Error processing message | offset=%s | error=%s",
                            msg.offset, str(e), exc_info=True,
                        )
                        # Vẫn commit để skip message lỗi (tránh loop vô tận)
                        await consumer.commit()
            finally:
                await consumer.stop()
                logger.info("Kafka consumer stopped")

        except asyncio.CancelledError:
            logger.info("Kafka consumer cancelled")
            break

        except KafkaConnectionError as e:
            retry_count += 1
            wait = min(30, 2 ** retry_count)
            logger.warning(
                "Kafka connection lost, retrying in %ds (%d/%d): %s",
                wait, retry_count, max_retries, e,
            )
            await asyncio.sleep(wait)

        except Exception as e:
            retry_count += 1
            wait = min(30, 2 ** retry_count)
            logger.warning(
                "Kafka error, retrying in %ds (%d/%d): %s",
                wait, retry_count, max_retries, e,
            )
            await asyncio.sleep(wait)

    if retry_count >= max_retries:
        logger.error("Kafka consumer exhausted retries, stopping")
