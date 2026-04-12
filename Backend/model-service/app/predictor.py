"""
app/predictor.py — Optimized ToxicPredictor with lazy loading

Improvements over v1:
1. Lazy loading — model loads in background, FastAPI starts instantly
2. Dynamic padding — only pad to actual text length (not always 256)
3. torch.inference_mode — faster than torch.no_grad
4. Chunked batch prediction — avoids OOM on large batches
5. Warmup inference — pre-allocate GPU memory, JIT compile CUDA kernels
6. moderation_action — maps score to APPROVED / REJECTED / FLAGGED
"""

import logging
from typing import List, Optional

from app.schemas import CommentAnalysis, ToxicLevel, ModerationAction
from app.config import settings

logger = logging.getLogger(__name__)


import re

# ─── Context-aware false positive filter ───
# Từ có nghĩa kép (vừa danh từ bình thường, vừa từ chửi):
#   chó, bò, gà, lợn/heo, cặc (cặc kè), đĩ (đĩa)
# Nếu đi kèm context tích cực/trung tính → giảm toxic score

_AMBIGUOUS_WORDS = re.compile(
    r'\b(con\s+)?(chó|bò|gà|lợn|heo|mèo|ngựa|khỉ|chuột)\b', re.IGNORECASE
)
_POSITIVE_CONTEXT = re.compile(
    r'(dễ\s*thương|xinh|đẹp|cute|cưng|yêu|thích|nuôi|nhỏ|bé|ăn|thịt|sữa|trứng|'
    r'giống|phố|cảnh|nhà|hoang|con\s+sen|dắt|chăm|tắm|ốm|tiêm|pet|hamster|'
    r'vàng|đốm|mập|ú|adorable|lovely)', re.IGNORECASE
)
_STRONG_TOXIC = re.compile(
    r'(đồ\s*(chó|bò|gà|lợn|heo|khỉ)|thằng|con\s*(đĩ|điếm)|'
    r'mẹ\s*m|đ[ií]t|l[oồ]n|c[aặ][ck]|đ[éè]o|v[ãả]i?\s*l[oồ]n|'
    r'ngu|óc\s*chó|chó\s*chết|súc\s*sinh|khốn)', re.IGNORECASE
)


def _adjust_context_score(text: str, raw_score: float) -> float:
    """Điều chỉnh toxic score dựa trên context.
    Chỉ giảm score khi có từ ambiguous + context tích cực + KHÔNG có strong toxic.
    """
    if raw_score < 0.3:
        return raw_score

    has_ambiguous = _AMBIGUOUS_WORDS.search(text) is not None
    has_positive = _POSITIVE_CONTEXT.search(text) is not None
    has_strong_toxic = _STRONG_TOXIC.search(text) is not None

    if has_ambiguous and has_positive and not has_strong_toxic:
        # "con chó dễ thương" → giảm mạnh
        return raw_score * 0.2

    return raw_score


