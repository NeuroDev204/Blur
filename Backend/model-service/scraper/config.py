"""
Configuration Module
Tập trung quản lý cấu hình từ environment variables
"""

import os
from pathlib import Path
from dataclasses import dataclass, field
from typing import List, Optional
from dotenv import load_dotenv

# Load .env file
load_dotenv()


@dataclass
class RetryConfig:
    """Cấu hình retry"""
    max_retries: int = 5
    base_delay: float = 1.0
    max_delay: float = 60.0
    exponential_base: float = 2.0
    jitter: bool = True


@dataclass
class ProxyConfig:
    """Cấu hình proxy rotation"""
    enabled: bool = False
    proxies: List[str] = field(default_factory=list)
    rotation_interval: int = 10  # Đổi proxy sau mỗi N requests
    
    @classmethod
    def from_file(cls, filepath: str) -> 'ProxyConfig':
        """Load proxy list từ file"""
        if not os.path.exists(filepath):
            return cls(enabled=False)
        
        with open(filepath, 'r') as f:
            proxies = [line.strip() for line in f if line.strip()]
        
        return cls(enabled=len(proxies) > 0, proxies=proxies)


@dataclass
class YouTubeConfig:
    """Cấu hình YouTube API"""
    api_key: str = field(default_factory=lambda: os.getenv('YOUTUBE_API_KEY', ''))
    max_results_per_request: int = 100  # API limit
    quota_per_day: int = 10000
    rate_limit_delay: float = 0.1  # seconds between requests


@dataclass
class TikTokConfig:
    """Cấu hình TikTok scraper"""
    headless: bool = True
    max_scroll: int = 100
    scroll_delay_min: float = 0.5
    scroll_delay_max: float = 1.0
    page_load_timeout: int = 30
    profile_dir: str = field(default_factory=lambda: os.path.join(
        os.path.dirname(os.path.abspath(__file__)), 'chrome_profile'
    ))


@dataclass
class NLPConfig:
    """Cấu hình NLP labeling"""
    # Sentiment model
    use_ml_sentiment: bool = True
    sentiment_model: str = "nlptown/bert-base-multilingual-uncased-sentiment"
    
    # Topic classification
    use_ml_topic: bool = False  # False = dùng rule-based
    topic_model: str = ""
    
    # Toxic detection
    toxic_threshold: float = 0.45
    
    # Cache models
    cache_dir: str = field(default_factory=lambda: os.path.join(
        os.path.expanduser('~'), '.cache', 'comment_scraper'
    ))


@dataclass
class Config:
    """Main configuration"""
    # API & Scraping
    youtube: YouTubeConfig = field(default_factory=YouTubeConfig)
    tiktok: TikTokConfig = field(default_factory=TikTokConfig)
    
    # Retry & Proxy
    retry: RetryConfig = field(default_factory=RetryConfig)
    proxy: ProxyConfig = field(default_factory=ProxyConfig)
    
    # NLP
    nlp: NLPConfig = field(default_factory=NLPConfig)
    
    # Output
    output_dir: Path = field(default_factory=lambda: Path('data'))
    
    # Logging
    log_level: str = field(default_factory=lambda: os.getenv('LOG_LEVEL', 'INFO'))
    
    def __post_init__(self):
        """Ensure output directory exists"""
        self.output_dir.mkdir(parents=True, exist_ok=True)
    
    @classmethod
    def load(cls, env_file: Optional[str] = None) -> 'Config':
        """Load config từ environment"""
        if env_file:
            load_dotenv(env_file)
        
        config = cls()
        
        # Load proxy từ file nếu có
        proxy_file = os.getenv('PROXY_FILE', 'proxies.txt')
        if os.path.exists(proxy_file):
            config.proxy = ProxyConfig.from_file(proxy_file)
        
        return config


# Singleton instance
_config: Optional[Config] = None


def get_config() -> Config:
    """Get global config instance"""
    global _config
    if _config is None:
        _config = Config.load()
    return _config
