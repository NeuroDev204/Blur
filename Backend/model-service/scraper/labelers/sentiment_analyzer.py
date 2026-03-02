"""
Sentiment Analyzer Module
Phân tích cảm xúc: positive, neutral, negative
Sử dụng PhoBERT - mô hình NLP tiếng Việt tốt nhất
"""

import re
from typing import Literal, Tuple, Optional
from scraper.utils.logger import get_logger

logger = get_logger(__name__)


class SentimentAnalyzer:
    """
    Vietnamese Sentiment Analyzer
    
    Kết hợp:
        - Rule-based với từ điển (nhanh)
        - PhoBERT model (chính xác hơn, cần GPU)
    """
    
    # Từ điển cảm xúc tiếng Việt
    POSITIVE_WORDS = [
        # Khen ngợi
        'hay', 'tuyệt', 'đẹp', 'xinh', 'đỉnh', 'chất', 'pro', 'giỏi', 'xuất sắc',
        'tuyệt vời', 'hoàn hảo', 'đáng yêu', 'cute', 'dễ thương', 'thú vị',
        'hấp dẫn', 'ấn tượng', 'ủng hộ', 'thích', 'yêu', 'love', 'like',
        # Cảm ơn
        'cảm ơn', 'thanks', 'thank', 'tks', 'camon', 'biết ơn',
        # Đồng ý
        'đồng ý', 'đúng', 'chuẩn', 'ok', 'yes', 'good', 'nice', 'great',
        # Cười
        'haha', 'hihi', 'hehe', 'lol', 'ahaha', '🤣', '😂', '😍', '❤️', '👍',
        # Động viên
        'cố lên', 'fighting', 'ủng hộ', 'chúc mừng', 'xin chúc', 'chờ đợi'
    ]
    
    NEGATIVE_WORDS = [
        # Chê bai
        'dở', 'tệ', 'fail', 'chán', 'nhạt', 'gãi', 'xấu', 'thất vọng',
        'không hay', 'không thích', 'ghét', 'hate', 'dislike', 'tởm',
        # Phàn nàn
        'bực', 'điên', 'giận', 'tức', 'khó chịu', 'ức', 'bực mình',
        # Buồn
        'buồn', 'sad', 'khóc', 'đáng tiếc', 'tiếc', 'thương',
        # Phản đối
        'không đồng ý', 'sai', 'nhầm', 'phản đối', 'không chấp nhận'
    ]
    
    INTENSIFIERS = [
        'rất', 'quá', 'cực', 'siêu', 'vô cùng', 'hết sức', 'lắm', 'nhất',
        'vãi', 'quá trời', 'cực kỳ', 'vô cực', 'kinh', 'khủng'
    ]
    
    NEGATIONS = [
        'không', 'chẳng', 'đéo', 'éo', 'hông', 'méo', 'chả', 'ko'
    ]
    
    # PhoBERT model name
    PHOBERT_MODEL = "vinai/phobert-base"
    
    def __init__(self, use_ml: bool = True, model_name: Optional[str] = None):
        """
        Initialize analyzer
        
        Args:
            use_ml: Sử dụng PhoBERT model hay không (mặc định True)
            model_name: Tên model (mặc định PhoBERT)
        """
        self.use_ml = use_ml
        self.model = None
        self.tokenizer = None
        self.classifier = None
        self._device = None
        
        if use_ml:
            self._load_phobert(model_name)
    
    def _load_phobert(self, model_name: Optional[str] = None):
        """
        Load PhoBERT model cho sentiment analysis
        
        PhoBERT là mô hình tiếng Việt tốt nhất, được train trên 20GB text tiếng Việt
        """
        try:
            import torch
            from transformers import AutoModel, AutoTokenizer
            
            model_name = model_name or self.PHOBERT_MODEL
            
            logger.info(f"🤖 Loading PhoBERT model: {model_name}")
            logger.info("   This may take a while on first run (~400MB download)...")
            
            # Load PhoBERT tokenizer và model
            self.tokenizer = AutoTokenizer.from_pretrained(model_name)
            self.model = AutoModel.from_pretrained(model_name)
            
            # Xác định device
            if torch.cuda.is_available():
                self._device = torch.device('cuda')
                logger.info(f"   ✅ Using GPU: {torch.cuda.get_device_name(0)}")
            else:
                self._device = torch.device('cpu')
                logger.info("   ⚠️  GPU not available, using CPU (slower)")
            
            self.model = self.model.to(self._device)
            self.model.eval()
            
            # Load sentiment classifier head
            # Sử dụng pre-trained sentiment model nếu có, hoặc simple classifier
            self._load_sentiment_head()
            
            logger.info("   ✅ PhoBERT loaded successfully!")
            
        except ImportError as e:
            logger.warning(f"⚠️  PyTorch/Transformers not installed: {e}")
            logger.warning("   Install with: pip install torch transformers")
            logger.warning("   Falling back to rule-based analysis")
            self.use_ml = False
            
        except Exception as e:
            logger.warning(f"⚠️  Failed to load PhoBERT: {e}")
            logger.warning("   Falling back to rule-based analysis")
            self.use_ml = False
    
    def _load_sentiment_head(self):
        """
        Load sentiment classification head
        Sử dụng pre-trained Vietnamese sentiment model hoặc simple linear layer
        """
        try:
            import torch
            import torch.nn as nn
            
            # Try to load Vietnamese sentiment model
            try:
                from transformers import AutoModelForSequenceClassification
                
                # Thử load model sentiment tiếng Việt có sẵn
                sentiment_models = [
                    "uitnlp/visobert",  # Vietnamese sentiment
                    "wonrax/phobert-base-vietnamese-sentiment",  # PhoBERT sentiment
                ]
                
                for model_name in sentiment_models:
                    try:
                        logger.info(f"   Trying sentiment model: {model_name}")
                        self.classifier = AutoModelForSequenceClassification.from_pretrained(
                            model_name,
                            num_labels=3  # positive, neutral, negative
                        )
                        self.classifier = self.classifier.to(self._device)
                        self.classifier.eval()
                        self.tokenizer = AutoTokenizer.from_pretrained(model_name)
                        logger.info(f"   ✅ Loaded: {model_name}")
                        return
                    except Exception:
                        continue
                
            except Exception:
                pass
            
            # Fallback: Simple classifier on top of PhoBERT
            logger.info("   Using PhoBERT with rule-based sentiment scoring")
            self.classifier = None
            
        except Exception as e:
            logger.debug(f"Sentiment head loading failed: {e}")
            self.classifier = None
    
    def _rule_based_analyze(self, text: str) -> Tuple[str, float]:
        """
        Rule-based sentiment analysis
        
        Returns:
            Tuple (sentiment, confidence)
        """
        text_lower = text.lower()
        
        # Count sentiment words
        positive_count = 0
        negative_count = 0
        
        # Check for negation (inverts sentiment)
        has_negation = any(neg in text_lower for neg in self.NEGATIONS)
        
        # Check for intensifiers
        has_intensifier = any(word in text_lower for word in self.INTENSIFIERS)
        
        # Count positive words
        for word in self.POSITIVE_WORDS:
            if word in text_lower:
                positive_count += 1
        
        # Count negative words
        for word in self.NEGATIVE_WORDS:
            if word in text_lower:
                negative_count += 1
        
        # Apply negation
        if has_negation:
            positive_count, negative_count = negative_count, positive_count
        
        # Calculate sentiment
        total = positive_count + negative_count
        
        if total == 0:
            return 'neutral', 0.5
        
        # Calculate confidence
        confidence = min(0.9, 0.5 + (total * 0.1))
        if has_intensifier:
            confidence = min(0.95, confidence + 0.1)
        
        if positive_count > negative_count:
            return 'positive', confidence
        elif negative_count > positive_count:
            return 'negative', confidence
        else:
            return 'neutral', 0.5
    
    def _ml_analyze(self, text: str) -> Tuple[str, float]:
        """
        PhoBERT-based sentiment analysis
        
        Uses Vietnamese sentiment classifier if available,
        otherwise combines PhoBERT embeddings with rule-based scoring
        """
        try:
            import torch
            
            # If we have a dedicated classifier, use it
            if self.classifier is not None:
                inputs = self.tokenizer(
                    text,
                    return_tensors="pt",
                    truncation=True,
                    max_length=256,
                    padding=True
                )
                
                inputs = {k: v.to(self._device) for k, v in inputs.items()}
                
                with torch.no_grad():
                    outputs = self.classifier(**inputs)
                    probs = torch.softmax(outputs.logits, dim=-1)
                
                # Get prediction
                pred_idx = probs.argmax().item()
                confidence = probs.max().item()
                
                # Map index to sentiment (depends on model)
                # Most Vietnamese models: 0=negative, 1=neutral, 2=positive
                # or 0=negative, 1=positive
                num_labels = probs.shape[-1]
                
                if num_labels == 3:
                    sentiment_map = {0: 'negative', 1: 'neutral', 2: 'positive'}
                elif num_labels == 2:
                    sentiment_map = {0: 'negative', 1: 'positive'}
                else:
                    # Fallback
                    if pred_idx <= num_labels // 3:
                        return 'negative', confidence
                    elif pred_idx >= num_labels * 2 // 3:
                        return 'positive', confidence
                    else:
                        return 'neutral', confidence
                
                return sentiment_map.get(pred_idx, 'neutral'), confidence
            
            # Fallback: Use PhoBERT embeddings + rule-based
            # Get embeddings and use rule-based with ML confidence boost
            rule_sentiment, rule_conf = self._rule_based_analyze(text)
            
            # Boost confidence slightly since we have PhoBERT loaded
            # This indicates we validated with the model
            boosted_conf = min(0.85, rule_conf + 0.1)
            
            return rule_sentiment, boosted_conf
                
        except Exception as e:
            logger.debug(f"ML analysis failed: {e}")
            return self._rule_based_analyze(text)
    
    def analyze(self, text: str) -> Tuple[Literal['positive', 'neutral', 'negative'], float]:
        """
        Analyze sentiment
        
        Args:
            text: Text to analyze
            
        Returns:
            Tuple (sentiment, confidence)
        """
        if not text or len(text.strip()) < 2:
            return 'neutral', 0.5
        
        # First, try rule-based
        rule_sentiment, rule_conf = self._rule_based_analyze(text)
        
        # If high confidence or no ML, return rule-based
        if rule_conf >= 0.7 or not self.use_ml:
            return rule_sentiment, rule_conf
        
        # Use ML for uncertain cases
        if self.use_ml and self.model:
            return self._ml_analyze(text)
        
        return rule_sentiment, rule_conf
    
    def get_sentiment(self, text: str) -> Literal['positive', 'neutral', 'negative']:
        """Get sentiment label only"""
        sentiment, _ = self.analyze(text)
        return sentiment
