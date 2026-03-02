"""
Proxy Rotation Module
Quản lý và rotate proxy cho scraping
"""

import random
from typing import List, Optional, Dict
from dataclasses import dataclass, field
from scraper.utils.logger import get_logger

logger = get_logger(__name__)


@dataclass
class ProxyStats:
    """Thống kê sử dụng proxy"""
    total_requests: int = 0
    success_count: int = 0
    fail_count: int = 0
    last_error: Optional[str] = None
    
    @property
    def success_rate(self) -> float:
        if self.total_requests == 0:
            return 1.0
        return self.success_count / self.total_requests


class ProxyRotator:
    """
    Proxy rotation với health check
    
    Features:
        - Round-robin rotation
        - Lọc proxy fail
        - Health tracking
        - Random selection khi cần
    
    Usage:
        rotator = ProxyRotator(proxies=['http://proxy1:8080', 'http://proxy2:8080'])
        proxy = rotator.get_next()
        
        # After using
        rotator.mark_success(proxy)  # or rotator.mark_fail(proxy, "connection timeout")
    """
    
    def __init__(
        self,
        proxies: List[str],
        rotation_interval: int = 10,
        min_success_rate: float = 0.5,
        max_consecutive_fails: int = 3
    ):
        """
        Args:
            proxies: Danh sách proxy URLs
            rotation_interval: Đổi proxy sau mỗi N requests
            min_success_rate: Tỷ lệ thành công tối thiểu để giữ proxy
            max_consecutive_fails: Số lần fail liên tiếp trước khi loại proxy
        """
        self.proxies = proxies.copy()
        self.rotation_interval = rotation_interval
        self.min_success_rate = min_success_rate
        self.max_consecutive_fails = max_consecutive_fails
        
        # Tracking
        self._stats: Dict[str, ProxyStats] = {p: ProxyStats() for p in proxies}
        self._current_index = 0
        self._request_count = 0
        self._consecutive_fails: Dict[str, int] = {p: 0 for p in proxies}
        self._blacklist: set = set()
        
        logger.info(f"ProxyRotator initialized with {len(proxies)} proxies")
    
    @property
    def available_proxies(self) -> List[str]:
        """Danh sách proxy còn hoạt động"""
        return [p for p in self.proxies if p not in self._blacklist]
    
    @property
    def current_proxy(self) -> Optional[str]:
        """Proxy đang sử dụng"""
        available = self.available_proxies
        if not available:
            return None
        
        idx = self._current_index % len(available)
        return available[idx]
    
    def get_next(self) -> Optional[str]:
        """
        Lấy proxy tiếp theo
        
        Returns:
            Proxy URL hoặc None nếu không còn proxy
        """
        self._request_count += 1
        
        # Rotate sau mỗi N requests
        if self._request_count >= self.rotation_interval:
            self._rotate()
            self._request_count = 0
        
        proxy = self.current_proxy
        if proxy:
            self._stats[proxy].total_requests += 1
        
        return proxy
    
    def get_random(self) -> Optional[str]:
        """Lấy random proxy"""
        available = self.available_proxies
        if not available:
            return None
        
        proxy = random.choice(available)
        self._stats[proxy].total_requests += 1
        return proxy
    
    def _rotate(self):
        """Chuyển sang proxy tiếp theo"""
        available = self.available_proxies
        if len(available) > 1:
            self._current_index = (self._current_index + 1) % len(available)
            logger.debug(f"Rotated to proxy {self._current_index + 1}/{len(available)}")
    
    def mark_success(self, proxy: str):
        """Đánh dấu request thành công"""
        if proxy in self._stats:
            self._stats[proxy].success_count += 1
            self._consecutive_fails[proxy] = 0
    
    def mark_fail(self, proxy: str, error: Optional[str] = None):
        """Đánh dấu request thất bại"""
        if proxy not in self._stats:
            return
        
        self._stats[proxy].fail_count += 1
        self._stats[proxy].last_error = error
        self._consecutive_fails[proxy] += 1
        
        # Check if should blacklist
        if self._consecutive_fails[proxy] >= self.max_consecutive_fails:
            self._blacklist_proxy(proxy, "too many consecutive failures")
        elif self._stats[proxy].success_rate < self.min_success_rate:
            if self._stats[proxy].total_requests >= 10:  # Đủ sample size
                self._blacklist_proxy(proxy, f"success rate {self._stats[proxy].success_rate:.2%}")
    
    def _blacklist_proxy(self, proxy: str, reason: str):
        """Đưa proxy vào blacklist"""
        if proxy not in self._blacklist:
            self._blacklist.add(proxy)
            logger.warning(f"Blacklisted proxy: {proxy} ({reason})")
            
            # Rotate if current proxy is blacklisted
            if self.current_proxy is None:
                logger.error("All proxies blacklisted!")
    
    def reset_blacklist(self):
        """Reset blacklist (cho phép dùng lại tất cả proxy)"""
        self._blacklist.clear()
        self._consecutive_fails = {p: 0 for p in self.proxies}
        logger.info("Proxy blacklist reset")
    
    def get_stats(self) -> Dict[str, Dict]:
        """Lấy thống kê tất cả proxy"""
        return {
            proxy: {
                'total_requests': stats.total_requests,
                'success_count': stats.success_count,
                'fail_count': stats.fail_count,
                'success_rate': f"{stats.success_rate:.2%}",
                'is_blacklisted': proxy in self._blacklist,
                'last_error': stats.last_error
            }
            for proxy, stats in self._stats.items()
        }
    
    def format_proxy_for_selenium(self, proxy: str) -> Dict:
        """
        Format proxy cho Selenium WebDriver
        
        Returns:
            Dict với cấu hình cho Chrome options
        """
        # Parse proxy URL
        if '://' in proxy:
            _, proxy = proxy.split('://', 1)
        
        if '@' in proxy:
            # proxy có auth: user:pass@host:port
            auth, host_port = proxy.rsplit('@', 1)
            user, password = auth.split(':', 1)
            host, port = host_port.split(':')
            
            return {
                'host': host,
                'port': int(port),
                'user': user,
                'password': password
            }
        else:
            # proxy không có auth: host:port
            host, port = proxy.split(':')
            return {
                'host': host,
                'port': int(port),
                'user': None,
                'password': None
            }
    
    def format_proxy_for_aiohttp(self, proxy: str) -> str:
        """Format proxy URL cho aiohttp"""
        if not proxy.startswith('http'):
            proxy = f'http://{proxy}'
        return proxy
