"""
API Models (DTOs)
Standalone models file for clarity
"""

from pydantic import BaseModel, Field
from typing import List, Dict, Any, Optional
from enum import Enum


class ToxicLevel(str, Enum):
    """Toxic level classification"""
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"


class SentimentType(str, Enum):
    """Sentiment types"""
    POSITIVE = "positive"
    NEUTRAL = "neutral"
    NEGATIVE = "negative"


class TopicType(str, Enum):
    """Topic types"""
    KHEN_VIDEO = "khen_video"
    CHE_VIDEO = "che_video"
    HOI_THONG_TIN = "hoi_thong_tin"
    TRANH_LUAN = "tranh_luan"
    SPAM = "spam"
    HAI_HUOC = "hai_huoc"
    KHAC = "khac"


# ============================================================================
# REQUEST MODELS
# ============================================================================

class CommentRequest(BaseModel):
    """Single comment request"""
    text: str = Field(..., min_length=1, max_length=5000)


class BatchCommentRequest(BaseModel):
    """Batch comments request"""
    texts: List[str] = Field(..., min_items=1, max_items=100)


# ============================================================================
# RESPONSE MODELS
# ============================================================================

class CommentAnalysis(BaseModel):
    """Analysis result for a single comment"""
    text: str
    is_toxic: bool
    toxic_score: float = Field(..., ge=0, le=1)
    toxic_level: ToxicLevel
    confidence: float = Field(..., ge=0, le=1)
    probabilities: Dict[str, float]


class PredictionSummary(BaseModel):
    """Summary statistics for batch prediction"""
    total: int
    toxic_count: int
    clean_count: int
    toxic_percentage: float


class NLPLabels(BaseModel):
    """NLP label results"""
    sentiment: SentimentType
    toxic_score: float
    toxic_level: ToxicLevel
    topic: TopicType


# ============================================================================
# API RESPONSES (Standard format)
# ============================================================================

class ApiResponse(BaseModel):
    """Base API response"""
    success: bool = True
    timestamp: str


class PredictionResponse(ApiResponse):
    """Single prediction response"""
    data: CommentAnalysis


class BatchPredictionResponse(ApiResponse):
    """Batch prediction response"""
    data: Dict[str, Any]  # Contains predictions and summary


class LabelResponse(ApiResponse):
    """Label response"""
    data: Dict[str, Any]


class HealthResponse(ApiResponse):
    """Health check response"""
    status: str
    model_loaded: bool
    device: str
    version: str
    uptime_seconds: float


class ErrorResponse(ApiResponse):
    """Error response"""
    success: bool = False
    error: Dict[str, Any]
