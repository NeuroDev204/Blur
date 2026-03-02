"""
Vietnamese Toxic Comment Detection API
Optimized for Spring Boot Integration

Features:
    - RESTful API với JSON responses
    - CORS enabled cho cross-origin requests
    - Proper HTTP status codes
    - OpenAPI/Swagger documentation
    - Health check endpoint
    - Standard error response format
"""

import os
import sys
from pathlib import Path

# Add parent directory to path
sys.path.insert(0, str(Path(__file__).parent.parent))

from fastapi import FastAPI, HTTPException, Query, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
import torch
from transformers import AutoTokenizer, AutoModelForSequenceClassification
import uvicorn
from datetime import datetime
from enum import Enum


# ============================================================================
# ENUMS & CONSTANTS
# ============================================================================

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


# ============================================================================
# REQUEST/RESPONSE MODELS (DTOs)
# ============================================================================

class CommentRequest(BaseModel):
    """Request body for single comment prediction"""
    text: str = Field(..., min_length=1, max_length=5000, description="Comment text to analyze")

    class Config:
        json_schema_extra = {
            "example": {
                "text": "Video này hay quá!"
            }
        }


class BatchCommentRequest(BaseModel):
    """Request body for batch prediction"""
    texts: List[str] = Field(..., min_items=1, max_items=100, description="List of comments to analyze")

    class Config:
        json_schema_extra = {
            "example": {
                "texts": ["Video hay quá!", "Comment thứ 2"]
            }
        }


class CommentAnalysis(BaseModel):
    """Individual comment analysis result"""
    text: str = Field(..., description="Original comment text")
    is_toxic: bool = Field(..., description="Whether the comment is toxic")
    toxic_score: float = Field(..., ge=0, le=1, description="Toxicity score (0-1)")
    toxic_level: ToxicLevel = Field(..., description="Toxicity level: low/medium/high")
    confidence: float = Field(..., ge=0, le=1, description="Model confidence")
    probabilities: Dict[str, float] = Field(..., description="Class probabilities")

    class Config:
        json_schema_extra = {
            "example": {
                "text": "Video này hay quá!",
                "is_toxic": False,
                "toxic_score": 0.05,
                "toxic_level": "low",
                "confidence": 0.95,
                "probabilities": {"clean": 0.95, "toxic": 0.05}
            }
        }


class PredictionResponse(BaseModel):
    """Response for single comment prediction"""
    success: bool = Field(default=True)
    data: CommentAnalysis
    timestamp: str = Field(..., description="ISO format timestamp")


class BatchPredictionResponse(BaseModel):
    """Response for batch prediction"""
    success: bool = Field(default=True)
    data: Dict[str, Any] = Field(..., description="Batch prediction results")
    timestamp: str = Field(..., description="ISO format timestamp")

    class Config:
        json_schema_extra = {
            "example": {
                "success": True,
                "data": {
                    "predictions": [],
                    "summary": {
                        "total": 10,
                        "toxic_count": 2,
                        "clean_count": 8,
                        "toxic_percentage": 20.0
                    }
                },
                "timestamp": "2025-12-12T13:45:00"
            }
        }


class HealthResponse(BaseModel):
    """Health check response"""
    status: str = Field(..., description="Service status: healthy/unhealthy")
    model_loaded: bool
    device: str = Field(..., description="Compute device: cpu/cuda")
    version: str
    uptime_seconds: float
    timestamp: str


class ErrorResponse(BaseModel):
    """Standard error response format"""
    success: bool = Field(default=False)
    error: Dict[str, Any] = Field(..., description="Error details")
    timestamp: str


class LabelRequest(BaseModel):
    """Request for NLP labeling"""
    text: str = Field(..., min_length=1, max_length=5000)


class LabelResponse(BaseModel):
    """NLP label response"""
    success: bool = Field(default=True)
    data: Dict[str, Any]
    timestamp: str


# ============================================================================
# TOXIC PREDICTOR
# ============================================================================

