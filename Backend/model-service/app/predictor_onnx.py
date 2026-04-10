"""
app/predictor_onnx.py — ONNX Runtime predictor

Startup time comparison vs PyTorch predictor:
  import torch + transformers:  20-40s  →  import onnxruntime: ~2s
  load PyTorch model (516MB):   15-30s  →  load ONNX (~400MB):  3-8s
  TOTAL PyTorch:                60-90s  →  TOTAL ONNX:           5-15s  (5-10x faster)

Requires: onnxruntime (CPU) or onnxruntime-gpu (GPU)
Generate ONNX model first: python scripts/export_onnx.py
"""

import numpy as np
import logging
from typing import List

from transformers import AutoTokenizer
from app.schemas import CommentAnalysis, ToxicLevel, ModerationAction
from app.config import settings

logger = logging.getLogger(__name__)


class ToxicPredictorONNX:
    """
    ONNX Runtime predictor — same interface as ToxicPredictor.
    Drop-in replacement for production environments where startup speed matters.
    """

    def __init__(self):
        self.session = None
        self.tokenizer = None
        self._is_ready = False

    @property
    def is_ready(self) -> bool:
        return self._is_ready

    @property
    def device(self):
        if self.session is None:
            return None
        providers = self.session.get_providers()
        if 'CUDAExecutionProvider' in providers:
            return 'cuda (ONNX)'
        return 'cpu (ONNX)'

    def load_model(self, model_dir: str):
        """Load ONNX model — much faster than PyTorch from_pretrained."""
        import onnxruntime as ort

        logger.info("[1/3] Loading tokenizer...")
        self.tokenizer = AutoTokenizer.from_pretrained(model_dir)

        logger.info("[2/3] Loading ONNX model...")
        onnx_path = f"{model_dir}/model.onnx"

        sess_options = ort.SessionOptions()
        sess_options.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_ALL
        sess_options.intra_op_num_threads = 0  # 0 = auto (all CPU cores)

        providers = ['CUDAExecutionProvider', 'CPUExecutionProvider']
        self.session = ort.InferenceSession(
            onnx_path,
            sess_options=sess_options,
            providers=providers,
        )
        logger.info("  Provider: %s", self.session.get_providers())

        logger.info("[3/3] Warmup...")
        self._predict_internal("test warmup")

        self._is_ready = True
        logger.info("ONNX model ready!")

    def _softmax(self, x: np.ndarray) -> np.ndarray:
        e_x = np.exp(x - np.max(x, axis=1, keepdims=True))
        return e_x / e_x.sum(axis=1, keepdims=True)

    def _get_toxic_level(self, score: float) -> ToxicLevel:
        if score < 0.4:
            return ToxicLevel.LOW
        elif score < 0.7:
            return ToxicLevel.MEDIUM
        return ToxicLevel.HIGH

    def _get_moderation_action(self, score: float) -> ModerationAction:
        if score >= settings.HIGH_TOXIC_THRESHOLD:
            return ModerationAction.FLAGGED
        elif score >= settings.TOXIC_THRESHOLD:
            return ModerationAction.REJECTED
        return ModerationAction.APPROVED

    def _predict_internal(self, text: str) -> CommentAnalysis:
        inputs = self.tokenizer(
            text,
            padding=True,
            truncation=True,
            max_length=settings.MAX_TOKEN_LENGTH,
            return_tensors='np',  # NumPy tensors — no PyTorch needed
        )

        logits = self.session.run(
            ['logits'],
            {
                'input_ids': inputs['input_ids'],
                'attention_mask': inputs['attention_mask'],
            },
        )[0]

        probs = self._softmax(logits)
        toxic_prob = float(probs[0][1])
        clean_prob = float(probs[0][0])

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
        if not self._is_ready:
            raise RuntimeError("Model not loaded yet. Retry in a few seconds.")

        results = []
        for i in range(0, len(texts), chunk_size):
            chunk = texts[i:i + chunk_size]
            inputs = self.tokenizer(
                chunk,
                padding=True,
                truncation=True,
                max_length=settings.MAX_TOKEN_LENGTH,
                return_tensors='np',
            )

            logits = self.session.run(
                ['logits'],
                {
                    'input_ids': inputs['input_ids'],
                    'attention_mask': inputs['attention_mask'],
                },
            )[0]

            probs = self._softmax(logits)
            for j, text in enumerate(chunk):
                toxic_prob = float(probs[j][1])
                clean_prob = float(probs[j][0])
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
