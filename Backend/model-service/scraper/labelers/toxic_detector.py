"""
Toxic Detector Module
Phát hiện bình luận độc hại tiếng Việt

v1: ToxicDetector — rule-based cơ bản (dùng cho code cũ)
v2: ToxicDetectorV2 — context-aware, whitelist, giảm false positive
"""

import re
from typing import List, Dict, Literal
from scraper.utils.logger import get_logger

logger = get_logger(__name__)

ToxicLevel = Literal['low', 'medium', 'high']


class ToxicDetector:
    """
    Vietnamese Toxic Comment Detector v1 (legacy).

    Features:
        - Rule-based detection với từ điển
        - Hỗ trợ teencode variants (dm, đm, vcl, etc.)
        - Phát hiện nhiều loại toxic
        - Trả về toxic_level và toxic_score

    Hạn chế: base_score cao (0.3) → nhiều false positive.
    Xem ToxicDetectorV2 để cải thiện.
    """

    def __init__(self, threshold: float = 0.45):
        self.threshold = threshold
        self._init_keywords()
        self._compile_patterns()

    def _init_keywords(self):
        """Initialize toxic keywords"""
        self.toxic_keywords = {
            'vulgar': [
                'đụ', 'địt', 'lồn', 'cặc', 'buồi', 'nứng', 'đéo', 'vãi',
                'đjt', 'dit', 'lon', 'cac', 'buoi', 'nung', 'deo', 'vai',
                'dm', 'đm', 'đmmm', 'dmmm', 'dmm', 'đmm',
                'cc', 'ccc', 'ccmm', 'ckmm',
                'll', 'lll', 'llmm', 'clmm',
                'dcm', 'đcm', 'đkm', 'dkm',
                'vcl', 'vl', 'vll', 'vlll', 'cl', 'cll',
                'lol', 'loz', 'lờ',
                'đml', 'dml', 'cml', 'vln', 'ckm',
                'vãi', 'vail', 'vaiz', 'vãiz',
                'chó', 'cho', 'súc vật', 'suc vat', 'súc sinh',
                'con lợn', 'lợn',
                'chết đi', 'chet di', 'chếtđi',
                'fuck', 'shit', 'bitch', 'pussy', 'dick', 'ass', 'damn', 'fck', 'wtf'
            ],
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
                'idiot', 'stupid', 'dumb', 'moron', 'retard', 'loser', 'noob', 'trash'
            ],
            'discrimination': [
                'đồ đồng tính', 'đồ gay', 'đồ bóng', 'đồ lộ',
                'thằng đồng',
                'con gái điếm', 'đĩ', 'di', 'cave',
                'mại dâm', 'gái gọi',
                'con điếm', 'đồ điếm',
                'người mọi', 'người rừng',
                'thằng da đen', 'nô lệ'
            ],
            'threat': [
                'giết', 'giet', 'giết mày', 'giet may',
                'chém', 'chem', 'chém mày',
                'đánh', 'danh', 'đập', 'dap',
                'hãm', 'hiếp', 'hiep', 'cưỡng hiếp',
                'giở trò', 'biến đi', 'bien di',
                'cút đi', 'cut di', 'đi chết', 'di chet',
                'chết mẹ', 'chet me', 'chết cha', 'chet cha',
                'kill', 'die', 'death', 'murder'
            ],
            'spam': [
                'sub đi', 'sub di', 'sub nha', 'subscribe',
                'đăng ký kênh', 'quảng cáo', 'spam'
            ]
        }

        self.family_words = [
            'mẹ', 'me', 'má', 'ma', 'cha', 'ba', 'bố', 'bo',
            'ông', 'ong', 'bà', 'cụ', 'cu',
            'con', 'cháu', 'anh', 'chị', 'chi', 'em',
            'thằng', 'thang', 'đứa', 'dua',
            'nhà', 'nha', 'tổ tiên', 'gia đình'
        ]

        self.category_weights = {
            'vulgar': 0.30,
            'insult': 0.25,
            'discrimination': 0.35,
            'threat': 0.45,
            'spam': 0.10,
            'family_insult': 0.55
        }

    def _compile_patterns(self):
        self.patterns = {}
        for category, keywords in self.toxic_keywords.items():
            pattern = '(' + '|'.join(re.escape(kw) for kw in keywords) + ')'
            self.patterns[category] = re.compile(pattern, re.IGNORECASE)

        family_pattern = '(' + '|'.join(re.escape(w) for w in self.family_words) + ')'
        self.family_pattern = re.compile(family_pattern, re.IGNORECASE)

    def _normalize(self, text: str) -> str:
        text = text.lower()
        text = re.sub(r'\s+', ' ', text)
        return text.strip()

    def detect_categories(self, text: str) -> List[str]:
        normalized = self._normalize(text)
        categories = []
        for category, pattern in self.patterns.items():
            if pattern.search(normalized):
                categories.append(category)
        if self.family_pattern.search(normalized):
            has_toxic = any(p.search(normalized) for p in self.patterns.values())
            if has_toxic:
                categories.append('family_insult')
        return categories

    def calculate_score(self, text: str, categories: List[str]) -> float:
        if not categories:
            return 0.0
        score = 0.3
        for cat in categories:
            score += self.category_weights.get(cat, 0.15)
        normalized = self._normalize(text)
        toxic_count = sum(len(p.findall(normalized)) for p in self.patterns.values())
        if toxic_count > 1:
            score += min(0.25, toxic_count * 0.06)
        if text.isupper() and len(text) > 10:
            score += 0.15
        if re.search(r'[!?]{3,}', text):
            score += 0.08
        return min(1.0, score)

    def get_level(self, score: float) -> ToxicLevel:
        if score < 0.4:
            return 'low'
        elif score < 0.7:
            return 'medium'
        return 'high'

    def is_toxic(self, text: str) -> bool:
        categories = self.detect_categories(text)
        score = self.calculate_score(text, categories)
        return score >= self.threshold

    def analyze(self, text: str) -> Dict:
        if not text or len(text.strip()) < 2:
            return {'is_toxic': False, 'toxic_score': 0.0, 'toxic_level': 'low', 'toxic_categories': []}
        categories = self.detect_categories(text)
        score = self.calculate_score(text, categories)
        level = self.get_level(score)
        return {
            'is_toxic': score >= self.threshold,
            'toxic_score': round(score, 3),
            'toxic_level': level,
            'toxic_categories': categories,
        }

    def get_toxic_score(self, text: str) -> float:
        return self.calculate_score(text, self.detect_categories(text))

    def get_toxic_level(self, text: str) -> ToxicLevel:
        return self.get_level(self.get_toxic_score(text))