class ToxicPredictor:
    """PhoBERT-based toxic comment predictor"""
    
    def __init__(self, model_path: str):
        self.device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
        self.tokenizer = AutoTokenizer.from_pretrained(model_path)
        self.model = AutoModelForSequenceClassification.from_pretrained(model_path)
        self.model.to(self.device)
        self.model.eval()
    
    def _get_toxic_level(self, score: float) -> ToxicLevel:
        """Convert score to level"""
        if score < 0.4:
            return ToxicLevel.LOW
        elif score < 0.7:
            return ToxicLevel.MEDIUM
        else:
            return ToxicLevel.HIGH
    
    def predict(self, text: str) -> CommentAnalysis:
        """Predict single comment"""
        inputs = self.tokenizer(
            text,
            padding='max_length',
            truncation=True,
            max_length=256,
            return_tensors='pt'
        )
        inputs = {k: v.to(self.device) for k, v in inputs.items()}
        
        with torch.no_grad():
            outputs = self.model(**inputs)
            probabilities = torch.softmax(outputs.logits, dim=1)
        
        toxic_prob = probabilities[0][1].item()
        clean_prob = probabilities[0][0].item()
        is_toxic = toxic_prob > 0.5
        confidence = max(toxic_prob, clean_prob)
        
        return CommentAnalysis(
            text=text,
            is_toxic=is_toxic,
            toxic_score=round(toxic_prob, 4),
            toxic_level=self._get_toxic_level(toxic_prob),
            confidence=round(confidence, 4),
            probabilities={
                "clean": round(clean_prob, 4),
                "toxic": round(toxic_prob, 4)
            }
        )
    
    def predict_batch(self, texts: List[str]) -> List[CommentAnalysis]:
        """Predict batch of comments"""
        inputs = self.tokenizer(
            texts,
            padding='max_length',
            truncation=True,
            max_length=256,
            return_tensors='pt'
        )
        inputs = {k: v.to(self.device) for k, v in inputs.items()}
        
        with torch.no_grad():
            outputs = self.model(**inputs)
            probabilities = torch.softmax(outputs.logits, dim=1)
        
        results = []
        for i, text in enumerate(texts):
            toxic_prob = probabilities[i][1].item()
            clean_prob = probabilities[i][0].item()
            is_toxic = toxic_prob > 0.5
            confidence = max(toxic_prob, clean_prob)
            
            results.append(CommentAnalysis(
                text=text,
                is_toxic=is_toxic,
                toxic_score=round(toxic_prob, 4),
                toxic_level=self._get_toxic_level(toxic_prob),
                confidence=round(confidence, 4),
                probabilities={
                    "clean": round(clean_prob, 4),
                    "toxic": round(toxic_prob, 4)
                }
            ))
        
        return results


# ============================================================================
# FASTAPI APPLICATION
# ============================================================================

# App metadata for OpenAPI
app = FastAPI(
    title="Vietnamese Toxic Comment Detection API",
    description="""
## API để phát hiện bình luận độc hại tiếng Việt

### Features:
- 🔍 Phát hiện toxic comments với PhoBERT
- 📊 Batch processing (tối đa 100 comments/request)
- 🏷️ NLP labeling (sentiment, topic)
- 🚀 Tối ưu cho tích hợp Spring Boot

### Integration với Java Spring Boot:
```java
// RestTemplate example
RestTemplate restTemplate = new RestTemplate();
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.APPLICATION_JSON);

Map<String, String> body = new HashMap<>();
body.put("text", "Comment cần phân tích");

HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
ResponseEntity<PredictionResponse> response = restTemplate.postForEntity(
    "http://localhost:8000/api/v1/predict",
    request,
    PredictionResponse.class
);
```
    """,
    version="2.0.0",
    contact={
        "name": "API Support",
        "email": "support@example.com"
    },
    license_info={
        "name": "MIT",
    },
    openapi_tags=[
        {"name": "prediction", "description": "Toxic comment prediction endpoints"},
        {"name": "labeling", "description": "NLP labeling endpoints"},
        {"name": "health", "description": "Health check endpoints"},
    ]
)

# CORS Middleware - Allow all origins for easy integration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Cho phép tất cả origins
    allow_credentials=True,
    allow_methods=["*"],  # Cho phép tất cả methods
    allow_headers=["*"],  # Cho phép tất cả headers
)

