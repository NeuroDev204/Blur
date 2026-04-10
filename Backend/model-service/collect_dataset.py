"""
Thu Thập Dataset từ YouTube + TikTok - OPTIMIZED VERSION
✅ Async/await cho hiệu năng cao
✅ PhoBERT cho NLP labeling tiếng Việt
✅ Auto retry với exponential backoff
✅ Full pagination & deduplication
✅ Clean code với modular structure
"""

import asyncio
import sys
import os
import random
from typing import List, Dict, Optional
from datetime import datetime

# Add parent directory to path for imports
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from scraper.scrapers import YouTubeScraper, TikTokScraper
from scraper.labelers import NLPLabeler
from scraper.validators import CommentValidator
from scraper.exporters import JSONExporter
from scraper.models import Comment
from scraper.utils.logger import (
    get_logger, print_header, print_subheader,
    print_success, print_error, print_warning, print_info, print_stat,
    Colors
)

logger = get_logger(__name__)


# ============================================================================
# CẤU HÌNH
# ============================================================================

YOUTUBE_API_KEY = "AIzaSyCC8sYNhEDpvuqg7l64yWkDTG4AvevGIJw"

# ============================================================================
# YOUTUBE VIDEOS
# ============================================================================
YOUTUBE_VIDEO_URLS = [
    "https://www.youtube.com/watch?v=Oako9LG-uVI",  # MixiGaming
    "https://www.youtube.com/watch?v=OQHgiYq2LZ8",
    "https://www.youtube.com/watch?v=-kFLvlIjSDg",
    "https://www.youtube.com/watch?v=izLqGIoZ8EQ",
    "https://www.youtube.com/watch?v=AXIE9x2qi5M",
    "https://www.youtube.com/watch?v=42hqzXKYvkU",
    "https://www.youtube.com/watch?v=bIt6B7IhwLA",
    "https://www.youtube.com/watch?v=ysRkBCL_ucw",
    "https://www.youtube.com/watch?v=EUR3Fmwn1R0",
    "https://www.youtube.com/watch?v=psufCR1faYA",
    "https://www.youtube.com/watch?v=oZpJYDAvhTk",
    "https://www.youtube.com/watch?v=YAnw9dHY7i4",
    "https://www.youtube.com/watch?v=_nhLWUxH4pE",
    "https://www.youtube.com/watch?v=8-Sm7mGSiqw",
    "https://www.youtube.com/watch?v=wjr-6pDTxBE",
    "https://www.youtube.com/watch?v=IdjNKEs-ePs",
    "https://www.youtube.com/watch?v=eKkwnRxrNNU",
    "https://www.youtube.com/watch?v=j8sOCw5_qSA",
    "https://www.youtube.com/watch?v=xp7RtW-kCGA",
    "https://www.youtube.com/watch?v=qEvJE9VgBRU",
    "https://www.youtube.com/watch?v=ogTh68XH87E",
    "https://www.youtube.com/watch?v=I5OLWtcx3sY",
    "https://www.youtube.com/watch?v=sqEOWX0lbP4",
    "https://www.youtube.com/watch?v=9sDtGbCE3ng",
    "https://www.youtube.com/watch?v=hAp8SKcE_5k",
    "https://www.youtube.com/watch?v=95m4Bg0GkAQ",
    "https://www.youtube.com/watch?v=JrYV2gQ8z8Ui",
    "https://www.youtube.com/watch?v=6HZuUiDEjkM",
    "https://www.youtube.com/watch?v=RN4KKkv5aQ8",
    "https://www.youtube.com/watch?v=JyPHIlw-tvY",
    "https://www.youtube.com/watch?v=v7LnYCE_Uzw",
    "https://www.youtube.com/watch?v=HNZVfS2XlYE",
    "https://www.youtube.com/watch?v=DTLDfo5bnCQ",
    "https://www.youtube.com/watch?v=NfpoyDRMfxM",
    "https://www.youtube.com/watch?v=TcCmJyfQnYI",
    "https://www.youtube.com/watch?v=3G-2iGFfFFs",
    "https://www.youtube.com/watch?v=5NQRmu-YCII",
    "https://www.youtube.com/watch?v=wp016eg_Vpc",
    "https://www.youtube.com/watch?v=XEEwtFHiRoY",
    "https://www.youtube.com/watch?v=SS9PShSwAV4",
    "https://www.youtube.com/watch?v=1H23Han8omo",
    "https://www.youtube.com/watch?v=OIMoOhy7hBE",
    "https://www.youtube.com/watch?v=I-UJc0w5N_w",
    "https://www.youtube.com/watch?v=gylEJuzv1Sw",
    "https://www.youtube.com/watch?v=NbNBYA8Y41U",
    "https://www.youtube.com/watch?v=91M8kqeFn90",
    "https://www.youtube.com/watch?v=YbJAKamEH4o",
    "https://www.youtube.com/watch?v=_9bY3oZ2Evs",
    "https://www.youtube.com/watch?v=kqHgk9rdifw",
    "https://www.youtube.com/watch?v=x9qrC9tBokk",
    "https://www.youtube.com/watch?v=OYSjzloCZmo",
    "https://www.youtube.com/watch?v=VZ6PvJ-zKCA",
    "https://www.youtube.com/watch?v=RXYnNO6OXSw",
    "https://www.youtube.com/watch?v=KaF-K_ZgSzk",
    "https://www.youtube.com/watch?v=C7KhGcePa20",
    "https://www.youtube.com/watch?v=51X4lY-YFq4",
    "https://www.youtube.com/watch?v=rYom5pt_svg",
    "https://www.youtube.com/watch?v=8oMtQ-y9PEU",
    "https://www.youtube.com/watch?v=TpyfK1G1rzc",
    "https://www.youtube.com/watch?v=26KL3hBFuYo",
    "https://www.youtube.com/watch?v=Oako9LG-uVI",
    "https://www.youtube.com/watch?v=izLqGIoZ8EQ",
    "https://www.youtube.com/watch?v=rpe7SfxRckU",
    "https://www.youtube.com/watch?v=AXIE9x2qi5M",
    "https://www.youtube.com/watch?v=42hqzXKYvkU",
    "https://www.youtube.com/watch?v=JxH6HIJinlI",
    "https://www.youtube.com/watch?v=-kFLvlIjSDg",
    "https://www.youtube.com/watch?v=9ySJ5skhxo0",
    "https://www.youtube.com/watch?v=1g9SZ50ahTg",
    "https://www.youtube.com/watch?v=jk7LbXUpmz0",
    "https://www.youtube.com/watch?v=HOGfrOK9a84",
    "https://www.youtube.com/watch?v=EHNmr6OVvWI",
]