class ToxicPredictor:
    """
    PhoBERT-based Vietnamese Toxic Comment Predictor.

    Model không load trong __init__ — gọi load_model() từ background thread.
    Cho phép FastAPI start ngay lập tức, model load ngầm trong background.
    """

    def __init__(self):
        self.model = None
        self.tokenizer = None
        self.device = None
        self._is_ready = False

    @property
    def is_ready(self) -> bool:
        return self._is_ready

    def load_model(self, model_path: str):
        """
        Load model — gọi từ background thread.
        Tách khỏi __init__ để không block FastAPI startup.
        """
        import torch
        from transformers import AutoTokenizer, AutoModelForSequenceClassification

        logger.info("[1/4] Detecting device...")
        self.device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
        logger.info("  Device: %s", self.device)

        logger.info("[2/4] Loading tokenizer from %s...", model_path)
        self.tokenizer = AutoTokenizer.from_pretrained(model_path)

        logger.info("[3/4] Loading model (may take 15-30s)...")
        self.model = AutoModelForSequenceClassification.from_pretrained(model_path)
        self.model.to(self.device)
        self.model.eval()

        logger.info("[4/4] Warming up model...")
        # Warmup: JIT compile CUDA kernels + pre-allocate GPU memory.
        # Without warmup, first real request would be 200-500ms slower.
        for text in ["test", "xin chào", "nội dung cần kiểm tra trung bình độ dài"]:
            self._predict_internal(text)

        self._is_ready = True
        logger.info("Model ready! Device=%s", self.device)

    def _get_toxic_level(self, score: float) -> ToxicLevel:
        if score < 0.4:
            return ToxicLevel.LOW
        elif score < 0.7:
            return ToxicLevel.MEDIUM
        return ToxicLevel.HIGH

    def _get_moderation_action(self, score: float) -> ModerationAction:
        """Map toxic_score to moderation action based on configured thresholds."""
        if score >= settings.HIGH_TOXIC_THRESHOLD:
            return ModerationAction.FLAGGED
        elif score >= settings.TOXIC_THRESHOLD:
            return ModerationAction.REJECTED
        return ModerationAction.APPROVED

    def _predict_internal(self, text: str) -> CommentAnalysis:
        """Internal predict — assumes model is loaded, no is_ready check."""
        import torch

        # Dynamic padding: pad to actual text length, not max_length (5x speedup on short texts)
        inputs = self.tokenizer(
            text,
            padding=True,
            truncation=True,
            max_length=settings.MAX_TOKEN_LENGTH,
            return_tensors='pt',
        )
        inputs = {k: v.to(self.device) for k, v in inputs.items()}

        with torch.inference_mode():  # faster than torch.no_grad
            outputs = self.model(**inputs)
            probs = torch.softmax(outputs.logits, dim=1)

        raw_toxic = probs[0][1].item()
        toxic_prob = _adjust_context_score(text, raw_toxic)
        clean_prob = 1.0 - toxic_prob

        return CommentAnalysis(
            text=text,
            is_toxic=toxic_prob >= settings.TOXIC_THRESHOLD,
            toxic_score=round(toxic_prob, 4),
            toxic_level=self._get_toxic_level(toxic_prob),
            confidence=round(max(toxic_prob, clean_prob), 4),
            moderation_action=self._get_moderation_action(toxic_prob),
            probabilities={"clean": round(clean_prob, 4), "toxic": round(toxic_prob, 4)},
        )

    def predict(self, text: str) -> CommentAnalysis:
        if not self._is_ready:
            raise RuntimeError("Model not loaded yet. Retry in a few seconds.")
        return self._predict_internal(text)

    def predict_batch(self, texts: List[str], chunk_size: int = 32) -> List[CommentAnalysis]:
        """
        Batch prediction with chunking.
        chunk_size=32: process 32 texts at a time to avoid GPU OOM.
        Dynamic padding is applied per-chunk (pad to longest in that chunk).
        """
        import torch

        if not self._is_ready:
            raise RuntimeError("Model not loaded yet. Retry in a few seconds.")

        results = []
        for i in range(0, len(texts), chunk_size):
            chunk = texts[i:i + chunk_size]

            inputs = self.tokenizer(
                chunk,
                padding=True,              # pad to longest in this chunk
                truncation=True,
                max_length=settings.MAX_TOKEN_LENGTH,
                return_tensors='pt',
            )
            inputs = {k: v.to(self.device) for k, v in inputs.items()}

            with torch.inference_mode():
                outputs = self.model(**inputs)
                probs = torch.softmax(outputs.logits, dim=1)

            for j, text in enumerate(chunk):
                raw_toxic = probs[j][1].item()
                toxic_prob = _adjust_context_score(text, raw_toxic)
                clean_prob = 1.0 - toxic_prob
                results.append(CommentAnalysis(
                    text=text,
                    is_toxic=toxic_prob >= settings.TOXIC_THRESHOLD,
                    toxic_score=round(toxic_prob, 4),
                    toxic_level=self._get_toxic_level(toxic_prob),
                    confidence=round(max(toxic_prob, clean_prob), 4),
                    moderation_action=self._get_moderation_action(toxic_prob),
                    probabilities={
                        "clean": round(clean_prob, 4),
                        "toxic": round(toxic_prob, 4),
                    },
                ))

        return results
