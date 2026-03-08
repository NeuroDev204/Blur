from pydantic_settings import BaseSettings

class Settings(BaseSettings):
  #kafka
  KAFKA_BOOTSTRAP_SERVERS: str = "localhost:9092"
  KAFKA_CONSUMER_GROUP: str = "model-service"
  KAFKA_REQUEST_TOPIC: str = "comment-moderation-request"
  KAFKA_RESULT_TOPIC: str = "comment-moderation-results"

  #phobert model
  MODEL_PATH: str = "models/phobert_toxic_FINAL_20251111_232134"
  MODEL_VERSION: str = "phobert_toxic_FINAL"
  MAX_TOKEN_LENGTH: int = 256
  TOXIC_THRESHOLD: float = 0.5

  #Server
  HOST: str = "0.0.0.0"
  PORT: int = 8000

  class Config:
    env_file = ".env"
settings = Settings()