# ============================================================================
# TIKTOK VIDEOS
# ============================================================================
TIKTOK_VIDEO_URLS = [
    "https://www.tiktok.com/@trabongchubbyneh/video/7566636064744492309",
    "https://www.tiktok.com/@trabongchubbyneh/video/7557428636266138897",
    "https://www.tiktok.com/@trabongchubbyneh/video/7560019314384964880",
    "https://www.tiktok.com/@trabongchubbyneh/video/7549570684603665665",
    "https://www.tiktok.com/@trabongchubbyneh/video/7548468569185537298",
    "https://www.tiktok.com/@layraytaivip/video/7551816387870690567",
    "https://www.tiktok.com/@doradora2112_/video/7566472026605620500",
    "https://www.tiktok.com/@we25.vn/video/7552373803276438791",
    "https://www.tiktok.com/@bachkhoatoanlong/video/7563330436248669460",
    "https://www.tiktok.com/@haidenis9x/video/7568879120692546837",
    "https://www.tiktok.com/@ttnhu8/video/7569485163298589970",
    "https://www.tiktok.com/@chilama_gfx_animezz/video/7549748983959293202",
    "https://www.tiktok.com/@vietnamhealthnews/video/7563633491913493780",
    "https://www.tiktok.com/@ngmhieu266/video/7561286412340006165",
    "https://www.tiktok.com/@vietnamexpress.diary/video/7548089579614588178",
    "https://www.tiktok.com/@themlon1999/video/7569861284037020949",
    "https://www.tiktok.com/@brave_traveler/video/7550590755882962196",
    "https://www.tiktok.com/@nguyenthivinh61169/video/7569548031561977108",
    "https://www.tiktok.com/@ysan1006/video/7569444081655368978",
    "https://www.tiktok.com/@_tma25/video/7569560441144347911",
    "https://www.tiktok.com/@_ltvkhongsuyy/video/7569603674624888084",
    "https://www.tiktok.com/@tiktok.com.sunchan/video/7553786728570440976",
    "https://www.tiktok.com/@shuzennn/video/7569189239871622416",
    "https://www.tiktok.com/@tintucnews.vn/video/7568835590012144903",
    "https://www.tiktok.com/@twn.05/video/7566848019463769345",
    "https://www.tiktok.com/@zenfi629/video/7549405435657211158",
    "https://www.tiktok.com/@9th_212/video/7569872463375518983",
    "https://www.tiktok.com/@phongclone1111/video/7569215643002686727",
    "https://www.tiktok.com/@vtin247/video/7569898370295254293",
    "https://www.tiktok.com/@3amdiaryvn/video/7568809489390963986",
    "https://www.tiktok.com/@thiku113/video/7569849854243917074",
    "https://www.tiktok.com/@myanhcuaanhoi/video/7566993450411871496",
    "https://www.tiktok.com/@doanhnhansena/video/7569842922263203092",
    "https://www.tiktok.com/@dikavision_as/video/7553834881990626572",
    "https://www.tiktok.com/@tquynbith/video/7570999481357962517",
    "https://www.tiktok.com/@tamanan177/video/7569530870147599634",
    "https://www.tiktok.com/@pearsclip/video/7568803182181207313",
    "https://www.tiktok.com/@atbichbencat/video/7569879336669465864",
    "https://www.tiktok.com/@dr_wafakhan14/video/7569291722929343774",
    "https://www.tiktok.com/@icecreamtina0/video/7566947787787619598",
    "https://www.tiktok.com/@quanhaylo/video/7559253717686504712",
    "https://www.tiktok.com/@hitomi_vietnam/video/7569277842836491541",
    "https://www.tiktok.com/@minbn11/video/7532062580785892616",
    "https://www.tiktok.com/@theanh28trending/video/7532805458260757768",
    "https://www.tiktok.com/@vivumuasam/video/7569586311967919367",
    "https://www.tiktok.com/@tony.beer2/video/7567307496063274254",
    "https://www.tiktok.com/@tuanhuyenvlog/video/7558874845858090248",
    "https://www.tiktok.com/@thaidanchuyendoi/video/7565137723858914567",
    "https://www.tiktok.com/@mauaothanthuong/video/7564243523625700616",
    "https://www.tiktok.com/@lamlaicuocdoi00112/video/7569543453680340232",
    "https://www.tiktok.com/@lamranthua/video/7566901908019907847",
    "https://www.tiktok.com/@hi258100/video/7563320607731977479",
    "https://www.tiktok.com/@mylanoffical/video/7569832037679418642",
    "https://www.tiktok.com/@lethikhanhhuyen2004/video/7566274876303052040",
    "https://www.tiktok.com/@cbd.toilet_/video/7565796504997301522",
    "https://www.tiktok.com/@blogtamsu.nghenghiep/video/7568331215418314005",
    "https://www.tiktok.com/@thaytunecon_2110/video/7569149984495455496",
    "https://www.tiktok.com/@u50thidasao.84/video/7564961365211811090",
    "https://www.tiktok.com/@nguyenthien0605/video/7569209746687888648",
    "https://www.tiktok.com/@mynameisnives/video/7565902771891801366",
    "https://www.tiktok.com/@hp6423/video/7568391662355942663",
    "https://www.tiktok.com/@huonghinne/video/7569180390204771592",
    "https://www.tiktok.com/@banhtrangchinhchutpdai/video/7562201450152054024",
    "https://www.tiktok.com/@waanimareal/video/7565976101013507350",
    "https://www.tiktok.com/@sani.jhone/video/7562797126577704214",
    "https://www.tiktok.com/@theanh28funfact/video/7568800049849781522",
    "https://www.tiktok.com/@haomean/video/7569183036147780880",
    "https://www.tiktok.com/@theanh28entertainment/video/7569068353155861767",
    "https://www.tiktok.com/@kiukiu9486/video/7560296865413991700",
    "https://www.tiktok.com/@beadanttas01/video/7558687391347657995",
    "https://www.tiktok.com/@binhtubi_/video/7564771595793747218",
]


