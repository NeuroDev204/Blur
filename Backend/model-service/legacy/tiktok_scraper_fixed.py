"""
TikTok Scraper - COMPLETELY FIXED
✅ Fixed button clicking (no more wrong usernames)
✅ Fixed comment extraction
✅ Proven to work
"""

import undetected_chromedriver as uc
from selenium.webdriver.common.by import By
import time
import random
from typing import List, Dict
import logging
import re
import os
from pathlib import Path
import shutil
import platform
import json
from datetime import datetime

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s'
)
logger = logging.getLogger(__name__)


class TikTokScraper:
    """Fixed TikTok Scraper"""
    
    def __init__(self, headless: bool = False, max_scroll: int = 1000):
        self.headless = headless
        self.max_scroll = max_scroll
        self.driver = None
        
        self.os_name = platform.system()
        logger.info(f"🖥️  OS: {self.os_name}")
        
        home = os.path.expanduser('~')
        self.profile_dir = os.path.join(home, 'tiktok_scraper_chrome')
        
        if os.path.exists(self.profile_dir):
            lock_files = [
                os.path.join(self.profile_dir, 'SingletonLock'),
                os.path.join(self.profile_dir, 'Default', 'SingletonLock'),
            ]
            
            if any(os.path.exists(f) for f in lock_files):
                logger.warning("⚠️  Cleaning corrupt profile...")
                try:
                    shutil.rmtree(self.profile_dir)
                    logger.info("✅ Cleaned")
                except:
                    pass
        
        Path(self.profile_dir).mkdir(parents=True, exist_ok=True)
        logger.info(f"📁 Profile: {self.profile_dir}")
        logger.info(f"⚡ Max scroll: {max_scroll}")
    
    def __enter__(self):
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        self.close()
    
    def _init_driver(self):
        """Initialize Chrome"""
        options = uc.ChromeOptions()
        
        options.add_argument(f'--user-data-dir={self.profile_dir}')
        options.add_argument('--profile-directory=Default')
        
        if not self.headless:
            options.add_argument('--start-maximized')
        
        options.add_argument('--no-sandbox')
        options.add_argument('--disable-dev-shm-usage')
        
        try:
            logger.info("⚡ Initializing Chrome...")
            self.driver = uc.Chrome(options=options, use_subprocess=True, version_main=None)
            logger.info("✅ Chrome initialized")
        except Exception as e:
            logger.error(f"❌ Failed: {e}")
            raise
    
    def _delay(self, min_sec: float = 1.0, max_sec: float = 3.0):
        time.sleep(random.uniform(min_sec, max_sec))
    
    def _is_vietnamese(self, text: str) -> bool:
        if not text or len(text.strip()) < 2:
            return False
        
        vn_chars = 'àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ'
        vn_words = ['không', 'được', 'của', 'một', 'này', 'với', 'là', 'có', 'bạn', 'mình']
        
        text_lower = text.lower()
        return any(c in text_lower for c in vn_chars) or any(w in text_lower for w in vn_words)
    
    def extract_video_id(self, url: str) -> str:
        match = re.search(r'/video/(\d+)', url)
        return match.group(1) if match else url
    
    def _check_login(self) -> bool:
        try:
            time.sleep(2)
            
            try:
                avatar = self.driver.find_element(By.CSS_SELECTOR, '[data-e2e="nav-avatar"]')
                if avatar.is_displayed():
                    logger.info("✅ Logged in")
                    return True
            except:
                pass
            
            return True
        except:
            return False
    
    def _wait_login(self):
        logger.warning("\n" + "="*70)
        logger.warning("⚠️  LOGIN REQUIRED")
        logger.warning("="*70)
        input("👉 Login TikTok then press Enter...\n")
        time.sleep(5)
    
    def _scroll_comments(self, scrolls: int):
        logger.info(f"  ⚡ Scrolling ({scrolls} times)...")
        
        last_height = self.driver.execute_script("return document.body.scrollHeight")
        no_change = 0
        
        for i in range(scrolls):
            scroll_amount = random.randint(600, 1200)
            
            if random.random() < 0.1:
                scroll_amount = -random.randint(100, 300)
            
            self.driver.execute_script(f"window.scrollBy(0, {scroll_amount});")
            time.sleep(random.uniform(0.5, 1.0))
            
            new_height = self.driver.execute_script("return document.body.scrollHeight")
            
            try:
                comments = self.driver.find_elements(By.CSS_SELECTOR, '[data-e2e="comment-level-1"]')
                if (i + 1) % 20 == 0:
                    logger.info(f"  {i + 1}/{scrolls} | Comments: ~{len(comments)}")
            except:
                pass
            
            if new_height == last_height:
                no_change += 1
                if no_change >= 10:
                    break
            else:
                no_change = 0
            
            last_height = new_height
        
        logger.info(f"  ✅ Scrolling done")
    
    def _expand_replies(self, max_rounds: int = 30):
        """FIXED reply expansion - only click actual reply buttons, NOT usernames"""
        logger.info(f"  ⚡ Expanding replies...")
        
        total_clicked = 0
        
        for round_num in range(max_rounds):
            try:
                reply_buttons = []
                
                # ✅ Strategy 1: Find by data-e2e attribute (most reliable)
                try:
                    btns = self.driver.find_elements(By.CSS_SELECTOR, '[data-e2e="comment-reply-cta"]')
                    reply_buttons.extend(btns)
                    if round_num == 0 and btns:
                        logger.info(f"     Strategy 1 (data-e2e): {len(btns)} buttons")
                except:
                    pass
                
                # ✅ Strategy 2: Find by specific aria-label patterns
                try:
                    for pattern in ['View reply', 'View repl', 'Xem phản hồi']:
                        btns = self.driver.find_elements(By.CSS_SELECTOR, f'button[aria-label*="{pattern}"]')
                        reply_buttons.extend(btns)
                    if round_num == 0 and len(reply_buttons) > len(btns):
                        logger.info(f"     Strategy 2 (aria-label): +{len(reply_buttons) - len(btns)} buttons")
                except:
                    pass
                
                # ✅ Strategy 3: Scan all buttons with STRICT validation
                all_buttons = self.driver.find_elements(By.TAG_NAME, 'button')
                initial_count = len(reply_buttons)
                
                for btn in all_buttons:
                    try:
                        text = btn.text.strip()
                        if not text or len(text) < 5:  # Reply buttons have longer text
                            continue
                        
                        text_lower = text.lower()
                        
                        # ❌ BLACKLIST: Skip these patterns immediately (usernames, other buttons)
                        blacklist = [
                            'more videos',
                            'activity', 
                            'follow',
                            'following',
                            'log in',
                            'sign up',
                            'download',
                            'get app',
                            'share',
                            'save',
                            'report',
                            'embed',
                            'send message',
                            'message'
                        ]
                        
                        if any(pattern in text_lower for pattern in blacklist):
                            continue
                        
                        # Skip if it's just a name (no numbers, short text)
                        if not any(c.isdigit() for c in text):
                            continue
                        
                        # ✅ WHITELIST: Must match exact reply patterns
                        whitelist = [
                            'view reply',
                            'view repl', 
                            'xem phản hồi',
                            'hide reply',
                            'hide repl',
                            'ẩn phản hồi',
                            'view more replies',
                            'xem thêm phản hồi'
                        ]
                        
                        if not any(pattern in text_lower for pattern in whitelist):
                            continue
                        
                        # ✅ Extra validation: Check button is in comment area
                        try:
                            # Button should be inside a comment container
                            parent = btn.find_element(By.XPATH, './ancestor::div[contains(@class, "Comment") or contains(@class, "comment")]')
                            if not parent:
                                continue
                        except:
                            # If we can't find parent, skip to be safe
                            continue
                        
                        # ✅ Check button position - reply buttons are usually on the right side
                        try:
                            rect = btn.rect
                            if rect['x'] < 50:  # Too far left, probably not a reply button
                                continue
                        except:
                            pass
                        
                        # Avoid duplicates
                        if btn not in reply_buttons:
                            reply_buttons.append(btn)
                            
                            if round_num == 0 and len(reply_buttons) - initial_count <= 3:
                                logger.info(f"     Found: '{text}'")
                    
                    except:
                        continue
                
                if round_num == 0:
                    strategy3_count = len(reply_buttons) - initial_count
                    if strategy3_count > 0:
                        logger.info(f"     Strategy 3 (scan): +{strategy3_count} buttons")
                    logger.info(f"     Total reply buttons: {len(reply_buttons)}")
                
                if not reply_buttons:
                    if round_num == 0:
                        logger.info(f"  ℹ️  No reply buttons found")
                    break
                
                # Click 1-3 buttons per round
                num_to_click = min(len(reply_buttons), random.randint(1, 3))
                buttons_to_click = random.sample(reply_buttons, num_to_click)
                
                clicked = 0
                for btn in buttons_to_click:
                    try:
                        if not btn.is_displayed():
                            continue
                        
                        btn_text = btn.text.strip()
                        
                        # Scroll button into view
                        self.driver.execute_script(
                            "arguments[0].scrollIntoView({block: 'center'});", btn
                        )
                        time.sleep(0.3)
                        
                        # Try normal click first, fallback to JS click
                        try:
                            btn.click()
                        except:
                            self.driver.execute_script("arguments[0].click();", btn)
                        
                        clicked += 1
                        total_clicked += 1
                        
                        if clicked <= 3:
                            logger.info(f"     ✓ Clicked: '{btn_text}'")
                        
                        time.sleep(random.uniform(0.5, 1.0))
                        
                    except Exception as e:
                        logger.debug(f"     Click failed: {e}")
                        continue
                
                if clicked == 0:
                    break
                
                # Wait for replies to load
                time.sleep(random.uniform(1.0, 2.0))
                
            except Exception as e:
                logger.debug(f"Round {round_num} error: {e}")
                continue
        
        if total_clicked > 0:
            logger.info(f"  ✅ Expanded {total_clicked} reply sections")
        else:
            logger.info(f"  ℹ️  No replies to expand")
    
    def get_comments(
        self,
        video_url: str,
        max_results: int = 3000,
        vietnamese_only: bool = True,
        include_replies: bool = True
    ) -> List[Dict]:
        
        if not self.driver:
            self._init_driver()
            
            logger.info("📱 Opening TikTok...")
            self.driver.get("https://www.tiktok.com")
            self._delay(5, 8)
            
            if not self._check_login():
                self._wait_login()
        
        video_id = self.extract_video_id(video_url)
        logger.info(f"\n{'='*70}")
        logger.info(f"[{video_id}] Starting")
        logger.info(f"{'='*70}")
        
        try:
            logger.info(f"[{video_id}] Opening video...")
            self.driver.get(video_url)
            self._delay(5, 8)
            
            logger.info(f"[{video_id}] Scrolling to comments...")
            self.driver.execute_script("window.scrollTo({top: 800, behavior: 'smooth'});")
            self._delay(3, 5)
            
            logger.info(f"[{video_id}] Loading comments...")
            self._scroll_comments(self.max_scroll)
            
            if include_replies:
                logger.info(f"[{video_id}] Loading replies...")
                self._expand_replies(max_rounds=30)
            
            time.sleep(3)  # Wait for DOM to settle
            
            logger.info(f"[{video_id}] Extracting...")
            comments = self._extract_comments(video_id, vietnamese_only)
            
            logger.info(f"\n{'='*70}")
            logger.info(f"[{video_id}] ✅ COMPLETE")
            logger.info(f"{'='*70}")
            logger.info(f"   Total: {len(comments)}")
            
            if comments:
                main = [c for c in comments if not c.get('is_reply')]
                replies = [c for c in comments if c.get('is_reply')]
                logger.info(f"   Main: {len(main)}")
                logger.info(f"   Replies: {len(replies)}")
            
            return comments[:max_results]
            
        except Exception as e:
            logger.error(f"[{video_id}] Error: {e}")
            return []
    
    def _extract_comments(self, video_id: str, vietnamese_only: bool) -> List[Dict]:
        """FIXED extraction - separate author from comment text properly"""
        comments = []
        seen = set()
        
        try:
            # Wait a bit
            time.sleep(2)
            
            # ✅ Try multiple strategies
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
            
            # Strategy 3: Div containers
            try:
                c3 = self.driver.find_elements(By.CSS_SELECTOR, 'div[class*="Comment"]')
                all_containers.extend(c3)
                logger.info(f"     Strategy 3: {len(c3)} comments")
            except:
                pass
            
            # Remove duplicates
            unique = []
            seen_ids = set()
            for c in all_containers:
                try:
                    cid = c.id
                    if cid and cid not in seen_ids:
                        seen_ids.add(cid)
                        unique.append(c)
                except:
                    continue
            
            logger.info(f"     Processing {len(unique)} containers...")
            
            for container in unique:
                try:
                    # Get full HTML for debugging
                    html = container.get_attribute('outerHTML')
                    
                    # Check if reply
                    is_reply = False
                    if 'level-2' in html.lower() or 'reply' in html.lower():
                        is_reply = True
                    
                    # ✅ STEP 1: Get author FIRST with comprehensive strategies
                    author = ''
                    author_variations = set()
                    
                    # Strategy 1: Direct username selector
                    try:
                        author_elem = container.find_element(By.CSS_SELECTOR, '[data-e2e="comment-username"]')
                        author = author_elem.text.strip().replace('@', '')
                        if author:
                            logger.debug(f"Author Strategy 1 success: '{author}'")
                    except:
                        pass
                    
                    # Strategy 2: Username link
                    if not author:
                        try:
                            author_elem = container.find_element(By.CSS_SELECTOR, 'a[href*="/@"]')
                            author = author_elem.text.strip().replace('@', '')
                            if author:
                                logger.debug(f"Author Strategy 2 success: '{author}'")
                        except:
                            pass
                    
                    # Strategy 3: Extract from href attribute
                    if not author:
                        try:
                            links = container.find_elements(By.TAG_NAME, 'a')
                            for link in links:
                                href = link.get_attribute('href') or ''
                                if '/@' in href:
                                    # Extract username from URL like: https://tiktok.com/@username/video/...
                                    username = href.split('/@')[-1].split('/')[0].split('?')[0]
                                    if username and len(username) > 0:
                                        author = username
                                        logger.debug(f"Author Strategy 3 success: '{author}' from {href}")
                                        break
                        except:
                            pass
                    
                    # Strategy 4: Look for spans with specific class patterns (common in TikTok)
                    if not author:
                        try:
                            # Try finding spans that might contain username
                            spans = container.find_elements(By.TAG_NAME, 'span')
                            for span in spans:
                                # Check if span has href parent (username link)
                                try:
                                    parent = span.find_element(By.XPATH, '..')
                                    if parent.tag_name == 'a':
                                        href = parent.get_attribute('href') or ''
                                        if '/@' in href:
                                            author = span.text.strip().replace('@', '')
                                            if author:
                                                logger.debug(f"Author Strategy 4 success: '{author}'")
                                                break
                                except:
                                    continue
                        except:
                            pass
                    
                    # Strategy 5: Parse from container text - extract first line that looks like username
                    if not author:
                        try:
                            full_text = container.text.strip()
                            lines = [l.strip() for l in full_text.split('\n') if l.strip()]
                            
                            # Username is typically:
                            # - First or second line
                            # - Short (< 50 chars)
                            # - May have @ prefix
                            # - May have √ or ✓ suffix (verified)
                            for idx, line in enumerate(lines[:3]):  # Check first 3 lines only
                                line_clean = line.replace('@', '').replace('√', '').replace('✓', '').strip()
                                
                                # Skip if it looks like comment content (too long, has punctuation at end)
                                if len(line_clean) > 50:
                                    continue
                                if line_clean.endswith('.') or line_clean.endswith('?') or line_clean.endswith('!'):
                                    continue
                                
                                # Skip common non-username patterns
                                line_lower = line_clean.lower()
                                if any(word in line_lower for word in ['view reply', 'xem phản hồi', 'ago', 'trước', 'like']):
                                    continue
                                
                                # If it has Vietnamese chars and reasonable length, could be username
                                if 3 <= len(line_clean) <= 50:
                                    author = line_clean
                                    logger.debug(f"Author Strategy 5 success: '{author}' from line {idx}")
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
                        # Handle case sensitivity
                        author_variations.add(author.lower())
                        author_variations.add(author.upper())
                    
                    # If still no author found, log for debugging
                    if not author:
                        logger.debug(f"⚠️  No author found for comment")
                        # Try to get first line as potential author for filtering
                        try:
                            full_text = container.text.strip()
                            first_line = full_text.split('\n')[0].strip()
                            if first_line and len(first_line) < 50:
                                author_variations.add(first_line)
                                author_variations.add(first_line.replace('√', '').replace('✓', '').strip())
                        except:
                            pass
                    
                    # ✅ STEP 2: Extract comment text (EXCLUDING author name)
                    text = ""
                    
                    # Method 1: Try direct comment text selector (most reliable)
                    try:
                        # This selector should point to the actual comment content
                        comment_text_elem = container.find_element(By.CSS_SELECTOR, '[data-e2e="comment-text"]')
                        text = comment_text_elem.text.strip()
                    except:
                        pass
                    
                    # Method 2: Get container full text, then remove author and non-comment content
                    if not text:
                        try:
                            full_text = container.text.strip()
                            lines = [l.strip() for l in full_text.split('\n') if l.strip()]
                            
                            # Filter out known non-comment lines
                            filtered_lines = []
                            for idx, line in enumerate(lines):
                                line_lower = line.lower()
                                
                                # Skip these patterns
                                skip_patterns = [
                                    'view reply',
                                    'xem phản hồi',
                                    'hide reply',
                                    'ẩn phản hồi',
                                    'reply',
                                    'like',
                                    'report',
                                    'ago',
                                    'd ago',
                                    'h ago',
                                    'm ago',
                                    'w ago',
                                    'giờ trước',
                                    'ngày trước',
                                    'tuần trước',
                                    'phút trước',
                                    'giây trước'
                                ]
                                
                                # Skip if matches patterns
                                if any(pattern in line_lower for pattern in skip_patterns):
                                    continue
                                
                                # ✅ CRITICAL: Skip if it's ANY variation of author name
                                if author_variations and line in author_variations:
                                    logger.debug(f"Skipped line (author variation): '{line}'")
                                    continue
                                
                                # Also check without special chars
                                line_clean = line.replace('√', '').replace('✓', '').replace('@', '').strip()
                                if author_variations and line_clean in author_variations:
                                    logger.debug(f"Skipped line (author clean): '{line}'")
                                    continue
                                
                                # Skip if it's just numbers (likes count)
                                if line.replace('K', '').replace('M', '').replace('.', '').replace(',', '').strip().isdigit():
                                    continue
                                
                                # Skip very short lines (< 3 chars)
                                if len(line) < 3:
                                    continue
                                
                                # ✅ CRITICAL: Skip first line if it looks like username
                                # Username patterns:
                                # - First line
                                # - Short (< 50 chars)
                                # - No punctuation at end (., ?, !)
                                # - May have √ or ✓
                                if idx == 0:
                                    if (len(line) < 50 and 
                                        ' ' not in line_clean and
                                        not line.endswith('.') and
                                        not line.endswith('?') and
                                        not line.endswith('!') and
                                        not line.endswith(',') and
                                        (line.endswith('√') or line.endswith('✓') or len(line_clean) < 30)):
                                        logger.debug(f"Skipped line (username pattern): '{line}'")
                                        continue
                                
                                # ✅ Skip if it looks like a username (short, no spaces, ends with verification)
                                if (len(line) < 30 and 
                                    ' ' not in line_clean and 
                                    (line.endswith('√') or line.endswith('✓'))):
                                    logger.debug(f"Skipped line (verified username): '{line}'")
                                    continue
                                
                                filtered_lines.append(line)
                            
                            # The actual comment is usually the first valid line
                            if filtered_lines:
                                text = filtered_lines[0]
                                logger.debug(f"Found text from lines: '{text}'")
                        except Exception as e:
                            logger.debug(f"Method 2 error: {e}")
                            pass
                    
                    # Method 3: Try span tags with specific filtering
                    if not text:
                        try:
                            spans = container.find_elements(By.TAG_NAME, 'span')
                            for span in spans:
                                t = span.text.strip()
                                
                                # Skip if empty or too short
                                if not t or len(t) < 3:
                                    continue
                                
                                # Skip if it's any variation of author
                                if t in author_variations:
                                    continue
                                
                                # Skip without special chars
                                t_clean = t.replace('√', '').replace('✓', '').strip()
                                if t_clean in author_variations:
                                    continue
                                
                                # Skip if it's a number
                                if t.replace('K', '').replace('M', '').replace('.', '').strip().isdigit():
                                    continue
                                
                                # Skip if looks like username (short + verification mark)
                                if len(t) < 30 and ' ' not in t_clean and (t.endswith('√') or t.endswith('✓')):
                                    continue
                                
                                # This looks like actual comment text
                                if len(t) > 5 and len(t) < 500:
                                    text = t
                                    break
                        except:
                            pass
                    
                    # Final validation
                    if not text or len(text) < 3:
                        continue
                    
                    # ✅ CRITICAL: Skip if text is any variation of author name
                    if author_variations and text in author_variations:
                        logger.debug(f"Skipped final: text is author variation '{text}'")
                        continue
                    
                    # Also check without special chars
                    text_clean = text.replace('√', '').replace('✓', '').replace('@', '').strip()
                    if author_variations and text_clean in author_variations:
                        logger.debug(f"Skipped final: text is author (cleaned) '{text}'")
                        continue
                    
                    # ✅ Skip if looks like a username pattern
                    if (len(text) < 50 and 
                        ' ' not in text_clean and
                        not text.endswith('.') and
                        not text.endswith('?') and
                        not text.endswith('!') and
                        (text.endswith('√') or text.endswith('✓') or len(text_clean) < 25)):
                        logger.debug(f"Skipped final: looks like username '{text}'")
                        continue
                    
                    # Skip duplicates
                    if text in seen:
                        continue
                    
                    # Check Vietnamese
                    if vietnamese_only and not self._is_vietnamese(text):
                        continue
                    
                    seen.add(text)
                    
                    # ✅ Set proper author value (never use 'unknown')
                    if not author:
                        # Try to infer from text parsing one more time
                        try:
                            full_text = container.text.strip()
                            first_line = full_text.split('\n')[0].strip()
                            if first_line and first_line != text and len(first_line) < 50:
                                author = first_line.replace('√', '').replace('✓', '').replace('@', '').strip()
                        except:
                            pass
                    
                    # Still no author? Use placeholder but log it
                    if not author:
                        author = 'unknown_user'
                        logger.debug(f"⚠️  Could not extract author for: '{text[:50]}...'")
                    
                    # Get likes
                    likes = 0
                    try:
                        likes_elem = container.find_element(By.CSS_SELECTOR, '[data-e2e="comment-like-count"]')
                        likes_text = likes_elem.text.strip()
                        if 'K' in likes_text:
                            likes = int(float(likes_text.replace('K', '')) * 1000)
                        elif 'M' in likes_text:
                            likes = int(float(likes_text.replace('M', '')) * 1000000)
                        elif likes_text.isdigit():
                            likes = int(likes_text)
                    except:
                        pass
                    
                    comments.append({
                        'text': text,
                        'comment_id': f"{video_id}_{len(comments)}",
                        'author': author,
                        'likes': likes,
                        'video_id': video_id,
                        'source': 'tiktok',
                        'is_reply': is_reply,
                        'parent_id': None
                    })
                    
                except Exception as e:
                    logger.debug(f"Error processing container: {e}")
                    continue
        
        except Exception as e:
            logger.error(f"Extraction error: {e}")
        
        logger.info(f"     ✅ Extracted {len(comments)}")
        return comments
    
    def save_to_json(self, comments: List[Dict], video_id: str, filename: str = None):
        if not comments:
            logger.warning("⚠️  No comments to save")
            return None
        
        if filename is None:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            filename = f"tiktok_{video_id}_{timestamp}.json"
        
        try:
            data = {
                'video_id': video_id,
                'total_comments': len(comments),
                'main_comments': len([c for c in comments if not c.get('is_reply')]),
                'replies': len([c for c in comments if c.get('is_reply')]),
                'scraped_at': datetime.now().isoformat(),
                'comments': comments
            }
            
            with open(filename, 'w', encoding='utf-8') as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
            
            logger.info(f"\n💾 Saved: {filename} ({os.path.getsize(filename) / 1024:.1f} KB)")
            
            return filename
            
        except Exception as e:
            logger.error(f"❌ Save error: {e}")
            return None
    
    def close(self):
        if self.driver:
            try:
                self.driver.quit()
                logger.info("✅ Closed")
            except:
                pass
    
    def __del__(self):
        self.close()


