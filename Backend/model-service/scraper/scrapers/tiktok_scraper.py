"""
TikTok Scraper Module
Selenium-based TikTok comment scraper với proxy support
"""

import undetected_chromedriver as uc
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
import time
import random
import re
import os
import asyncio
from pathlib import Path
from typing import List, Optional, Dict, Any
from datetime import datetime

from scraper.scrapers.base_scraper import BaseScraper
from scraper.models.comment import Comment
from scraper.config import get_config
from scraper.utils.logger import get_logger
from scraper.utils.retry import sync_retry

logger = get_logger(__name__)


class TikTokScraper(BaseScraper):
    """
    TikTok Comment Scraper
    
    Features:
        - Selenium với undetected-chromedriver
        - Persistent login session
        - Proxy support (nếu có)
        - Better error handling & recovery
        - Reply expansion
    """
    
    PLATFORM = "tiktok"
    
    # Vietnamese characters
    VIETNAMESE_CHARS = 'àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ'
    VIETNAMESE_WORDS = ['không', 'được', 'của', 'một', 'này', 'với', 'là', 'có', 'bạn', 'mình']
    
    def __init__(
        self,
        headless: bool = False,
        max_scroll: int = 100,
        proxy: Optional[str] = None
    ):
        """
        Initialize TikTok scraper
        
        Args:
            headless: Chạy Chrome ẩn
            max_scroll: Số lần scroll tối đa
            proxy: Proxy URL (optional)
        """
        super().__init__()
        
        self.headless = headless
        self.max_scroll = max_scroll
        self.proxy = proxy
        self.driver = None
        
        # Profile directory cho persistent session
        self.profile_dir = self.config.tiktok.profile_dir
        Path(self.profile_dir).mkdir(parents=True, exist_ok=True)
        
        logger.info(f"TikTokScraper initialized (headless={headless}, max_scroll={max_scroll})")
    
    def _init_driver(self):
        """Initialize Chrome WebDriver"""
        if self.driver:
            return
        
        options = uc.ChromeOptions()
        
        # User profile for persistent login
        options.add_argument(f'--user-data-dir={self.profile_dir}')
        options.add_argument('--profile-directory=Default')
        
        if self.headless:
            options.add_argument('--headless=new')
        else:
            options.add_argument('--start-maximized')
        
        options.add_argument('--no-sandbox')
        options.add_argument('--disable-dev-shm-usage')
        options.add_argument('--disable-blink-features=AutomationControlled')
        
        # Proxy nếu có
        if self.proxy:
            options.add_argument(f'--proxy-server={self.proxy}')
            logger.info(f"Using proxy: {self.proxy}")
        
        try:
            logger.info("Initializing Chrome...")
            self.driver = uc.Chrome(options=options, use_subprocess=True)
            logger.info("Chrome initialized successfully")
        except Exception as e:
            logger.error(f"Failed to initialize Chrome: {e}")
            raise
    
    def extract_video_id(self, url: str) -> str:
        """Extract video ID from TikTok URL"""
        match = re.search(r'/video/(\d+)', url)
        return match.group(1) if match else url
    
    def _is_vietnamese(self, text: str) -> bool:
        """Check if text contains Vietnamese"""
        if not text or len(text.strip()) < 2:
            return False
        
        text_lower = text.lower()
        
        # Check Vietnamese characters
        if any(c in text_lower for c in self.VIETNAMESE_CHARS):
            return True
        
        # Check Vietnamese words
        if any(w in text_lower for w in self.VIETNAMESE_WORDS):
            return True
        
        return False
    
    def _delay(self, min_sec: float = 0.5, max_sec: float = 1.5):
        """Random delay"""
        time.sleep(random.uniform(min_sec, max_sec))
    
    def _check_login(self) -> bool:
        """Check if logged in to TikTok"""
        try:
            time.sleep(2)
            # Check for avatar (indicates logged in)
            avatar = self.driver.find_elements(By.CSS_SELECTOR, '[data-e2e="nav-avatar"]')
            return len(avatar) > 0
        except:
            return False
    
    def _wait_for_login(self):
        """Wait for user to login manually"""
        logger.warning("=" * 70)
        logger.warning("⚠️  LOGIN REQUIRED")
        logger.warning("Please login to TikTok in the browser window")
        logger.warning("=" * 70)
        input("Press Enter after logging in...")
        time.sleep(5)
    
    def _scroll_comments(self, max_scrolls: int):
        """Scroll to load more comments - Natural mouse wheel scrolling"""
        import pyautogui
        pyautogui.FAILSAFE = False
        
        logger.info(f"Scrolling to load comments ({max_scrolls} times)...")
        
        # Wait for page to load
        time.sleep(2)
        
        # Use screen coordinates - right side where comment panel is
        screen_width, screen_height = pyautogui.size()
        scroll_x = int(screen_width * 0.85)
        scroll_y = int(screen_height * 0.5)
        
        logger.info(f"Will scroll at position: ({scroll_x}, {scroll_y})")
        
        # Move mouse and CLICK to focus the comment panel
        pyautogui.moveTo(scroll_x, scroll_y)
        time.sleep(0.2)
        pyautogui.click()
        time.sleep(0.3)
        
        last_comment_count = 0
        no_new_comments = 0
        
        for i in range(max_scrolls):
            # Very fast scroll
            pyautogui.scroll(-100)
            
            # Check count every 50 scrolls
            if (i + 1) % 30 == 0:
                try:
                    comments = self.driver.find_elements(
                        By.CSS_SELECTOR, 
                        'span.TUXText, [data-e2e="comment-level-1"]'
                    )
                    current_count = len(comments)
                except:
                    current_count = 0
                
                logger.info(f"Scroll {i + 1}/{max_scrolls} | Elements: ~{current_count}")
                
                if current_count == last_comment_count:
                    no_new_comments += 1
                    if no_new_comments >= 5:
                        logger.info(f"No new comments, stopping")
                        break
                else:
                    no_new_comments = 0
                
                last_comment_count = current_count
                time.sleep(0.3)  # Wait for lazy load
        
        logger.info(f"Finished scrolling, ~{last_comment_count} elements visible")
    
    def _expand_replies(self, max_rounds: int = 30):
        """Click 'View replies' buttons to expand"""
        logger.info("Expanding reply sections...")
        
        total_clicked = 0
        
        for round_num in range(max_rounds):
            try:
                # Find reply buttons
                reply_buttons = self.driver.find_elements(
                    By.CSS_SELECTOR, 
                    '[data-e2e="comment-reply-cta"]'
                )
                
                # Also try aria-label patterns
                for pattern in ['View reply', 'Xem phản hồi']:
                    btns = self.driver.find_elements(
                        By.CSS_SELECTOR, 
                        f'button[aria-label*="{pattern}"]'
                    )
                    reply_buttons.extend(btns)
                
                if not reply_buttons:
                    break
                
                # Click a few buttons per round
                num_to_click = min(len(reply_buttons), random.randint(1, 3))
                buttons_to_click = random.sample(reply_buttons, num_to_click)
                
                clicked = 0
                for btn in buttons_to_click:
                    try:
                        if not btn.is_displayed():
                            continue
                        
                        # Scroll into view
                        self.driver.execute_script(
                            "arguments[0].scrollIntoView({block: 'center'});", 
                            btn
                        )
                        self._delay(0.2, 0.4)
                        
                        btn.click()
                        clicked += 1
                        total_clicked += 1
                        self._delay(0.3, 0.6)
                        
                    except:
                        continue
                
                if clicked == 0:
                    break
                
                self._delay(0.5, 1.0)
                
            except:
                continue
        
        if total_clicked > 0:
            logger.info(f"Expanded {total_clicked} reply sections")
    
    def _extract_comments(self, video_id: str, vietnamese_only: bool) -> List[Comment]:
        """FIXED extraction - comprehensive strategies from legacy code"""
        comments: List[Comment] = []
        seen_content: set = set()
        
        try:
            time.sleep(2)  # Wait for DOM to settle
            
            # ✅ Try multiple strategies to find comment containers
            all_containers = []
            
            # Strategy 1: Direct comment selectors
            try:
                c1 = self.driver.find_elements(By.CSS_SELECTOR, '[data-e2e="comment-level-1"]')
                all_containers.extend(c1)
                logger.info(f"     Strategy 1: {len(c1)} comments")
            except:
                pass
            
            # Strategy 2: Comment item
            try:
                c2 = self.driver.find_elements(By.CSS_SELECTOR, '[data-e2e="comment-item"]')
                all_containers.extend(c2)
                logger.info(f"     Strategy 2: {len(c2)} comments")
            except:
                pass
            
            # Strategy 3: Div containers with Comment class
            try:
                c3 = self.driver.find_elements(By.CSS_SELECTOR, 'div[class*="Comment"]')
                all_containers.extend(c3)
                logger.info(f"     Strategy 3: {len(c3)} comments") 
            except:
                pass
            
            # Strategy 4: Find parent containers of TUXText spans (new TikTok layout)
            try:
                tux_spans = self.driver.find_elements(
                    By.CSS_SELECTOR, 
                    'span[class*="TUXText"][class*="StyledTUXText"], span.TUXText'
                )
                for span in tux_spans:
                    try:
                        # Get parent container
                        parent = span.find_element(By.XPATH, './ancestor::div[contains(@class, "comment") or contains(@class, "Comment")]')
                        if parent and parent not in all_containers:
                            all_containers.append(parent)
                    except:
                        continue
                logger.info(f"     Strategy 4 (TUXText): {len(tux_spans)} spans found")
            except:
                pass
            
            # Remove duplicates by element id
            unique_containers = []
            seen_ids = set()
            for c in all_containers:
                try:
                    cid = c.id
                    if cid and cid not in seen_ids:
                        seen_ids.add(cid)
                        unique_containers.append(c)
                except:
                    continue
            
            logger.info(f"Processing {len(unique_containers)} unique containers...")
            
            for container in unique_containers:
                try:
                    # Get HTML for checking reply status
                    html = container.get_attribute('outerHTML') or ''
                    is_reply = 'level-2' in html.lower() or 'reply' in html.lower()
                    
                    # ✅ STEP 1: Get author with multiple strategies
                    author = ''
                    author_variations = set()
                    
                    # Strategy 1: Direct username selector
                    try:
                        author_elem = container.find_element(
                            By.CSS_SELECTOR, '[data-e2e="comment-username"]'
                        )
                        author = author_elem.text.strip().replace('@', '')
                    except:
                        pass
                    
                    # Strategy 2: Username link
                    if not author:
                        try:
                            author_elem = container.find_element(
                                By.CSS_SELECTOR, 'a[href*="/@"]'
                            )
                            author = author_elem.text.strip().replace('@', '')
                        except:
                            pass
                    
                    # Strategy 3: Extract from href attribute
                    if not author:
                        try:
                            links = container.find_elements(By.TAG_NAME, 'a')
                            for link in links:
                                href = link.get_attribute('href') or ''
                                if '/@' in href:
                                    username = href.split('/@')[-1].split('/')[0].split('?')[0]
                                    if username and len(username) > 0:
                                        author = username
                                        break
                        except:
                            pass
                    
                    # Build author variations for filtering
                    if author:
                        author_variations.add(author)
                        author_variations.add(author.replace('@', ''))
                        author_variations.add(author + ' √')
                        author_variations.add(author + ' ✓')
                        author_variations.add('@' + author)
                        author_variations.add(author.lower())
                    
                    # ✅ STEP 2: Extract ALL text from container - NO FILTERING
                    text = ''
                    
                    # Just get the container text directly
                    try:
                        text = container.text.strip()
                        # Remove author from text if present
                        if author and text.startswith(author):
                            text = text[len(author):].strip()
                    except:
                        continue
                    
                    # Only skip if completely empty
                    if not text:
                        continue
                    
                    # Simple dedup by first 50 chars
                    content_hash = text[:50].lower()
                    if content_hash in seen_content:
                        continue
                    seen_content.add(content_hash)
                    
                    # Extract likes
                    likes = 0
                    try:
                        likes_elem = container.find_element(
                            By.CSS_SELECTOR, '[data-e2e="comment-like-count"]'
                        )
                        likes_text = likes_elem.text.strip()
                        if 'K' in likes_text:
                            likes = int(float(likes_text.replace('K', '')) * 1000)
                        elif 'M' in likes_text:
                            likes = int(float(likes_text.replace('M', '')) * 1000000)
                        elif likes_text.isdigit():
                            likes = int(likes_text)
                    except:
                        pass
                    
                    # Set author if still empty
                    if not author:
                        author = 'unknown_user'
                    
                    # Create comment
                    comment = Comment(
                        id=f"{video_id}_{len(comments)}",
                        user=author,
                        content=text,
                        timestamp=datetime.now().isoformat(),
                        platform='tiktok',
                        like_count=likes,
                        reply_count=0,
                        parent_id=None if not is_reply else f"{video_id}_parent"
                    )
                    
                    if comment.is_valid:
                        comments.append(comment)
                
                except Exception as e:
                    logger.debug(f"Error processing container: {e}")
                    continue
        
        except Exception as e:
            logger.error(f"Extraction error: {e}")
        
        logger.info(f"     ✅ Extracted {len(comments)} comments")
        return comments
    
    async def get_video_info(self, video_id: str) -> Optional[Dict[str, Any]]:
        """Get video info (limited for TikTok)"""
        return {
            'video_id': video_id,
            'platform': 'tiktok'
        }
    
    async def get_comments(
        self,
        video_url: str,
        max_results: int = 3000,
        include_replies: bool = True,
        vietnamese_only: bool = True
    ) -> List[Comment]:
        """
        Cào comments từ TikTok video
        
        Note: Sử dụng sync selenium internally
        """
        # Run sync code
        return await asyncio.get_event_loop().run_in_executor(
            None,
            lambda: self._get_comments_sync(
                video_url, max_results, include_replies, vietnamese_only
            )
        )
    
    @sync_retry(
        max_retries=3,
        base_delay=5.0,
        exceptions=(Exception,)
    )
    def _get_comments_sync(
        self,
        video_url: str,
        max_results: int,
        include_replies: bool,
        vietnamese_only: bool
    ) -> List[Comment]:
        """Sync version of get_comments"""
        
        # Initialize driver if needed
        if not self.driver:
            self._init_driver()
            
            logger.info("Opening TikTok...")
            self.driver.get("https://www.tiktok.com")
            self._delay(5, 8)
            
            if not self._check_login():
                self._wait_for_login()
        
        video_id = self.extract_video_id(video_url)
        logger.info(f"[{video_id}] Starting...")
        
        try:
            # Navigate to video
            logger.info(f"[{video_id}] Loading video page...")
            self.driver.get(video_url)
            self._delay(5, 8)
            
            # Click on Comments tab to ensure it's open (TikTok new layout)
            try:
                comment_tabs = self.driver.find_elements(
                    By.XPATH, 
                    "//span[contains(text(), 'Comments')] | //span[contains(text(), 'comments')] | //div[contains(text(), '1774 comments')]"
                )
                if not comment_tabs:
                    # Try clicking the comment icon
                    comment_icons = self.driver.find_elements(
                        By.CSS_SELECTOR,
                        '[data-e2e="comment-icon"], button[aria-label*="comment"], [class*="comment-icon"]'
                    )
                    for icon in comment_icons:
                        try:
                            if icon.is_displayed():
                                icon.click()
                                logger.info("Clicked comment icon")
                                self._delay(2, 3)
                                break
                        except:
                            continue
                else:
                    for tab in comment_tabs:
                        try:
                            if tab.is_displayed():
                                tab.click()
                                logger.info("Clicked Comments tab")
                                self._delay(2, 3)
                                break
                        except:
                            continue
            except Exception as e:
                logger.debug(f"Could not click Comments tab: {e}")
            
            # Wait for comments to load
            self._delay(3, 5)
            
            # Load comments by scrolling
            self._scroll_comments(self.max_scroll)
            
            # Expand replies
            if include_replies:
                self._expand_replies()
            
            # Wait for DOM
            time.sleep(3)
            
            # Extract comments
            comments = self._extract_comments(video_id, vietnamese_only)
            
            # Stats
            main_count = sum(1 for c in comments if c.parent_id is None)
            reply_count = len(comments) - main_count
            
            logger.info(
                f"[{video_id}] Collected {len(comments)} comments "
                f"({main_count} main, {reply_count} replies)"
            )
            
            return comments[:max_results]
            
        except Exception as e:
            logger.error(f"[{video_id}] Error: {e}")
            raise
    
    def close(self):
        """Close browser"""
        if self.driver:
            try:
                self.driver.quit()
            except:
                pass
            self.driver = None
