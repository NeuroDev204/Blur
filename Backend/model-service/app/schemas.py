from pydantic import BaseModel, Field
from typing import List, Dict, Any
from enum import Enum


class ToxicLevel(str, Enum):
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"


class ModerationAction(str, Enum):
    """Hành động moderation dựa trên toxic_score"""
    APPROVED = "APPROVED"           # score < 0.5: an toàn
    REJECTED = "REJECTED"           # score >= 0.5: ẩn comment
    FLAGGED = "FLAGGED"             # score >= 0.7: ẩn + báo admin


class CommentRequest(BaseModel):
    text: str = Field(
        ...,
        min_length=1,
        max_length=5000,
        description="Nội dung comment cần phân tích"
    )


class BatchCommentRequest(BaseModel):
    texts: List[str] = Field(
        ...,
        min_length=1,
        max_length=100,
        description="Danh sách comments cần phân tích"
    )


class CommentAnalysis(BaseModel):
    text: str
    is_toxic: bool
    toxic_score: float = Field(
        ..., ge=0, le=1,
        description="Điểm toxic (0.0 = sạch, 1.0 = rất toxic)"
    )
    toxic_level: ToxicLevel = Field(
        ...,
        description="Mức độ: low (<0.4) / medium (0.4-0.7) / high (>=0.7)"
    )
    confidence: float = Field(
        ..., ge=0, le=1,
        description="Độ tin cậy của model"
    )
    moderation_action: ModerationAction = Field(
        ...,
        description="Hành động: APPROVED / REJECTED / FLAGGED"
    )
    probabilities: Dict[str, float] = Field(
        ...,
        description="Xác suất từng class: {clean, toxic}"
    )


class PredictionResponse(BaseModel):
    success: bool = True
    data: CommentAnalysis
    timestamp: str


class BatchPredictionResponse(BaseModel):
    success: bool = True
    data: Dict[str, Any]
    timestamp: str


class HealthResponse(BaseModel):
    status: str = Field(..., description="healthy / unhealthy / degraded / starting")
    model_loaded: bool = Field(..., description="Model đã load chưa")
    device: str = Field(..., description="cpu hoặc cuda")
    version: str
    uptime_seconds: float
    timestamp: str