# ============================================================================
# SCRAPER FUNCTIONS
# ============================================================================

async def collect_youtube_comments(
    api_key: str,
    video_urls: List[str],
    max_per_video: int = 500
) -> List[Comment]:
    """Thu thập comments từ YouTube với async"""
    
    print_header("📺 THU THẬP TỪ YOUTUBE", Colors.BRIGHT_RED)
    print_stat("Videos", len(video_urls))
    print_stat("Max per video", max_per_video)
    
    all_comments: List[Comment] = []
    
    async with YouTubeScraper(api_key=api_key) as scraper:
        for i, url in enumerate(video_urls, 1):
            try:
                video_id = scraper.extract_video_id(url)
                print(f"\n{Colors.BRIGHT_YELLOW}[{i}/{len(video_urls)}]{Colors.RESET} "
                      f"{Colors.CYAN}Video: {video_id}{Colors.RESET}")
                
                # Get video info
                info = await scraper.get_video_info(video_id)
                if info:
                    print_stat("📹 Title", info['title'][:60])
                    print_stat("👤 Channel", info['channel'])
                
                # Get comments
                comments = await scraper.get_comments(
                    video_url=url,
                    max_results=max_per_video,
                    include_replies=True,
                    vietnamese_only=True
                )
                
                if comments:
                    main_count = sum(1 for c in comments if c.parent_id is None)
                    reply_count = len(comments) - main_count
                    print_success(f"Collected: {len(comments)} ({main_count} main, {reply_count} replies)")
                    all_comments.extend(comments)
                else:
                    print_warning("No comments found")
                
            except Exception as e:
                print_error(f"Error: {e}")
                continue
    
    print(f"\n{Colors.BRIGHT_GREEN}✅ YouTube total: {len(all_comments)} comments{Colors.RESET}")
    return all_comments


