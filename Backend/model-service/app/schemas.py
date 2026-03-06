from pydantic import BaseModel, Field
from typing import List,Dict,Any
from enum import Enum

class ToxicLevel(str, Enum):
  LOW = "low"
  MEDIUM = "medium"
  HIGH = "high"

class CommentRequest(BaseModel):
  """request body cho single comment prediction"""
  texts: List[str] = Field(..., min_items=1, max_items=100)

class BatchCommentRequest (BaseModel):
  """request body cho bath prediction"""
  texts = List[str] = Field(...,min_items=1, max_items=100)

class CommentAnalysis(BaseModel):
  """ket qua phan tich 1 comment"""
  text: str
  is_toxic: bool
  toxic_score: float = Field(..., ge=0,le=1)
  toxic_level = ToxicLevel
  confidence: float = Field(...,ge=0,le=1)
  probabilities: Dict[str, float]

class PredictionResponse(BaseModel):
  """response cho sigle prediction"""
  success: bool = True
  data: CommentAnalysis
  timestamp: str

class BatchPredictionResponse(BaseModel):
  """response cho batch prediction"""
  success: bool = True
  data: Dict[str, Any]
  timestamp: str

class HealthResponse(BaseModel):
  """health check response"""
  status: str
  model_loaded: bool
  device: str
  version: str
  uptime_seconds: float
  timestamp: str