# Global state
predictor: Optional[ToxicPredictor] = None
nlp_labeler = None
start_time: datetime = None


# ============================================================================
# EXCEPTION HANDLERS
# ============================================================================

@app.exception_handler(HTTPException)
async def http_exception_handler(request: Request, exc: HTTPException):
    """Custom HTTP exception handler"""
    return JSONResponse(
        status_code=exc.status_code,
        content={
            "success": False,
            "error": {
                "code": exc.status_code,
                "message": exc.detail,
                "path": str(request.url.path)
            },
            "timestamp": datetime.now().isoformat()
        }
    )


@app.exception_handler(Exception)
async def general_exception_handler(request: Request, exc: Exception):
    """General exception handler"""
    return JSONResponse(
        status_code=500,
        content={
            "success": False,
            "error": {
                "code": 500,
                "message": "Internal server error",
                "detail": str(exc),
                "path": str(request.url.path)
            },
            "timestamp": datetime.now().isoformat()
        }
    )


# ============================================================================
# STARTUP EVENT
# ============================================================================

@app.on_event("startup")
async def startup_event():
    """Initialize models on startup"""
    global predictor, nlp_labeler, start_time
    
    start_time = datetime.now()
    
    # Load toxic predictor
    model_path = os.path.join(
        os.path.dirname(os.path.dirname(__file__)),
        "models", "phobert_toxic_FINAL_20251111_232134"
    )
    
    if os.path.exists(model_path):
        print(f"🚀 Loading model from: {model_path}")
        predictor = ToxicPredictor(model_path)
        print(f"✅ Model loaded on {predictor.device}")
    else:
        print(f"⚠️  Model not found at: {model_path}")
        print("   API will run without toxic prediction")
    
    # Load NLP labeler
    try:
        from scraper.labelers import NLPLabeler
        nlp_labeler = NLPLabeler(use_ml_sentiment=False)  # Rule-based for speed
        print("✅ NLP Labeler loaded")
    except Exception as e:
        print(f"⚠️  NLP Labeler not loaded: {e}")


# ============================================================================
# HEALTH ENDPOINTS
# ============================================================================

@app.get("/", tags=["health"])
async def root():
    """Root endpoint - API documentation links"""
    return {
        "success": True,
        "message": "Vietnamese Toxic Comment Detection API",
        "version": "2.0.0",
        "docs": {
            "swagger": "/docs",
            "redoc": "/redoc",
            "openapi": "/openapi.json"
        },
        "endpoints": {
            "predict": "POST /api/v1/predict",
            "predict_batch": "POST /api/v1/predict/batch",
            "label": "POST /api/v1/label",
            "health": "GET /api/v1/health"
        }
    }


@app.get("/api/v1/health", response_model=HealthResponse, tags=["health"])
async def health_check():
    """
    Health check endpoint
    
    Use this to verify the API is running and model is loaded.
    Spring Boot Actuator compatible.
    """
    uptime = (datetime.now() - start_time).total_seconds() if start_time else 0
    
    return HealthResponse(
        status="healthy" if predictor else "degraded",
        model_loaded=predictor is not None,
        device=str(predictor.device) if predictor else "none",
        version="2.0.0",
        uptime_seconds=round(uptime, 2),
        timestamp=datetime.now().isoformat()
    )


# ============================================================================
# PREDICTION ENDPOINTS
# ============================================================================

@app.post("/api/v1/predict", response_model=PredictionResponse, tags=["prediction"])
async def predict_single(request: CommentRequest):
    """
    Predict if a single comment is toxic
    
    **Request Body:**
    ```json
    {
        "text": "Comment cần phân tích"
    }
    ```
    
    **Response:**
    ```json
    {
        "success": true,
        "data": {
            "text": "...",
            "is_toxic": false,
            "toxic_score": 0.05,
            "toxic_level": "low",
            "confidence": 0.95,
            "probabilities": {"clean": 0.95, "toxic": 0.05}
        },
        "timestamp": "2025-12-12T13:45:00"
    }
    ```
    """
    if predictor is None:
        raise HTTPException(status_code=503, detail="Model not loaded")
    
    result = predictor.predict(request.text)
    
    return PredictionResponse(
        success=True,
        data=result,
        timestamp=datetime.now().isoformat()
    )