async def collect_tiktok_comments(
    video_urls: List[str],
    max_per_video: int = 3000,
    headless: bool = False
) -> List[Comment]:
    """Thu thập comments từ TikTok"""
    
    print_header("🎵 THU THẬP TỪ TIKTOK", Colors.BRIGHT_MAGENTA)
    print_stat("Videos", len(video_urls))
    print_stat("Max per video", max_per_video)
    print_info("Lần đầu: Cần login thủ công")
    print_info("Lần sau: Tự động login!")
    
    all_comments: List[Comment] = []
    scraper = TikTokScraper(headless=headless, max_scroll=10000)  # Increased for more comments
    
    try:
        for i, url in enumerate(video_urls, 1):
            try:
                video_id = scraper.extract_video_id(url)
                print(f"\n{Colors.BRIGHT_YELLOW}[{i}/{len(video_urls)}]{Colors.RESET} "
                      f"{Colors.MAGENTA}Video: {video_id}{Colors.RESET}")
                
                comments = await scraper.get_comments(
                    video_url=url,
                    max_results=max_per_video,
                    include_replies=True,
                    vietnamese_only=True
                )
                
                if comments:
                    main_count = sum(1 for c in comments if c.parent_id is None)
                    reply_count = len(comments) - main_count
                    print_stat("💬 Main", main_count)
                    print_stat("↳  Replies", reply_count)
                    print_success(f"Collected: {len(comments)} total")
                    all_comments.extend(comments)
                else:
                    print_warning("No comments found")
                
                # Delay between videos
                delay = random.uniform(3, 6)
                print(f"{Colors.BRIGHT_BLACK}   ⏳ Waiting {delay:.1f}s...{Colors.RESET}")
                await asyncio.sleep(delay)
                
            except Exception as e:
                print_error(f"Error: {e}")
                await asyncio.sleep(10)
                continue
    finally:
        scraper.close()
    
    print(f"\n{Colors.BRIGHT_MAGENTA}✅ TikTok total: {len(all_comments)} comments{Colors.RESET}")
    return all_comments