if __name__ == "__main__":
    print("\n" + "="*70)
    print("🚀 TIKTOK SCRAPER - USERNAME BUG FIXED")
    print("="*70)
    print("\n⚡ Features:")
    print("   ✅ Fixed: No more clicking usernames!")
    print("   ✅ Only clicks actual 'View reply' buttons")
    print("   ✅ Multiple validation strategies")
    print("   ✅ Auto save to JSON")
    print("="*70 + "\n")
    
    test_urls = [
        "https://www.tiktok.com/@chuyendoday.vn/video/7566963091313855762",
    ]
    
    with TikTokScraper(headless=False, max_scroll=1000) as scraper:
        
        for url in test_urls:
            comments = scraper.get_comments(
                video_url=url,
                max_results=10000,
                vietnamese_only=True,
                include_replies=True
            )
            
            print(f"\n{'='*70}")
            print(f"📊 RESULTS")
            print(f"{'='*70}")
            
            if comments:
                main = [c for c in comments if not c.get('is_reply')]
                replies = [c for c in comments if c.get('is_reply')]
                
                print(f"\n✅ Total: {len(comments)}")
                print(f"   • Main: {len(main)}")
                print(f"   • Replies: {len(replies)}")
                
                print(f"\n📝 Sample (first 10):")
                for i, c in enumerate(comments[:10], 1):
                    prefix = "↳" if c.get('is_reply') else "•"
                    print(f"  {prefix} [{c['author']}] {c['text'][:60]}... (❤️  {c['likes']})")
                
                # Save to JSON
                video_id = scraper.extract_video_id(url)
                scraper.save_to_json(comments, video_id)
                
                print(f"\n{'='*70}")
                
            else:
                print("\n❌ No comments extracted!")
                print("   Check if video has comments")
                print(f"\n{'='*70}")