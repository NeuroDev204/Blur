"""
Topic Classifier Module
Phân loại chủ đề bình luận
"""

import re
from typing import Literal, Tuple
from scraper.utils.logger import get_logger

logger = get_logger(__name__)

TopicType = Literal[
    'khen_video', 'che_video', 'hoi_thong_tin', 
    'tranh_luan', 'spam', 'hai_huoc', 'khac'
]


class TopicClassifier:
    """
    Vietnamese Topic Classifier
    
    Phân loại bình luận vào các chủ đề:
        - khen_video: Khen ngợi video/nội dung
        - che_video: Chê bai video/nội dung
        - hoi_thong_tin: Hỏi thông tin, câu hỏi
        - tranh_luan: Tranh luận, phản biện
        - spam: Spam, quảng cáo
        - hai_huoc: Bình luận hài hước
        - khac: Các loại khác
    """
    
    # Topic keywords
    TOPIC_KEYWORDS = {
        'khen_video': [
            # Khen hay
            'hay quá', 'hay lắm', 'tuyệt vời', 'xuất sắc', 'đỉnh cao',
            'video hay', 'clip hay', 'content hay', 'nội dung hay',
            'làm tốt', 'edit đẹp', 'chất lượng', 'pro', 'professional',
            # Ủng hộ
            'ủng hộ', 'subscribe', 'đăng ký', 'follow', 'theo dõi',
            'tiếp tục', 'thêm video', 'chờ video', 'mong video',
            # Thích
            'thích video', 'thích clip', 'thích nội dung', 'thích lắm',
            'yêu video', 'love this', 'amazing', 'awesome'
        ],
        
        'che_video': [
            # Chê tệ
            'video tệ', 'clip tệ', 'video dở', 'video nhạt', 'video chán',
            'nội dung tệ', 'content rác', 'content tệ',
            # Thất vọng
            'thất vọng', 'kỳ vọng', 'không như', 'không đúng',
            # Click bait
            'click bait', 'clickbait', 'bait', 'lừa', 'bị lừa',
            # Ghét
            'ghét video', 'không thích video', 'dislike', 'unsubscribe'
        ],
        
        'hoi_thong_tin': [
            # Câu hỏi
            'hỏi', 'cho hỏi', 'xin hỏi', 'ai biết', 'có ai biết',
            'là gì', 'ở đâu', 'khi nào', 'như thế nào', 'tại sao', 'bao giờ',
            # Yêu cầu thông tin
            'cho mình', 'cho em', 'cho xin', 'link', 'nguồn', 'source',
            'tên bài', 'tên nhạc', 'tên người', 'ai đây', 'đây là ai'
        ],
        
        'tranh_luan': [
            # Phản biện
            'không đồng ý', 'phản đối', 'sai rồi', 'không phải',
            'nhầm rồi', 'lầm rồi', 'sai', 'nhầm',
            # Tranh cãi
            'tranh luận', 'tranh cãi', 'cãi nhau', 'chống lại',
            # Bảo vệ quan điểm
            'theo tôi', 'theo mình', 'quan điểm', 'ý kiến',
            'thực tế là', 'sự thật là', 'thật ra'
        ],
        
        'spam': [
            # Quảng cáo
            'quảng cáo', 'ad', 'ads', 'mua', 'bán', 'giá', 'liên hệ',
            'inbox', 'pm', 'zalo', 'sđt', 'số điện thoại',
            # Kêu gọi
            'sub kênh', 'sub đi', 'subscribe kênh', 'đăng ký kênh',
            'follow mình', 'like share', 'chia sẻ',
            # Web/link
            'http', 'www.', '.com', '.vn', 'click link', 'vào link',
            # Lặp lại
            'first', 'đầu tiên', 'đầu', 'ai xem tới đây'
        ],
        
        'hai_huoc': [
            # Cười
            'haha', 'hihi', 'hehe', 'lol', 'lmao', 'rofl',
            'ahahaha', 'hihihi', 'hehehe',
            # Emoji cười
            '🤣', '😂', '😆', '😝', '🤪',
            # Từ vui
            'vui quá', 'buồn cười', 'hài', 'funny', 'comedy',
            'cười', 'tức cười', 'lầy', 'lầy lội',
            # Joke
            'joke', 'meme', 'troll'
        ]
    }
    
    # Question patterns
    QUESTION_PATTERNS = [
        r'\?+\s*$',  # Ends with ?
        r'^(ai|gì|sao|nào|đâu|khi|bao|mấy)\b',  # Starts with question word
        r'\b(là gì|ở đâu|như thế nào|tại sao|bao giờ|bao nhiêu)\b'
    ]
    
    def __init__(self):
        """Initialize classifier"""
        # Compile question patterns
        self._question_patterns = [re.compile(p, re.IGNORECASE) for p in self.QUESTION_PATTERNS]
    
    def _has_question(self, text: str) -> bool:
        """Check if text contains a question"""
        return any(p.search(text) for p in self._question_patterns)
    
    def _is_spam_pattern(self, text: str) -> bool:
        """Check for spam patterns"""
        text_lower = text.lower()
        
        # Check for URLs
        if re.search(r'https?://|www\.|\.com|\.vn', text_lower):
            return True
        
        # Check for phone numbers
        if re.search(r'0\d{9,10}|(\d{4}[.\-\s]){2,}\d{3,4}', text):
            return True
        
        # Check for repetitive characters
        if re.search(r'(.)\1{6,}', text):  # Same char 7+ times
            return True
        
        # Check for excessive caps
        caps = sum(1 for c in text if c.isupper())
        if len(text) > 10 and caps / len(text) > 0.7:
            return True
        
        return False
    
    def _count_keywords(self, text: str, topic: str) -> int:
        """Count topic keywords in text"""
        text_lower = text.lower()
        keywords = self.TOPIC_KEYWORDS.get(topic, [])
        
        count = 0
        for kw in keywords:
            if kw in text_lower:
                count += 1
        
        return count
    
    def classify(self, text: str) -> Tuple[TopicType, float]:
        """
        Classify text into topic
        
        Args:
            text: Text to classify
            
        Returns:
            Tuple (topic, confidence)
        """
        if not text or len(text.strip()) < 2:
            return 'khac', 0.5
        
        text_lower = text.lower()
        
        # Check spam first (high priority)
        spam_count = self._count_keywords(text, 'spam')
        if spam_count >= 2 or self._is_spam_pattern(text):
            return 'spam', min(0.9, 0.6 + spam_count * 0.1)
        
        # Check question
        if self._has_question(text):
            hoi_count = self._count_keywords(text, 'hoi_thong_tin')
            if hoi_count > 0 or '?' in text:
                return 'hoi_thong_tin', min(0.85, 0.6 + hoi_count * 0.1)
        
        # Check hai huoc
        hai_count = self._count_keywords(text, 'hai_huoc')
        if hai_count >= 2:
            return 'hai_huoc', min(0.85, 0.5 + hai_count * 0.1)
        
        # Count topic keywords
        topic_scores = {}
        for topic in ['khen_video', 'che_video', 'tranh_luan']:
            count = self._count_keywords(text, topic)
            if count > 0:
                topic_scores[topic] = count
        
        if topic_scores:
            # Get topic with highest score
            best_topic = max(topic_scores, key=topic_scores.get)
            score = topic_scores[best_topic]
            confidence = min(0.85, 0.5 + score * 0.15)
            return best_topic, confidence
        
        # Default
        return 'khac', 0.5
    
    def get_topic(self, text: str) -> TopicType:
        """Get topic label only"""
        topic, _ = self.classify(text)
        return topic
