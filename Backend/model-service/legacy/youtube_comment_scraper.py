"""
YouTube Comment Scraper
Crawl comments từ YouTube videos sử dụng YouTube Data API v3
"""

import re
from googleapiclient.discovery import build
from googleapiclient.errors import HttpError


def extract_video_id(url):
    """
    Trích xuất video ID từ YouTube URL
    
    Args:
        url (str): YouTube URL
        
    Returns:
        str: Video ID
    """
    # Patterns cho các dạng URL
    patterns = [
        r'(?:v=|\/)([0-9A-Za-z_-]{11}).*',
        r'(?:embed\/)([0-9A-Za-z_-]{11})',
        r'^([0-9A-Za-z_-]{11})$'
    ]
    
    for pattern in patterns:
        match = re.search(pattern, url)
        if match:
            return match.group(1)
    
    raise ValueError(f"Cannot extract video ID from URL: {url}")


class YouTubeCommentScraper:
    """YouTube Comment Scraper using YouTube Data API"""
    
    def __init__(self, api_key):
        """
        Initialize scraper
        
        Args:
            api_key (str): YouTube Data API key
        """
        self.api_key = api_key
        self.youtube = build('youtube', 'v3', developerKey=api_key)
        
    def get_video_info(self, video_id):
        """
        Lấy thông tin video
        
        Args:
            video_id (str): Video ID
            
        Returns:
            dict: Video info
        """
        try:
            request = self.youtube.videos().list(
                part='snippet,statistics',
                id=video_id
            )
            response = request.execute()
            
            if not response['items']:
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
            
        except HttpError as e:
            print(f"Error getting video info: {e}")
            return None
    
    def is_vietnamese(self, text):
        """
        Kiểm tra text có phải tiếng Việt không
        
        Args:
            text (str): Text to check
            
        Returns:
            bool: True if Vietnamese
        """
        # Các ký tự đặc trưng tiếng Việt
        vietnamese_chars = 'àáảãạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệìíỉĩịòóỏõọôồốổỗộơờớởỡợùúủũụưừứửữựỳýỷỹỵđ'
        vietnamese_chars += vietnamese_chars.upper()
        
        # Nếu có ít nhất 3 ký tự tiếng Việt trong text
        vietnamese_count = sum(1 for char in text if char in vietnamese_chars)
        
        return vietnamese_count >= 3
    
    def get_video_comments(self, video_id, max_results=100, include_replies=True, vietnamese_only=True):
        """
        Crawl comments từ video
        
        Args:
            video_id (str): Video ID
            max_results (int): Maximum number of comments
            include_replies (bool): Include replies
            vietnamese_only (bool): Only Vietnamese comments
            
        Returns:
            list: List of comments
        """
        comments = []
        
        try:
            request = self.youtube.commentThreads().list(
                part='snippet,replies',
                videoId=video_id,
                maxResults=min(100, max_results),  # API limit: 100 per request
                order='relevance',  # hoặc 'time'
                textFormat='plainText'
            )
            
            while request and len(comments) < max_results:
                response = request.execute()
                
                for item in response['items']:
                    # Top-level comment
                    top_comment = item['snippet']['topLevelComment']['snippet']
                    comment_text = top_comment['textDisplay']
                    
                    # Filter Vietnamese
                    if vietnamese_only and not self.is_vietnamese(comment_text):
                        continue
                    
                    comment_data = {
                        'text': comment_text,
                        'author': top_comment['authorDisplayName'],
                        'like_count': top_comment['likeCount'],
                        'published_at': top_comment['publishedAt'],
                        'comment_id': item['snippet']['topLevelComment']['id'],
                        'is_reply': False
                    }
                    
                    comments.append(comment_data)
                    
                    # Replies
                    if include_replies and 'replies' in item:
                        for reply in item['replies']['comments']:
                            reply_snippet = reply['snippet']
                            reply_text = reply_snippet['textDisplay']
                            
                            # Filter Vietnamese
                            if vietnamese_only and not self.is_vietnamese(reply_text):
                                continue
                            
                            reply_data = {
                                'text': reply_text,
                                'author': reply_snippet['authorDisplayName'],
                                'like_count': reply_snippet['likeCount'],
                                'published_at': reply_snippet['publishedAt'],
                                'comment_id': reply['id'],
                                'is_reply': True
                            }
                            
                            comments.append(reply_data)
                    
                    if len(comments) >= max_results:
                        break
                
                # Next page
                if 'nextPageToken' in response and len(comments) < max_results:
                    request = self.youtube.commentThreads().list(
                        part='snippet,replies',
                        videoId=video_id,
                        maxResults=min(100, max_results - len(comments)),
                        pageToken=response['nextPageToken'],
                        order='relevance',
                        textFormat='plainText'
                    )
                else:
                    request = None
                    
        except HttpError as e:
            if 'commentsDisabled' in str(e):
                print(f"   ⚠️  Comments disabled for video {video_id}")
            else:
                print(f"   ❌ API Error: {e}")
        
        return comments[:max_results]


if __name__ == "__main__":
    # Test
    API_KEY = "YOUR_API_KEY_HERE"
    VIDEO_URL = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    
    scraper = YouTubeCommentScraper(API_KEY)
    video_id = extract_video_id(VIDEO_URL)
    
    print(f"Video ID: {video_id}")
    
    # Get video info
    info = scraper.get_video_info(video_id)
    if info:
        print(f"Title: {info['title']}")
        print(f"Comments: {info['comment_count']}")
    
    # Get comments
    comments = scraper.get_video_comments(video_id, max_results=10)
    print(f"\nCollected {len(comments)} comments")
    
    for i, comment in enumerate(comments[:5], 1):
        print(f"\n{i}. {comment['author']}: {comment['text'][:100]}...")