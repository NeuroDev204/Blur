from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    # Kafka
    KAFKA_BOOTSTRAP_SERVERS: str = "localhost:9092"
    KAFKA_CONSUMER_GROUP: str = "model-service"
    KAFKA_REQUEST_TOPIC: str = "comment-moderation-request"
    KAFKA_RESULT_TOPIC: str = "comment-moderation-results"

    # PhoBERT Model
    MODEL_PATH: str = "models/phobert_toxic_FINAL_20251111_232134"
    MODEL_VERSION: str = "phobert_toxic_FINAL"
    MAX_TOKEN_LENGTH: int = 256

    # Toxic thresholds — ranh giới quyết định
    TOXIC_THRESHOLD: float = 0.5          # >= 0.5 → REJECTED
    BORDERLINE_THRESHOLD: float = 0.3     # 0.3-0.5: log để review
    HIGH_TOXIC_THRESHOLD: float = 0.7     # >= 0.7: FLAGGED (ẩn + báo admin)

    # Model backend: "pytorch" hoặc "onnx"
    MODEL_BACKEND: str = "pytorch"
    ONNX_MODEL_PATH: str = "models/phobert_toxic_onnx"

    # Server
    HOST: str = "0.0.0.0"
    PORT: int = 8000

    class Config:
        env_file = ".env"


settings = Settings()