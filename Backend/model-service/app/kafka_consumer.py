import json
import logging
from aiokafka import AIOKafkaConsumer
from app.config import settings

logger = logging.getLogger(__name__)

async def start_consumer(moderation_service):
  """
  consume tu topic 'comment-moderation-request'.
  moi message chua: {commentId, postId, userId, content}
  """

  consumer = AIOKafkaConsumer(
    settings.KAFKA_REQUEST_TOPIC,
    bootstrap_servers=settings.KAFKA_BOOTSTRAP_SERVER,
    group_id=settings.KAFKA_CONSUMER_GROUP,
    value_deserializer=lambda m: json.loads(m.decode('utf-8')),
    auto_offset_reset='earliest',
    enable_auto_commit=True
    
  )