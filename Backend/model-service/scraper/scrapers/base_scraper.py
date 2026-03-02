"""
Base Scraper Module
Abstract base class cho tất cả scrapers
"""

from abc import ABC, abstractmethod
from typing import List, Optional, Callable, Any
from scraper.models.comment import Comment
from scraper.config import get_config
from scraper.utils.logger import get_logger

logger = get_logger(__name__)


class BaseScraper(ABC):
    """
    Abstract base class cho scrapers
    
    Dễ dàng mở rộng thêm platform khác (Facebook, Instagram...)
    bằng cách kế thừa class này.
    """
    
    PLATFORM: str = "unknown"
    
    def __init__(self):
        self.config = get_config()
        self._progress_callback: Optional[Callable[[int, int, str], None]] = None
    
    def set_progress_callback(self, callback: Callable[[int, int, str], None]):
        """
        Set callback để báo progress
        
        Args:
            callback: function(current, total, message)
        """
        self._progress_callback = callback
    
    def _report_progress(self, current: int, total: int, message: str = ""):
        """Report progress nếu có callback"""
        if self._progress_callback:
            self._progress_callback(current, total, message)
    
    @abstractmethod
    def extract_video_id(self, url: str) -> str:
        """
        Trích xuất video ID từ URL
        
        Args:
            url: Video URL
            
        Returns:
            Video ID
        """
        pass
    
    @abstractmethod
    async def get_video_info(self, video_id: str) -> Optional[dict]:
        """
        Lấy thông tin video
        
        Args:
            video_id: Video ID
            
        Returns:
            Dict với video info hoặc None
        """
        pass
    
    @abstractmethod
    async def get_comments(
        self,
        video_url: str,
        max_results: int = 1000,
        include_replies: bool = True,
        vietnamese_only: bool = True
    ) -> List[Comment]:
        """
        Cào comments từ video
        
        Args:
            video_url: URL của video
            max_results: Số comments tối đa
            include_replies: Bao gồm replies
            vietnamese_only: Chỉ lấy comments tiếng Việt
            
        Returns:
            List of Comment objects
        """
        pass
    
    async def get_all_comments(
        self,
        video_urls: List[str],
        max_per_video: int = 1000,
        include_replies: bool = True,
        vietnamese_only: bool = True
    ) -> List[Comment]:
        """
        Cào comments từ nhiều videos
        
        Args:
            video_urls: List of video URLs
            max_per_video: Max comments per video
            include_replies: Include replies
            vietnamese_only: Vietnamese only
            
        Returns:
            List of all comments
        """
        all_comments = []
        total_videos = len(video_urls)
        
        for i, url in enumerate(video_urls, 1):
            try:
                self._report_progress(i, total_videos, f"Processing video {i}/{total_videos}")
                
                comments = await self.get_comments(
                    video_url=url,
                    max_results=max_per_video,
                    include_replies=include_replies,
                    vietnamese_only=vietnamese_only
                )
                
                all_comments.extend(comments)
                logger.info(f"[{i}/{total_videos}] Collected {len(comments)} comments from {self.extract_video_id(url)}")
                
            except Exception as e:
                logger.error(f"Error processing {url}: {e}")
                continue
        
        return all_comments
    
    def close(self):
        """Cleanup resources - sync version"""
        pass
    
    async def aclose(self):
        """Cleanup resources - async version"""
        pass
    
    def __enter__(self):
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        self.close()
    
    async def __aenter__(self):
        return self
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        # Use async close if available
        if hasattr(self, 'aclose'):
            await self.aclose()
        else:
            self.close()

