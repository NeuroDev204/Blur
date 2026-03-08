from pydantic import BaseModel, Field
from typing import List,Dict,Any
from enum import Enum

class ToxicLevel(str,Enum):
  LOW = "low"
  MEDIUM ="medium"
  HIGHT = "high"
class CommentRequest(BaseModel):
  text: str = Field(..., min_length=1, max_length=5000, description="Nội dung comment cần phân tích")
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
  toxic_score: float = Field(..., ge=0,le=1,description="Điểm toxic (0.0 = sạch, 1.0 = rất toxic)")
  toxic_level: ToxicLevel = Field(..., description="Mức độ: low/medium/high")
  confidence: float = Field(...,ge=0,le=1, description="Độ tin cậy của model")
  probabilities: Dict[str,float]= Field(..., description="Xác xuất từng class: {clean, toxic}")

class PredictResponse(BaseModel):
  success: bool = True
  data: CommentAnalysis
  timestamp: str
class BatchPredictionResponse(BaseModel):
  success: bool = True
  data: Dict[str,Any]
  timestamp: str
class HealthResponse(BaseModel):
  status: str = Field(..., description="Healthy/ unhealthy, degraded")
  model_loaded: bool = Field(..., description="Model loaded flag")
  device: str = Field(..., description="Cpu hoặc cuda")
  version: str
  uptime_seconds: float
  timestamp: str
  
# Backwards compatibility: older code imports `PredictionResponse`
PredictionResponse = PredictResponse

