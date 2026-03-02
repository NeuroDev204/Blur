"""
Comment Validator Module
Validate và filter comments trước khi lưu
"""

import re
from typing import List, Set, Tuple
from scraper.models.comment import Comment
from scraper.utils.logger import get_logger

logger = get_logger(__name__)


class CommentValidator:
    """
    Validator cho comments
    
    Features:
        - Lọc comments trống
        - Loại bỏ trùng lặp
        - Lọc ký tự invalid
        - Kiểm tra độ dài
        - Kiểm tra tiếng Việt
    """
    
    # Ký tự đặc trưng tiếng Việt
    VIETNAMESE_CHARS = 'àáảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđ'
    VIETNAMESE_CHARS += VIETNAMESE_CHARS.upper()
    
    # Các từ tiếng Việt phổ biến
    VIETNAMESE_WORDS = [
        'không', 'được', 'của', 'một', 'này', 'với', 'là', 'có', 'bạn', 'mình',
        'anh', 'chị', 'em', 'ông', 'bà', 'tôi', 'đây', 'đó', 'như', 'thì',
        'hay', 'rất', 'lắm', 'quá', 'nhất', 'sao', 'gì', 'nào', 'nên', 'vì'
    ]
    
    def __init__(
        self,
        min_length: int = 2,
        max_length: int = 5000,
        vietnamese_only: bool = True,
        min_vietnamese_chars: int = 2
    ):
        """
        Args:
            min_length: Độ dài tối thiểu của comment
            max_length: Độ dài tối đa của comment
            vietnamese_only: Chỉ giữ comments tiếng Việt
            min_vietnamese_chars: Số ký tự tiếng Việt tối thiểu
        """
        self.min_length = min_length
        self.max_length = max_length
        self.vietnamese_only = vietnamese_only
        self.min_vietnamese_chars = min_vietnamese_chars
        
        # Tracking
        self._seen_contents: Set[str] = set()
        self._stats = {
            'total_processed': 0,
            'valid': 0,
            'empty': 0,
            'duplicate': 0,
            'too_short': 0,
            'too_long': 0,
            'not_vietnamese': 0,
            'invalid_chars': 0
        }
    
    def reset(self):
        """Reset validator state"""
        self._seen_contents.clear()
        self._stats = {k: 0 for k in self._stats}
    
    @property
    def stats(self) -> dict:
        """Get validation statistics"""
        return self._stats.copy()
    
    def is_vietnamese(self, text: str) -> bool:
        """
        Kiểm tra text có phải tiếng Việt không
        
        Args:
            text: Text to check
            
        Returns:
            True if Vietnamese
        """
        if not text or len(text.strip()) < 2:
            return False
        
        text_lower = text.lower()
        
        # Check Vietnamese characters
        vn_char_count = sum(1 for char in text if char in self.VIETNAMESE_CHARS)
        if vn_char_count >= self.min_vietnamese_chars:
            return True
        
        # Check Vietnamese words
        if any(word in text_lower for word in self.VIETNAMESE_WORDS):
            return True
        
        return False
    
    def _get_content_hash(self, content: str) -> str:
        """Tạo hash cho deduplication"""
        # Normalize: lowercase, strip, remove extra spaces
        normalized = content.lower().strip()
        normalized = re.sub(r'\s+', ' ', normalized)
        return normalized
    
    def validate_comment(self, comment: Comment) -> Tuple[bool, str]:
        """
        Validate một comment
        
        Args:
            comment: Comment to validate
            
        Returns:
            Tuple (is_valid, reason)
        """
        self._stats['total_processed'] += 1
        
        # Check model validation
        if not comment.is_valid:
            return False, f"Model validation failed: {comment.validation_errors}"
        
        content = comment.content
        
        # Check empty
        if not content or not content.strip():
            self._stats['empty'] += 1
            return False, "Empty content"
        
        # Check length
        if len(content) < self.min_length:
            self._stats['too_short'] += 1
            return False, f"Too short ({len(content)} < {self.min_length})"
        
        if len(content) > self.max_length:
            self._stats['too_long'] += 1
            return False, f"Too long ({len(content)} > {self.max_length})"
        
        # Check duplicate
        content_hash = self._get_content_hash(content)
        if content_hash in self._seen_contents:
            self._stats['duplicate'] += 1
            return False, "Duplicate content"
        
        # Check Vietnamese
        if self.vietnamese_only and not self.is_vietnamese(content):
            self._stats['not_vietnamese'] += 1
            return False, "Not Vietnamese"
        
        # Mark as seen
        self._seen_contents.add(content_hash)
        self._stats['valid'] += 1
        
        return True, "OK"
    
    def validate_batch(
        self,
        comments: List[Comment],
        return_invalid: bool = False
    ) -> Tuple[List[Comment], List[Tuple[Comment, str]]]:
        """
        Validate batch of comments
        
        Args:
            comments: List of comments to validate
            return_invalid: Trả về cả comments invalid nếu True
            
        Returns:
            Tuple (valid_comments, invalid_comments_with_reasons)
        """
        valid = []
        invalid = []
        
        for comment in comments:
            is_valid, reason = self.validate_comment(comment)
            
            if is_valid:
                valid.append(comment)
            elif return_invalid:
                invalid.append((comment, reason))
        
        # Log stats
        logger.info(
            f"Validation: {len(valid)}/{len(comments)} valid "
            f"({self._stats['duplicate']} duplicates, "
            f"{self._stats['not_vietnamese']} non-Vietnamese)"
        )
        
        if return_invalid:
            return valid, invalid
        return valid, []
    
    def deduplicate(self, comments: List[Comment]) -> List[Comment]:
        """
        Loại bỏ comments trùng lặp
        
        Args:
            comments: List of comments
            
        Returns:
            List of unique comments
        """
        seen = set()
        unique = []
        
        for comment in comments:
            content_hash = self._get_content_hash(comment.content)
            if content_hash not in seen:
                seen.add(content_hash)
                unique.append(comment)
        
        removed = len(comments) - len(unique)
        if removed > 0:
            logger.info(f"Removed {removed} duplicate comments")
        
        return unique