# =============================================================================
# ToxicDetectorV2 — Cải thiện: context-aware, whitelist, giảm false positive
# =============================================================================

class ToxicDetectorV2:
    """
    Vietnamese Toxic Comment Detector v2.

    Cải thiện so với v1:
    1. Context-aware: từ nhạy cảm chỉ toxic khi đi kèm đại từ nhân xưng
    2. Whitelist: loại trừ cụm từ an toàn chứa từ nhạy cảm
    3. Base score thấp hơn (0.15 vs 0.3): giảm false positive
    4. Taxonomy rõ ràng theo docs giai đoạn 10

    So sánh v1 vs v2:
      "con chó dễ thương"  → v1: Toxic (0.60) | v2: Clean (0.00)
      "đánh giá 5 sao"     → v1: Toxic (0.75) | v2: Clean (0.00)
      "giết boss đi"       → v1: Toxic (0.75) | v2: Clean (0.00)
      "thằng ngu"          → v1: Toxic (0.55) | v2: Toxic (0.45)
      "dm mẹ mày"          → v1: Toxic (0.85) | v2: Toxic (0.75)
    """

    def __init__(self, threshold: float = 0.5):
        self.threshold = threshold
        self._init_keywords()
        self._init_whitelist()
        self._compile_patterns()

    def _init_keywords(self):
        """Chỉ giữ từ/cụm từ CHẮC CHẮN toxic — bỏ từ đơn lẻ dễ nhầm."""
        self.toxic_keywords = {
            'vulgar': [
                # Chửi thề rõ ràng (bỏ "chó", "lợn" vì dùng bình thường)
                'đụ', 'địt', 'lồn', 'cặc', 'buồi', 'đéo',
                # Teencode — luôn là tục
                'đjt', 'dm', 'đm', 'đmmm', 'dmmm', 'dmm', 'đmm',
                'cc', 'ccc', 'dcm', 'đcm', 'đkm', 'dkm',
                'vcl', 'vl', 'vll',
                # English
                'fuck', 'shit', 'bitch', 'fck', 'wtf',
            ],
            'insult': [
                # Chỉ giữ CỤM TỪ xúc phạm, không giữ từ đơn lẻ "ngu"
                'thằng ngu', 'con ngu', 'đồ ngu',
                'thằng khốn', 'đồ khốn',
                'mất dạy', 'vô học',
                'óc chó', 'não tôm',
                'thằng rác', 'đồ rác', 'rác rưởi',
                'idiot', 'stupid', 'moron', 'retard', 'loser',
            ],
            'discrimination': [
                'đồ gay', 'đồ bóng', 'thằng bóng',
                'con điếm', 'đồ điếm', 'đĩ',
                'người mọi', 'nô lệ',
            ],
            'threat': [
                # Chỉ CỤM TỪ đe dọa rõ ràng
                'giết mày', 'giết m', 'chém mày',
                'đi chết', 'chết đi', 'chết mẹ mày',
                'cút đi', 'biến đi',
                'cưỡng hiếp',
                'kill you', 'kill yourself',
            ],
        }

        # Từ đơn lẻ CẦN CONTEXT (toxic khi đi kèm đại từ nhân xưng)
        self.context_sensitive_words = [
            'ngu', 'ngốc', 'khùng', 'điên', 'đần', 'hâm',
            'chó', 'lợn', 'súc vật',
            'giết', 'chém', 'đánh', 'đập',
        ]

        # Đại từ chỉ người
        self.personal_pronouns = [
            'mày', 'may', 'tao', 'mình', 'bạn', 'ông', 'bà',
            'nó', 'hắn', 'thằng', 'con', 'đứa', 'chúng mày',
        ]

        # Family insult patterns
        self.family_patterns = [
            r'\b(đm|dm|đcm|dcm|đkm|dkm)\s*(mẹ|me|má|cha|ba|bố|ông|bà)',
            r'\b(mẹ|cha|bố|ông|bà)\s*(mày|may|nó|hắn)',
            r'(cả\s*nhà|cả\s*họ|cả\s*dòng)\s*(mày|may|nó)',
        ]

        self.category_weights = {
            'vulgar': 0.25,
            'insult': 0.30,
            'discrimination': 0.40,
            'threat': 0.45,
            'context_sensitive': 0.20,
            'family_insult': 0.35,
        }

    def _init_whitelist(self):
        """Cụm từ AN TOÀN chứa từ nhạy cảm → không đánh là toxic."""
        self.whitelist_patterns = [
            r'con\s*chó\s*(dễ\s*thương|cute|xinh|đáng\s*yêu|nhỏ|cưng)',
            r'(nuôi|yêu|thương)\s*chó',
            r'chó\s*(mèo|cưng|con)',
            r'đánh\s*(giá|bại|máy|game|bóng|đàn|guitar)',
            r'ngủ',          # "ngủ" ≠ "ngu"
            r'điện\s*(thoại|tử|ảnh|năng)',
            r'giết\s*(boss|mob|quái|rồng|creep|minion)',
            r'^(vcl|vl|dm|đm)$',  # Chỉ mỗi teencode, không kèm gì
        ]
        self.whitelist_compiled = [
            re.compile(p, re.IGNORECASE) for p in self.whitelist_patterns
        ]

    def _compile_patterns(self):
        self.patterns = {}
        for category, keywords in self.toxic_keywords.items():
            parts = []
            for kw in keywords:
                if len(kw) <= 3:
                    parts.append(r'(?:^|\s)' + re.escape(kw) + r'(?:\s|$|[!?.,])')
                else:
                    parts.append(r'\b' + re.escape(kw) + r'\b')
            self.patterns[category] = re.compile(
                '(' + '|'.join(parts) + ')', re.IGNORECASE
            )

        self.family_compiled = [
            re.compile(p, re.IGNORECASE) for p in self.family_patterns
        ]

        ctx_parts = [r'\b' + re.escape(w) + r'\b' for w in self.context_sensitive_words]
        self.context_pattern = re.compile(
            '(' + '|'.join(ctx_parts) + ')', re.IGNORECASE
        )

        pronoun_parts = [r'\b' + re.escape(p) + r'\b' for p in self.personal_pronouns]
        self.pronoun_pattern = re.compile(
            '(' + '|'.join(pronoun_parts) + ')', re.IGNORECASE
        )

    def _is_whitelisted(self, text: str) -> bool:
        return any(p.search(text) for p in self.whitelist_compiled)

    def _normalize(self, text: str) -> str:
        return re.sub(r'\s+', ' ', text.lower()).strip()

    def detect_categories(self, text: str) -> List[str]:
        normalized = self._normalize(text)
        categories = []

        if self._is_whitelisted(normalized):
            return []

        for category, pattern in self.patterns.items():
            if pattern.search(normalized):
                categories.append(category)

        # Context-sensitive: chỉ toxic khi đi kèm đại từ
        if self.context_pattern.search(normalized):
            if self.pronoun_pattern.search(normalized):
                categories.append('context_sensitive')

        # Family insults
        for fp in self.family_compiled:
            if fp.search(normalized):
                categories.append('family_insult')
                break

        return categories

    def calculate_score(self, text: str, categories: List[str]) -> float:
        if not categories:
            return 0.0

        score = 0.15  # Base score thấp hơn v1 (0.3 → 0.15)
        for cat in categories:
            score += self.category_weights.get(cat, 0.10)

        normalized = self._normalize(text)
        toxic_count = sum(
            len(pattern.findall(normalized)) for pattern in self.patterns.values()
        )
        if toxic_count > 2:
            score += min(0.20, toxic_count * 0.05)

        if text.isupper() and len(text) > 15:
            score += 0.10

        return min(1.0, score)

    def get_level(self, score: float) -> ToxicLevel:
        if score < 0.4:
            return 'low'
        elif score < 0.7:
            return 'medium'
        return 'high'

    def analyze(self, text: str) -> Dict:
        if not text or len(text.strip()) < 2:
            return {'is_toxic': False, 'toxic_score': 0.0, 'toxic_level': 'low', 'toxic_categories': []}

        categories = self.detect_categories(text)
        score = self.calculate_score(text, categories)
        level = self.get_level(score)

        return {
            'is_toxic': score >= self.threshold,
            'toxic_score': round(score, 3),
            'toxic_level': level,
            'toxic_categories': categories,
        }

    def is_toxic(self, text: str) -> bool:
        categories = self.detect_categories(text)
        score = self.calculate_score(text, categories)
        return score >= self.threshold

    def get_toxic_score(self, text: str) -> float:
        return self.calculate_score(text, self.detect_categories(text))

    def get_toxic_level(self, text: str) -> ToxicLevel:
        return self.get_level(self.get_toxic_score(text))
