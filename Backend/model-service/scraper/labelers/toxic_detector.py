"""
Toxic Detector Module
Phát hiện bình luận độc hại tiếng Việt
"""

import re
from typing import List, Dict, Tuple, Literal
from scraper.utils.logger import get_logger

logger = get_logger(__name__)

ToxicLevel = Literal['low', 'medium', 'high']


class ToxicDetector:
    """
    Vietnamese Toxic Comment Detector
    
    Features:
        - Rule-based detection với từ điển
        - Hỗ trợ teencode variants (dm, đm, vcl, etc.)
        - Phát hiện nhiều loại toxic
        - Trả về toxic_level và toxic_score
    """
    
    def __init__(self, threshold: float = 0.45):
        """
        Initialize detector
        
        Args:
            threshold: Ngưỡng để đánh giá là toxic (0.0 - 1.0)
        """
        self.threshold = threshold
        self._init_keywords()
        self._compile_patterns()
    
    def _init_keywords(self):
        """Initialize toxic keywords"""
        self.toxic_keywords = {
            # Tục tĩu / Vulgar
            'vulgar': [
                # Core words
                'đụ', 'địt', 'lồn', 'cặc', 'buồi', 'nứng', 'đéo', 'vãi',
                
                # Teencode variants (CRITICAL!)
                'đjt', 'dit', 'lon', 'cac', 'buoi', 'nung', 'deo', 'vai',
                'dm', 'đm', 'đmmm', 'dmmm', 'dmm', 'đmm',
                'cc', 'ccc', 'ccmm', 'ckmm',
                'll', 'lll', 'llmm', 'clmm',
                'dcm', 'đcm', 'đkm', 'dkm',
                'vcl', 'vl', 'vll', 'vlll', 'cl', 'cll',
                'lol', 'loz', 'lờ',
                
                # Compound variants
                'đml', 'dml', 'cml', 'vln', 'ckm',
                'vãi', 'vail', 'vaiz', 'vãiz',
                
                # Animals as insults
                'chó', 'cho', 'súc vật', 'suc vat', 'súc sinh',
                'con lợn', 'lợn',
                
                # Death wishes
                'chết đi', 'chet di', 'chếtđi',
                
                # English
                'fuck', 'shit', 'bitch', 'pussy', 'dick', 'ass', 'damn', 'fck', 'wtf'
            ],
            
            # Xúc phạm / Insult
            'insult': [
                'ngu', 'ngốc', 'ngoc', 'khùng', 'khung', 'điên', 'dien',
                'đần', 'dan', 'óc chó', 'oc cho', 'não tôm', 'nao tom',
                'mất dạy', 'mat day', 'vô học', 'vo hoc',
                'thằng ngu', 'thang ngu', 'con ngu',
                'thằng khốn', 'thang khon', 'đồ khốn', 'do khon',
                'thằng rác', 'thang rac', 'đồ rác', 'do rac', 'rác rưởi',
                'ngu học', 'ngu hoc', 'ngu dốt', 'ngu dot',
                'đồ ngớ ngẩn', 'hâm', 'ham',
                'con ranh', 'thằng ranh', 'đồ ranh',
                
                # English
                'idiot', 'stupid', 'dumb', 'moron', 'retard', 'loser', 'noob', 'trash'
            ],
            
            # Kỳ thị / Discrimination
            'discrimination': [
                'đồ đồng tính', 'đồ gay', 'đồ bóng', 'đồ lộ',
                'thằng đồng',
                'con gái điếm', 'đĩ', 'di', 'cave',
                'mại dâm', 'gái gọi',
                'con điếm', 'đồ điếm',
                'người mọi', 'người rừng',
                'thằng da đen', 'nô lệ'
            ],
            
            # Đe dọa / Threat
            'threat': [
                'giết', 'giet', 'giết mày', 'giet may',
                'chém', 'chem', 'chém mày',
                'đánh', 'danh', 'đập', 'dap',
                'hãm', 'hiếp', 'hiep', 'cưỡng hiếp',
                'giở trò', 'biến đi', 'bien di',
                'cút đi', 'cut di', 'đi chết', 'di chet',
                'chết mẹ', 'chet me', 'chết cha', 'chet cha',
                
                # English
                'kill', 'die', 'death', 'murder'
            ],
            
            # Spam
            'spam': [
                'sub đi', 'sub di', 'sub nha', 'subscribe',
                'đăng ký kênh', 'quảng cáo', 'spam'
            ]
        }
        
        # Family insults (amplifiers)
        self.family_words = [
            'mẹ', 'me', 'má', 'ma', 'cha', 'ba', 'bố', 'bo',
            'ông', 'ong', 'bà', 'cụ', 'cu',
            'con', 'cháu', 'anh', 'chị', 'chi', 'em',
            'thằng', 'thang', 'đứa', 'dua',
            'nhà', 'nha', 'tổ tiên', 'gia đình'
        ]
        
        # Category weights
        self.category_weights = {
            'vulgar': 0.30,
            'insult': 0.25,
            'discrimination': 0.35,
            'threat': 0.45,
            'spam': 0.10,
            'family_insult': 0.55
        }
    
    def _compile_patterns(self):
        """Compile regex patterns"""
        self.patterns = {}
        
        for category, keywords in self.toxic_keywords.items():
            pattern = '(' + '|'.join(re.escape(kw) for kw in keywords) + ')'
            self.patterns[category] = re.compile(pattern, re.IGNORECASE)
        
        # Family pattern
        family_pattern = '(' + '|'.join(re.escape(word) for word in self.family_words) + ')'
        self.family_pattern = re.compile(family_pattern, re.IGNORECASE)
    
    def _normalize(self, text: str) -> str:
        """Normalize text for detection"""
        text = text.lower()
        text = re.sub(r'\s+', ' ', text)
        return text.strip()
    
    def detect_categories(self, text: str) -> List[str]:
        """
        Detect toxic categories in text
        
        Args:
            text: Text to analyze
            
        Returns:
            List of detected categories
        """
        normalized = self._normalize(text)
        categories = []
        
        # Check each category
        for category, pattern in self.patterns.items():
            if pattern.search(normalized):
                categories.append(category)
        
        # Check family insults (must have toxic word too)
        if self.family_pattern.search(normalized):
            has_toxic = any(p.search(normalized) for p in self.patterns.values())
            if has_toxic:
                categories.append('family_insult')
        
        return categories
    
    def calculate_score(self, text: str, categories: List[str]) -> float:
        """
        Calculate toxicity score (0.0 - 1.0)
        
        Args:
            text: Original text
            categories: Detected categories
            
        Returns:
            Toxicity score
        """
        if not categories:
            return 0.0
        
        # Base score
        score = 0.3
        
        # Add category weights
        for cat in categories:
            score += self.category_weights.get(cat, 0.15)
        
        # Multiple toxic words
        normalized = self._normalize(text)
        toxic_count = sum(
            len(pattern.findall(normalized))
            for pattern in self.patterns.values()
        )
        
        if toxic_count > 1:
            score += min(0.25, toxic_count * 0.06)
        
        # All caps (SCREAMING)
        if text.isupper() and len(text) > 10:
            score += 0.15
        
        # Excessive punctuation (!!!, ???)
        if re.search(r'[!?]{3,}', text):
            score += 0.08
        
        return min(1.0, score)
    
    def get_level(self, score: float) -> ToxicLevel:
        """
        Convert score to level
        
        Args:
            score: Toxicity score (0.0 - 1.0)
            
        Returns:
            'low', 'medium', or 'high'
        """
        if score < 0.4:
            return 'low'
        elif score < 0.7:
            return 'medium'
        else:
            return 'high'
    
    def is_toxic(self, text: str) -> bool:
        """
        Check if text is toxic
        
        Args:
            text: Text to check
            
        Returns:
            True if toxic
        """
        categories = self.detect_categories(text)
        score = self.calculate_score(text, categories)
        return score >= self.threshold
    
    def analyze(self, text: str) -> Dict:
        """
        Full analysis of text
        
        Args:
            text: Text to analyze
            
        Returns:
            Dict with is_toxic, score, level, categories
        """
        if not text or len(text.strip()) < 2:
            return {
                'is_toxic': False,
                'toxic_score': 0.0,
                'toxic_level': 'low',
                'toxic_categories': []
            }
        
        categories = self.detect_categories(text)
        score = self.calculate_score(text, categories)
        level = self.get_level(score)
        is_toxic = score >= self.threshold
        
        return {
            'is_toxic': is_toxic,
            'toxic_score': round(score, 3),
            'toxic_level': level,
            'toxic_categories': categories
        }
    
    def get_toxic_score(self, text: str) -> float:
        """Get toxic score only"""
        categories = self.detect_categories(text)
        return self.calculate_score(text, categories)
    
    def get_toxic_level(self, text: str) -> ToxicLevel:
        """Get toxic level only"""
        score = self.get_toxic_score(text)
        return self.get_level(score)
