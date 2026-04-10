"""
YouTube Scraper Module
Async YouTube comment scraper với retry và full pagination
"""

import re
import asyncio
import aiohttp
from typing import List, Optional, Dict, Any
from datetime import datetime

from scraper.scrapers.base_scraper import BaseScraper
from scraper.models.comment import Comment
from scraper.config import get_config
from scraper.utils.logger import get_logger
from scraper.utils.retry import async_retry

logger = get_logger(__name__)


class YouTubeScraper(BaseScraper):
    """
    YouTube Comment Scraper
    
    Features:
        - Async/await với aiohttp
        - Auto retry với exponential backoff
        - Full pagination để cào TOÀN BỘ comments
        - Rate limiting
        - Quota-aware
    """
    
    PLATFORM = "youtube"
    API_BASE = "https://www.googleapis.com/youtube/v3"
    
    # Vietnamese characters for detection
    VIETNAMESE_CHARS = 'àáảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđ'
    VIETNAMESE_CHARS += VIETNAMESE_CHARS.upper()
    
    def __init__(self, api_key: Optional[str] = None):
        """
        Initialize YouTube scraper
        
        Args:
            api_key: YouTube Data API key (hoặc lấy từ environment)
        """
        super().__init__()
        
        self.api_key = api_key or self.config.youtube.api_key
        if not self.api_key:
            raise ValueError("YouTube API key is required. Set YOUTUBE_API_KEY environment variable.")
        
        self._session: Optional[aiohttp.ClientSession] = None
        self._quota_used = 0
    
    async def _get_session(self) -> aiohttp.ClientSession:
        """Get or create aiohttp session"""
        if self._session is None or self._session.closed:
            timeout = aiohttp.ClientTimeout(total=30)
            self._session = aiohttp.ClientSession(timeout=timeout)
        return self._session
    
    def extract_video_id(self, url: str) -> str:
        """
        Trích xuất video ID từ YouTube URL
        
        Supports:
            - https://www.youtube.com/watch?v=VIDEO_ID
            - https://youtu.be/VIDEO_ID
            - https://www.youtube.com/embed/VIDEO_ID
        """
        patterns = [
            r'(?:v=|/)([0-9A-Za-z_-]{11}).*',
            r'(?:embed/)([0-9A-Za-z_-]{11})',
            r'^([0-9A-Za-z_-]{11})$'
        ]
        
        for pattern in patterns:
            match = re.search(pattern, url)
            if match:
                return match.group(1)
        
        raise ValueError(f"Cannot extract video ID from URL: {url}")
    
    def _is_vietnamese(self, text: str) -> bool:
        """Check if text contains Vietnamese"""
        if not text or len(text.strip()) < 2:
            return False
        
        vietnamese_count = sum(1 for char in text if char in self.VIETNAMESE_CHARS)
        return vietnamese_count >= 3
    
    @async_retry(
        max_retries=5,
        base_delay=1.0,
        exceptions=(aiohttp.ClientError, asyncio.TimeoutError)
    )
    async def _api_request(
        self,
        endpoint: str,
        params: Dict[str, Any]
    ) -> Dict[str, Any]:
        """
        Make API request với retry
        
        Args:
            endpoint: API endpoint (e.g., 'videos', 'commentThreads')
            params: Query parameters
            
        Returns:
            API response as dict
        """
        session = await self._get_session()
        params['key'] = self.api_key
        
        url = f"{self.API_BASE}/{endpoint}"
        
        async with session.get(url, params=params) as response:
            if response.status == 403:
                error_data = await response.json()
                error_reason = error_data.get('error', {}).get('errors', [{}])[0].get('reason', '')
                
                if error_reason == 'quotaExceeded':
                    raise Exception("YouTube API quota exceeded")
                elif error_reason == 'commentsDisabled':
                    logger.warning("Comments disabled for this video")
                    return {'items': []}
                
                raise aiohttp.ClientResponseError(
                    response.request_info,
                    response.history,
                    status=response.status
                )
            
            response.raise_for_status()
            
            # Track quota usage (rough estimate)
            self._quota_used += 1
            
            # Rate limiting
            await asyncio.sleep(self.config.youtube.rate_limit_delay)
            
            return await response.json()
    
    async def get_video_info(self, video_id: str) -> Optional[Dict[str, Any]]:
        """Lấy thông tin video"""
        try:
            response = await self._api_request(
                'videos',
                {
                    'part': 'snippet,statistics',
                    'id': video_id
                }
            )
            
            if not response.get('items'):
                return None
            
            video = response['items'][0]
            
            return {
                'video_id': video_id,
                'title': video['snippet']['title'],
                'channel': video['snippet']['channelTitle'],
                'published_at': video['snippet']['publishedAt'],
                'view_count': int(video['statistics'].get('viewCount', 0)),
                'like_count': int(video['statistics'].get('likeCount', 0)),
                'comment_count': int(video['statistics'].get('commentCount', 0))
            }
            
        except Exception as e:
            logger.error(f"Error getting video info: {e}")
            return None
    
    async def get_comments(
        self,
        video_url: str,
        max_results: int = 1000,
        include_replies: bool = True,
        vietnamese_only: bool = True
    ) -> List[Comment]:
        """
        Cào comments với full pagination
        
        Args:
            video_url: YouTube video URL
            max_results: Số comments tối đa
            include_replies: Bao gồm replies
            vietnamese_only: Chỉ lấy tiếng Việt
            
        Returns:
            List of Comment objects
        """
        video_id = self.extract_video_id(video_url)
        comments: List[Comment] = []
        page_token = None
        
        logger.info(f"[{video_id}] Starting to collect comments...")
        
        while len(comments) < max_results:
            try:
                # Build request params
                params = {
                    'part': 'snippet,replies',
                    'videoId': video_id,
                    'maxResults': min(100, max_results - len(comments)),
                    'order': 'relevance',
                    'textFormat': 'plainText'
                }
                
                if page_token:
                    params['pageToken'] = page_token
                
                # Make request
                response = await self._api_request('commentThreads', params)
                
                items = response.get('items', [])
                if not items:
                    break
                
                # Process comments
                for item in items:
                    if len(comments) >= max_results:
                        break
                    
                    # Top-level comment
                    top_comment_data = item['snippet']['topLevelComment']['snippet']
                    comment_text = top_comment_data['textDisplay']
                    
                    # Vietnamese filter
                    if vietnamese_only and not self._is_vietnamese(comment_text):
                        continue
                    
                    comment = Comment(
                        id=item['snippet']['topLevelComment']['id'],
                        user=top_comment_data['authorDisplayName'],
                        content=comment_text,
                        timestamp=top_comment_data['publishedAt'],
                        platform='youtube',
                        like_count=top_comment_data.get('likeCount', 0),
                        reply_count=item['snippet'].get('totalReplyCount', 0),
                        parent_id=None
                    )
                    
                    if comment.is_valid:
                        comments.append(comment)
                    
                    # Process replies
                    if include_replies and 'replies' in item:
                        for reply in item['replies']['comments']:
                            if len(comments) >= max_results:
                                break
                            
                            reply_data = reply['snippet']
                            reply_text = reply_data['textDisplay']
                            
                            if vietnamese_only and not self._is_vietnamese(reply_text):
                                continue
                            
                            reply_comment = Comment(
                                id=reply['id'],
                                user=reply_data['authorDisplayName'],
                                content=reply_text,
                                timestamp=reply_data['publishedAt'],
                                platform='youtube',
                                like_count=reply_data.get('likeCount', 0),
                                reply_count=0,
                                parent_id=item['snippet']['topLevelComment']['id']
                            )
                            
                            if reply_comment.is_valid:
                                comments.append(reply_comment)
                
                # Next page
                page_token = response.get('nextPageToken')
                if not page_token:
                    break
                    
            except Exception as e:
                if 'commentsDisabled' in str(e):
                    logger.warning(f"[{video_id}] Comments are disabled")
                else:
                    logger.error(f"[{video_id}] Error: {e}")
                break
        
        # Stats
        main_count = sum(1 for c in comments if c.parent_id is None)
        reply_count = len(comments) - main_count
        
        logger.info(
            f"[{video_id}] Collected {len(comments)} comments "
            f"({main_count} main, {reply_count} replies)"
        )
        
        return comments[:max_results]
    
    def close(self):
        """Close aiohttp session - sync version"""
        if self._session and not self._session.closed:
            try:
                # Try to get running loop
                loop = asyncio.get_running_loop()
                # If we're in an async context, schedule the close
                loop.create_task(self._session.close())
            except RuntimeError:
                # No running loop, safe to use run_until_complete
                try:
                    loop = asyncio.get_event_loop()
                    if not loop.is_running():
                        loop.run_until_complete(self._session.close())
                except Exception:
                    pass  # Silently ignore - session will be garbage collected
    
    async def aclose(self):
        """Async close"""
        if self._session and not self._session.closed:
            await self._session.close()