@app.post("/api/v1/predict/batch", response_model=BatchPredictionResponse, tags=["prediction"])
async def predict_batch(request: BatchCommentRequest):
    """
    Predict toxicity for multiple comments (max 100)
    
    **Request Body:**
    ```json
    {
        "texts": ["Comment 1", "Comment 2", ...]
    }
    ```
    
    **Response includes summary statistics**
    """
    if predictor is None:
        raise HTTPException(status_code=503, detail="Model not loaded")
    
    results = predictor.predict_batch(request.texts)
    
    toxic_count = sum(1 for r in results if r.is_toxic)
    total = len(results)
    
    return BatchPredictionResponse(
        success=True,
        data={
            "predictions": [r.dict() for r in results],
            "summary": {
                "total": total,
                "toxic_count": toxic_count,
                "clean_count": total - toxic_count,
                "toxic_percentage": round(toxic_count / total * 100, 2) if total > 0 else 0
            }
        },
        timestamp=datetime.now().isoformat()
    )


# ============================================================================
# NLP LABELING ENDPOINTS
# ============================================================================

@app.post("/api/v1/label", response_model=LabelResponse, tags=["labeling"])
async def label_comment(request: LabelRequest):
    """
    Get NLP labels for a comment (sentiment, topic, toxic)
    
    **Response:**
    ```json
    {
        "success": true,
        "data": {
            "sentiment": "positive",
            "toxic_score": 0.05,
            "topic": "khen_video"
        },
        "timestamp": "2025-12-12T13:45:00"
    }
    ```
    """
    if nlp_labeler is None:
        raise HTTPException(status_code=503, detail="NLP Labeler not loaded")
    
    try:
        labels = nlp_labeler.label_text(request.text)
        
        return LabelResponse(
            success=True,
            data=labels,
            timestamp=datetime.now().isoformat()
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Labeling failed: {str(e)}")


@app.post("/api/v1/label/batch", response_model=LabelResponse, tags=["labeling"])
async def label_comments_batch(request: BatchCommentRequest):
    """
    Get NLP labels for multiple comments
    """
    if nlp_labeler is None:
        raise HTTPException(status_code=503, detail="NLP Labeler not loaded")
    
    try:
        labels = [nlp_labeler.label_text(text) for text in request.texts]
        
        return LabelResponse(
            success=True,
            data={
                "labels": labels,
                "total": len(labels)
            },
            timestamp=datetime.now().isoformat()
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Labeling failed: {str(e)}")


# ============================================================================
# BACKWARD COMPATIBILITY (old endpoints)
# ============================================================================

@app.post("/predict", include_in_schema=False)
async def predict_legacy(request: CommentRequest):
    """Legacy endpoint - redirect to new API"""
    return await predict_single(request)


@app.post("/predict/batch", include_in_schema=False)
async def predict_batch_legacy(request: BatchCommentRequest):
    """Legacy endpoint - redirect to new API"""
    return await predict_batch(request)


@app.get("/health", include_in_schema=False)
async def health_legacy():
    """Legacy endpoint - redirect to new API"""
    return await health_check()


# ============================================================================
# MAIN
# ============================================================================

if __name__ == "__main__":
    print("=" * 70)
    print("VIETNAMESE TOXIC COMMENT DETECTION API SERVER")
    print("Optimized for Spring Boot Integration")
    print("=" * 70)
    print()
    print("🚀 Starting server...")
    print()
    print("📖 Documentation:")
    print("   Swagger UI: http://localhost:8000/docs")
    print("   ReDoc:      http://localhost:8000/redoc")
    print("   OpenAPI:    http://localhost:8000/openapi.json")
    print()
    print("🔌 Endpoints:")
    print("   POST /api/v1/predict       - Single prediction")
    print("   POST /api/v1/predict/batch - Batch prediction")
    print("   POST /api/v1/label         - NLP labeling")
    print("   GET  /api/v1/health        - Health check")
    print()
    print("=" * 70)
    
    uvicorn.run(
        "server:app",
        host="0.0.0.0",
        port=8000,
        reload=True,
        log_level="info"
    )