def prepare_training_dataset(
    comments: List[Comment],
    balance: bool = True,
    min_samples: int = 10
) -> List[Dict]:
    """Chuẩn bị dataset cho training"""
    
    print_header("⚙️ CHUẨN BỊ TRAINING DATASET", Colors.BRIGHT_BLUE)
    
    if not comments:
        print_error("No comments to process!")
        return []
    
    # Separate toxic and clean
    toxic_comments = [c for c in comments if c.label and c.label.toxic_level != 'low']
    clean_comments = [c for c in comments if c.label and c.label.toxic_level == 'low']
    
    print(f"{Colors.BRIGHT_YELLOW}📊 Raw data:{Colors.RESET}")
    print_stat("Toxic", len(toxic_comments), Colors.BRIGHT_RED)
    print_stat("Clean", len(clean_comments), Colors.BRIGHT_GREEN)
    
    # Handle case: not enough samples
    if len(toxic_comments) < min_samples and len(clean_comments) < min_samples:
        print_warning(f"Very few samples! Continuing anyway...")
        balance = False
    
    # Balance dataset
    if balance and len(toxic_comments) > 0 and len(clean_comments) > 0:
        min_count = min(len(toxic_comments), len(clean_comments))
        if min_count >= min_samples:
            random.shuffle(toxic_comments)
            random.shuffle(clean_comments)
            toxic_comments = toxic_comments[:min_count]
            clean_comments = clean_comments[:min_count]
            print_success(f"Balanced dataset: {min_count} samples per class")
        else:
            print_warning(f"Not enough samples to balance ({min_count} < {min_samples})")
            print_info("Using all available samples")
    
    # Create training samples
    dataset = []
    
    # Add toxic samples (label = 1)
    for comment in toxic_comments:
        dataset.append({
            'text': comment.content,
            'label': 1,
            'toxicity_score': comment.label.toxic_score if comment.label else 1.0,
            'sentiment': comment.label.sentiment if comment.label else 'neutral',
            'topic': comment.label.topic if comment.label else 'khac',
            'author': comment.user,
            'source': comment.platform,
            'is_reply': comment.parent_id is not None,
            'likes': comment.like_count
        })
    
    # Add clean samples (label = 0)
    for comment in clean_comments:
        dataset.append({
            'text': comment.content,
            'label': 0,
            'toxicity_score': comment.label.toxic_score if comment.label else 0.0,
            'sentiment': comment.label.sentiment if comment.label else 'neutral',
            'topic': comment.label.topic if comment.label else 'khac',
            'author': comment.user,
            'source': comment.platform,
            'is_reply': comment.parent_id is not None,
            'likes': comment.like_count
        })
    
    print(f"\n{Colors.BRIGHT_GREEN}✅ Final dataset: {len(dataset)} samples{Colors.RESET}")
    print_stat("Toxic (label=1)", len(toxic_comments), Colors.BRIGHT_RED)
    print_stat("Clean (label=0)", len(clean_comments), Colors.BRIGHT_GREEN)
    
    return dataset


