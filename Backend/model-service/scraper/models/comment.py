"""
Comment Data Models
Định nghĩa cấu trúc dữ liệu cho comments với validation
"""

from dataclasses import dataclass, field, asdict
from typing import Optional, Literal, List, Dict, Any
from datetime import datetime
import re
import unicodedata


@dataclass
class CommentLabel:
    """NLP labels cho comment"""
    sentiment: Literal['positive', 'neutral', 'negative'] = 'neutral'
    toxic_score: float = 0.0
    toxic_level: Literal['low', 'medium', 'high'] = 'low'
    toxic_categories: List[str] = field(default_factory=list)
    topic: Literal[
        'khen_video', 'che_video', 'hoi_thong_tin', 
        'tranh_luan', 'spam', 'hai_huoc', 'khac'
    ] = 'khac'
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            'sentiment': self.sentiment,
            'toxic_score': round(self.toxic_score, 3),
            'toxic_level': self.toxic_level,
            'topic': self.topic
        }


@dataclass
class CommentMetadata:
    """Metadata cho export"""
    platform: Literal['youtube', 'tiktok']
    total_comments: int
    crawled_at: str = field(default_factory=lambda: datetime.now().isoformat())
    video_id: Optional[str] = None
    video_title: Optional[str] = None
    
    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


@dataclass
class Comment:
    """
    Comment model với validation
    
    Fields:
        id: Unique identifier
        user: Username/author
        content: Comment text
        timestamp: Time posted (ISO format)
        platform: youtube hoặc tiktok
        like_count: Số likes
        reply_count: Số replies
        parent_id: ID của comment cha (nếu là reply)
        label: NLP labels
    """
    id: str
    user: str
    content: str
    timestamp: str
    platform: Literal['youtube', 'tiktok']
    like_count: int = 0
    reply_count: int = 0
    parent_id: Optional[str] = None
    label: Optional[CommentLabel] = None
    
    # Internal fields
    _is_valid: bool = field(default=True, repr=False)
    _validation_errors: List[str] = field(default_factory=list, repr=False)
    
    def __post_init__(self):
        """Validate và normalize sau khi khởi tạo"""
        self._validate()
        if self._is_valid:
            self._normalize()
    
    def _validate(self):
        """Validate comment data"""
        errors = []
        
        # Check không trống
        if not self.id or not self.id.strip():
            errors.append("ID không được trống")
        
        if not self.content or not self.content.strip():
            errors.append("Content không được trống")
            
        if not self.user:
            self.user = "unknown_user"
        
        # Check platform
        if self.platform not in ('youtube', 'tiktok'):
            errors.append(f"Platform không hợp lệ: {self.platform}")
        
        # Check ký tự invalid
        if self.content and self._has_invalid_chars(self.content):
            errors.append("Content chứa ký tự không hợp lệ")
        
        self._validation_errors = errors
        self._is_valid = len(errors) == 0
    
    def _has_invalid_chars(self, text: str) -> bool:
        """Kiểm tra ký tự lỗi"""
        # Kiểm tra null bytes và control characters
        for char in text:
            if unicodedata.category(char) == 'Cc' and char not in '\n\r\t':
                return True
        return False
    
    def _normalize(self):
        """Chuẩn hóa text"""
        self.content = self._normalize_text(self.content)
        self.user = self._normalize_text(self.user)
    
    @staticmethod
    def _normalize_text(text: str) -> str:
        """
        Chuẩn hóa text:
        - Xóa khoảng trắng thừa
        - Xóa xuống dòng thừa
        - Fix emoji lỗi
        """
        if not text:
            return text
        
        # Loại bỏ null bytes
        text = text.replace('\x00', '')
        
        # Normalize unicode
        text = unicodedata.normalize('NFC', text)
        
        # Xóa khoảng trắng thừa
        text = re.sub(r'[ \t]+', ' ', text)
        
        # Xóa xuống dòng thừa (giữ max 2 dòng liên tiếp)
        text = re.sub(r'\n{3,}', '\n\n', text)
        
        # Xóa khoảng trắng đầu/cuối mỗi dòng
        lines = [line.strip() for line in text.split('\n')]
        text = '\n'.join(lines)
        
        # Xóa khoảng trắng đầu/cuối
        text = text.strip()
        
        return text
    
    @property
    def is_reply(self) -> bool:
        """Check if comment is a reply"""
        return self.parent_id is not None
    
    @property
    def is_valid(self) -> bool:
        """Check if comment passed validation"""
        return self._is_valid
    
    @property
    def validation_errors(self) -> List[str]:
        """Get validation errors"""
        return self._validation_errors.copy()
    
    def to_dict(self, include_label: bool = True) -> Dict[str, Any]:
        """Convert to dictionary for JSON export"""
        data = {
            'id': self.id,
            'user': self.user,
            'content': self.content,
            'timestamp': self.timestamp,
            'platform': self.platform,
            'like_count': self.like_count,
            'reply_count': self.reply_count,
            'parent_id': self.parent_id
        }
        
        if include_label and self.label:
            data['label'] = self.label.to_dict()
        
        return data
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'Comment':
        """Create Comment from dictionary"""
        label_data = data.pop('label', None)
        label = None
        if label_data:
            label = CommentLabel(**label_data)
        
        return cls(**data, label=label)
    
    def __hash__(self):
        """Hash by content for deduplication"""
        return hash((self.content.lower().strip(), self.platform))
    
    def __eq__(self, other):
        """Equal by content for deduplication"""
        if not isinstance(other, Comment):
            return False
        return (
            self.content.lower().strip() == other.content.lower().strip() 
            and self.platform == other.platform
        )
