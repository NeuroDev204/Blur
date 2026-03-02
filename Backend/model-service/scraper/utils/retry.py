"""
Retry Module
Decorator cho auto retry với exponential backoff
"""

import asyncio
import functools
import random
import time
from typing import Callable, Type, Tuple, Optional, Any
from scraper.utils.logger import get_logger

logger = get_logger(__name__)


def calculate_delay(
    attempt: int,
    base_delay: float = 1.0,
    max_delay: float = 60.0,
    exponential_base: float = 2.0,
    jitter: bool = True
) -> float:
    """
    Tính delay với exponential backoff
    
    Args:
        attempt: Số lần thử (0-indexed)
        base_delay: Delay cơ bản (seconds)
        max_delay: Delay tối đa (seconds)
        exponential_base: Base của exponential
        jitter: Thêm random jitter
    
    Returns:
        Delay time in seconds
    """
    delay = min(base_delay * (exponential_base ** attempt), max_delay)
    
    if jitter:
        # Thêm ±25% jitter
        jitter_range = delay * 0.25
        delay = delay + random.uniform(-jitter_range, jitter_range)
    
    return max(0.1, delay)


def async_retry(
    max_retries: int = 5,
    base_delay: float = 1.0,
    max_delay: float = 60.0,
    exponential_base: float = 2.0,
    jitter: bool = True,
    exceptions: Tuple[Type[Exception], ...] = (Exception,),
    on_retry: Optional[Callable[[int, Exception], None]] = None
):
    """
    Async retry decorator với exponential backoff
    
    Args:
        max_retries: Số lần retry tối đa
        base_delay: Delay cơ bản
        max_delay: Delay tối đa
        exponential_base: Base của exponential
        jitter: Thêm random jitter
        exceptions: Tuple các exception types cần retry
        on_retry: Callback khi retry (attempt, exception)
    
    Usage:
        @async_retry(max_retries=3)
        async def fetch_data():
            ...
    """
    def decorator(func: Callable) -> Callable:
        @functools.wraps(func)
        async def wrapper(*args, **kwargs) -> Any:
            last_exception = None
            
            for attempt in range(max_retries + 1):
                try:
                    return await func(*args, **kwargs)
                    
                except exceptions as e:
                    last_exception = e
                    
                    if attempt < max_retries:
                        delay = calculate_delay(
                            attempt, base_delay, max_delay, 
                            exponential_base, jitter
                        )
                        
                        logger.warning(
                            f"Attempt {attempt + 1}/{max_retries + 1} failed: {e}. "
                            f"Retrying in {delay:.2f}s..."
                        )
                        
                        if on_retry:
                            on_retry(attempt, e)
                        
                        await asyncio.sleep(delay)
                    else:
                        logger.error(
                            f"All {max_retries + 1} attempts failed. Last error: {e}"
                        )
            
            raise last_exception
        
        return wrapper
    return decorator


def sync_retry(
    max_retries: int = 5,
    base_delay: float = 1.0,
    max_delay: float = 60.0,
    exponential_base: float = 2.0,
    jitter: bool = True,
    exceptions: Tuple[Type[Exception], ...] = (Exception,),
    on_retry: Optional[Callable[[int, Exception], None]] = None
):
    """
    Sync retry decorator với exponential backoff
    
    Args:
        max_retries: Số lần retry tối đa
        base_delay: Delay cơ bản
        max_delay: Delay tối đa
        exponential_base: Base của exponential
        jitter: Thêm random jitter
        exceptions: Tuple các exception types cần retry
        on_retry: Callback khi retry (attempt, exception)
    
    Usage:
        @sync_retry(max_retries=3)
        def fetch_data():
            ...
    """
    def decorator(func: Callable) -> Callable:
        @functools.wraps(func)
        def wrapper(*args, **kwargs) -> Any:
            last_exception = None
            
            for attempt in range(max_retries + 1):
                try:
                    return func(*args, **kwargs)
                    
                except exceptions as e:
                    last_exception = e
                    
                    if attempt < max_retries:
                        delay = calculate_delay(
                            attempt, base_delay, max_delay, 
                            exponential_base, jitter
                        )
                        
                        logger.warning(
                            f"Attempt {attempt + 1}/{max_retries + 1} failed: {e}. "
                            f"Retrying in {delay:.2f}s..."
                        )
                        
                        if on_retry:
                            on_retry(attempt, e)
                        
                        time.sleep(delay)
                    else:
                        logger.error(
                            f"All {max_retries + 1} attempts failed. Last error: {e}"
                        )
            
            raise last_exception
        
        return wrapper
    return decorator


class RetryContext:
    """
    Context manager cho retry logic
    
    Usage:
        async with RetryContext(max_retries=3) as ctx:
            while ctx.should_retry():
                try:
                    result = await do_something()
                    ctx.success()
                except Exception as e:
                    await ctx.handle_error(e)
    """
    
    def __init__(
        self,
        max_retries: int = 5,
        base_delay: float = 1.0,
        max_delay: float = 60.0,
        exponential_base: float = 2.0,
        jitter: bool = True,
        exceptions: Tuple[Type[Exception], ...] = (Exception,)
    ):
        self.max_retries = max_retries
        self.base_delay = base_delay
        self.max_delay = max_delay
        self.exponential_base = exponential_base
        self.jitter = jitter
        self.exceptions = exceptions
        
        self.attempt = 0
        self.last_exception = None
        self._success = False
    
    async def __aenter__(self):
        return self
    
    async def __aexit__(self, exc_type, exc_val, exc_tb):
        if exc_type and issubclass(exc_type, self.exceptions):
            if self.attempt < self.max_retries:
                return True  # Suppress exception, will retry
        return False
    
    def should_retry(self) -> bool:
        """Check if should continue retrying"""
        return not self._success and self.attempt <= self.max_retries
    
    def success(self):
        """Mark as success"""
        self._success = True
    
    async def handle_error(self, e: Exception):
        """Handle error and wait before retry"""
        self.last_exception = e
        
        if self.attempt < self.max_retries:
            delay = calculate_delay(
                self.attempt, self.base_delay, self.max_delay,
                self.exponential_base, self.jitter
            )
            
            logger.warning(
                f"Attempt {self.attempt + 1}/{self.max_retries + 1} failed: {e}. "
                f"Retrying in {delay:.2f}s..."
            )
            
            await asyncio.sleep(delay)
            self.attempt += 1
        else:
            raise e