def print_dataset_stats(comments: List[Comment]):
    """In thống kê chi tiết với màu sắc"""
    
    print_header("📊 DATASET STATISTICS", Colors.BRIGHT_GREEN)
    
    total = len(comments)
    if total == 0:
        print_error("No comments to analyze")
        return
    
    # Toxic/Clean breakdown
    toxic_count = sum(1 for c in comments if c.label and c.label.toxic_level != 'low')
    clean_count = total - toxic_count
    
    print(f"{Colors.BRIGHT_YELLOW}📈 Overall:{Colors.RESET}")
    print_stat("Total samples", total, Colors.BRIGHT_WHITE)
    print_stat("Toxic", f"{toxic_count} ({toxic_count/total*100:.1f}%)", Colors.BRIGHT_RED)
    print_stat("Clean", f"{clean_count} ({clean_count/total*100:.1f}%)", Colors.BRIGHT_GREEN)
    
    # Source distribution
    from collections import Counter
    source_counts = Counter(c.platform for c in comments)
    
    print(f"\n{Colors.BRIGHT_YELLOW}📱 Source distribution:{Colors.RESET}")
    for source, count in source_counts.items():
        color = Colors.BRIGHT_RED if source == 'youtube' else Colors.BRIGHT_MAGENTA
        print_stat(source.capitalize(), f"{count} ({count/total*100:.1f}%)", color)
    
    # Reply vs main
    reply_count = sum(1 for c in comments if c.parent_id is not None)
    main_count = total - reply_count
    
    print(f"\n{Colors.BRIGHT_YELLOW}💬 Comment types:{Colors.RESET}")
    print_stat("Main comments", f"{main_count} ({main_count/total*100:.1f}%)", Colors.BRIGHT_CYAN)
    print_stat("Replies", f"{reply_count} ({reply_count/total*100:.1f}%)", Colors.BRIGHT_GREEN)
    
    # Sentiment distribution
    sentiment_counts = Counter(c.label.sentiment for c in comments if c.label)
    
    print(f"\n{Colors.BRIGHT_YELLOW}😊 Sentiment distribution:{Colors.RESET}")
    for sentiment, count in sentiment_counts.most_common():
        color = Colors.BRIGHT_GREEN if sentiment == 'positive' else (
            Colors.BRIGHT_RED if sentiment == 'negative' else Colors.BRIGHT_WHITE
        )
        print_stat(sentiment.capitalize(), f"{count} ({count/total*100:.1f}%)", color)
    
    # Topic distribution
    topic_counts = Counter(c.label.topic for c in comments if c.label)
    
    print(f"\n{Colors.BRIGHT_YELLOW}📁 Topic distribution:{Colors.RESET}")
    for topic, count in topic_counts.most_common():
        print_stat(topic, f"{count} ({count/total*100:.1f}%)", Colors.BRIGHT_CYAN)
    
    # Sample comments
    print(f"\n{Colors.BRIGHT_YELLOW}📝 Sample toxic comments:{Colors.RESET}")
    toxic_samples = [c for c in comments if c.label and c.label.toxic_level != 'low'][:5]
    for i, sample in enumerate(toxic_samples, 1):
        source_emoji = "📺" if sample.platform == 'youtube' else "🎵"
        print(f"{Colors.BRIGHT_RED}   {i}. {source_emoji} [{sample.user}] {sample.content[:60]}...{Colors.RESET}")
    
    print(f"\n{Colors.BRIGHT_YELLOW}📝 Sample clean comments:{Colors.RESET}")
    clean_samples = [c for c in comments if c.label and c.label.toxic_level == 'low'][:5]
    for i, sample in enumerate(clean_samples, 1):
        source_emoji = "📺" if sample.platform == 'youtube' else "🎵"
        print(f"{Colors.BRIGHT_GREEN}   {i}. {source_emoji} [{sample.user}] {sample.content[:60]}...{Colors.RESET}")


# ============================================================================
# MAIN
# ============================================================================

