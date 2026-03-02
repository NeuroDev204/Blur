"""
NLP Labeler Module
Main labeling function combining all analyzers
Sử dụng PhoBERT - mô hình NLP tiếng Việt tốt nhất
"""

from typing import Dict, List, Optional, Any
from scraper.models.comment import Comment, CommentLabel
from scraper.labelers.sentiment_analyzer import SentimentAnalyzer
from scraper.labelers.topic_classifier import TopicClassifier
from scraper.labelers.toxic_detector import ToxicDetector
from scraper.utils.logger import get_logger

logger = get_logger(__name__)


class NLPLabeler:
    """
    NLP Labeler - Combines all analyzers
    
    Provides:
        - Sentiment analysis (PhoBERT)
        - Toxicity detection
        - Topic classification
    """
    
    def __init__(
        self,
        use_ml_sentiment: bool = True,  # Mặc định dùng PhoBERT
        toxic_threshold: float = 0.45
    ):
        """
        Initialize labeler
        
        Args:
            use_ml_sentiment: Use PhoBERT for sentiment (default True)
            toxic_threshold: Threshold for toxic classification
        """
        logger.info("🏷️  Initializing NLP Labeler...")
        
        self.sentiment_analyzer = SentimentAnalyzer(use_ml=use_ml_sentiment)
        self.topic_classifier = TopicClassifier()
        self.toxic_detector = ToxicDetector(threshold=toxic_threshold)
        
        logger.info(
            f"NLPLabeler initialized (ml_sentiment={use_ml_sentiment}, "
            f"toxic_threshold={toxic_threshold})"
        )
    
    def label_text(self, text: str) -> Dict[str, Any]:
        """
        Label a single text
        
        Args:
            text: Text to label
            
        Returns:
            Dict with sentiment, toxic_score, topic
        """
        if not text or len(text.strip()) < 2:
            return {
                'sentiment': 'neutral',
                'toxic_score': 0.0,
                'toxic_level': 'low',
                'topic': 'khac'
            }
        
        # Sentiment
        sentiment, _ = self.sentiment_analyzer.analyze(text)
        
        # Toxicity
        toxic_result = self.toxic_detector.analyze(text)
        
        # Topic
        topic, _ = self.topic_classifier.classify(text)
        
        return {
            'sentiment': sentiment,
            'toxic_score': toxic_result['toxic_score'],
            'toxic_level': toxic_result['toxic_level'],
            'topic': topic
        }
    
    def label_comment(self, comment: Comment) -> Comment:
        """
        Label a Comment object
        
        Args:
            comment: Comment to label
            
        Returns:
            Comment with label attached
        """
        labels = self.label_text(comment.content)
        
        comment.label = CommentLabel(
            sentiment=labels['sentiment'],
            toxic_score=labels['toxic_score'],
            toxic_level=labels['toxic_level'],
            topic=labels['topic'],
            toxic_categories=self.toxic_detector.detect_categories(comment.content)
        )
        
        return comment
    
    def label_comments_batch(
        self,
        comments: List[Comment],
        show_progress: bool = True
    ) -> List[Comment]:
        """
        Label batch of comments
        
        Args:
            comments: List of comments
            show_progress: Show progress logging
            
        Returns:
            List of labeled comments
        """
        labeled = []
        
        total = len(comments)
        toxic_count = 0
        
        for i, comment in enumerate(comments):
            labeled_comment = self.label_comment(comment)
            labeled.append(labeled_comment)
            
            if labeled_comment.label and labeled_comment.label.toxic_level != 'low':
                toxic_count += 1
            
            # Progress logging
            if show_progress and (i + 1) % 100 == 0:
                logger.info(f"Labeled {i + 1}/{total} comments ({toxic_count} toxic)")
        
        if show_progress:
            logger.info(
                f"Labeling complete: {total} comments, "
                f"{toxic_count} toxic ({toxic_count/max(1,total)*100:.1f}%)"
            )
        
        return labeled


def label_comment(content: str) -> Dict[str, Any]:
    """
    Convenience function to label a single comment text
    
    Usage:
        from scraper.labelers import label_comment
        
        result = label_comment("Video hay quá!")
        # Returns:
        # {
        #     "sentiment": "positive",
        #     "toxic_score": 0.0,
        #     "topic": "khen_video"
        # }
    
    Args:
        content: Comment text
        
    Returns:
        Dict with sentiment, toxic_score, topic
    """
    # Use singleton labeler for efficiency
    global _default_labeler
    
    if '_default_labeler' not in globals() or _default_labeler is None:
        _default_labeler = NLPLabeler(use_ml_sentiment=False)
    
    result = _default_labeler.label_text(content)
    
    # Return simplified format as requested
    return {
        'sentiment': result['sentiment'],
        'toxic_score': result['toxic_score'],
        'topic': result['topic']
    }


# Initialize singleton as None
_default_labeler: Optional[NLPLabeler] = None