async def main():
    """Main entry point"""
    
    print_header("🚀 BẮT ĐẦU THU THẬP DATASET - OPTIMIZED VERSION", Colors.BRIGHT_CYAN)
    print_info("Sử dụng PhoBERT cho NLP labeling tiếng Việt")
    print()
    
    print_stat("📺 YouTube videos", len(YOUTUBE_VIDEO_URLS), Colors.BRIGHT_RED)
    print_stat("🎵 TikTok videos", len(TIKTOK_VIDEO_URLS), Colors.BRIGHT_MAGENTA)
    
    all_comments: List[Comment] = []
    
    # =========================================================================
    # PHASE 1: THU THẬP YOUTUBE
    # =========================================================================
    if YOUTUBE_VIDEO_URLS:
        print_subheader("PHASE 1: YOUTUBE", Colors.BRIGHT_RED)
        
        youtube_comments = await collect_youtube_comments(
            api_key=YOUTUBE_API_KEY,
            video_urls=YOUTUBE_VIDEO_URLS,
            max_per_video=10000
        )
        all_comments.extend(youtube_comments)
        print_success(f"YouTube done: {len(youtube_comments)} comments")
    
    # =========================================================================
    # PHASE 2: THU THẬP TIKTOK
    # =========================================================================
    if TIKTOK_VIDEO_URLS:
        print_subheader("PHASE 2: TIKTOK", Colors.BRIGHT_MAGENTA)
        
        tiktok_comments = await collect_tiktok_comments(
            video_urls=TIKTOK_VIDEO_URLS,
            max_per_video=3000,
            headless=False  # Set True for production
        )
        all_comments.extend(tiktok_comments)
        print_success(f"TikTok done: {len(tiktok_comments)} comments")
    
    # =========================================================================
    # KIỂM TRA DỮ LIỆU
    # =========================================================================
    print_header("✅ COLLECTION COMPLETE", Colors.BRIGHT_GREEN)
    print_stat("Total raw comments", len(all_comments), Colors.BRIGHT_YELLOW)
    
    if not all_comments:
        print_error("No comments collected!")
        print_info("Suggestions:")
        print("   1. Add more video URLs")
        print("   2. Check if you logged in to TikTok")
        print("   3. Check API key for YouTube")
        return
    
    # =========================================================================
    # VALIDATION
    # =========================================================================
    print_subheader("🔍 VALIDATING DATA", Colors.BRIGHT_BLUE)
    
    validator = CommentValidator(vietnamese_only=True)
    valid_comments, _ = validator.validate_batch(all_comments)
    
    print_stat("Valid comments", len(valid_comments))
    print_stat("Removed", len(all_comments) - len(valid_comments))
    
    # =========================================================================
    # NLP LABELING (PhoBERT)
    # =========================================================================
    print_subheader("🏷️  LABELING WITH PhoBERT", Colors.BRIGHT_YELLOW)
    print_info("Loading PhoBERT model... (may take a while on first run)")
    
    # Sử dụng PhoBERT cho sentiment (use_ml_sentiment=True)
    labeler = NLPLabeler(use_ml_sentiment=True)
    labeled_comments = labeler.label_comments_batch(valid_comments)
    
    # Stats
    toxic_count = sum(1 for c in labeled_comments if c.label and c.label.toxic_level != 'low')
    print_stat("Toxic comments", f"{toxic_count} ({toxic_count/len(labeled_comments)*100:.1f}%)")
    
    # =========================================================================
    # EXPORT
    # =========================================================================
    print_subheader("💾 EXPORTING DATA", Colors.BRIGHT_GREEN)
    
    exporter = JSONExporter(output_dir='data')
    
    # Determine platform
    yt_count = sum(1 for c in labeled_comments if c.platform == 'youtube')
    tt_count = sum(1 for c in labeled_comments if c.platform == 'tiktok')
    platform = 'mixed' if yt_count > 0 and tt_count > 0 else ('youtube' if yt_count > 0 else 'tiktok')
    
    # Export JSON
    json_path = exporter.export_json(labeled_comments, platform=platform)
    print_success(f"Saved JSON: {json_path}")
    
    # Export CSV
    csv_path = exporter.export_csv(labeled_comments)
    print_success(f"Saved CSV: {csv_path}")
    
    # Export training data
    training_paths = exporter.export_training_data(labeled_comments, label_type='toxic')
    if training_paths:
        print_success(f"Saved train: {training_paths.get('train', 'N/A')}")
        print_success(f"Saved test: {training_paths.get('test', 'N/A')}")
    
    # =========================================================================
    # STATISTICS
    # =========================================================================
    print_dataset_stats(labeled_comments)
    
    # =========================================================================
    # SUMMARY
    # =========================================================================
    print_header("✅ DATASET COLLECTION COMPLETE!", Colors.BRIGHT_GREEN)
    
    print(f"{Colors.BRIGHT_YELLOW}📁 Files saved:{Colors.RESET}")
    print_stat("JSON", json_path, Colors.BRIGHT_CYAN)
    print_stat("CSV", csv_path, Colors.BRIGHT_BLUE)
    
    if len(labeled_comments) >= 100:
        print(f"\n{Colors.BRIGHT_GREEN}🚀 Next steps:{Colors.RESET}")
        print(f"{Colors.BRIGHT_CYAN}   1. Review dataset quality{Colors.RESET}")
        print(f"{Colors.BRIGHT_CYAN}   2. Train model using the dataset{Colors.RESET}")
        print(f"{Colors.BRIGHT_CYAN}   3. Evaluate model performance{Colors.RESET}")
    else:
        print(f"\n{Colors.BRIGHT_YELLOW}⚠️  Dataset is small ({len(labeled_comments)} samples){Colors.RESET}")
        print(f"\n{Colors.BRIGHT_BLUE}💡 Recommendations:{Colors.RESET}")
        print(f"{Colors.BRIGHT_WHITE}   1. Add more videos (aim for 50-100 videos){Colors.RESET}")
        print(f"{Colors.BRIGHT_WHITE}   2. Mix different content types{Colors.RESET}")
        print(f"{Colors.BRIGHT_WHITE}   3. Target: 3000-10000 samples for good model{Colors.RESET}")
    
    print(f"\n{Colors.BRIGHT_MAGENTA}{'='*70}{Colors.RESET}\n")


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\n⚠️  Interrupted by user")
    except Exception as e:
        print(f"\n❌ Error: {e}")
        import traceback
        traceback.print_exc()
