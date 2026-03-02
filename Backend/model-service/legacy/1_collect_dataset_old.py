"""
Thu Thập Dataset từ YouTube + TikTok - FULLY INTEGRATED
✅ Tích hợp TikTok scraper đã fix
✅ Fixed toxic detection
✅ Better reply extraction
✅ Resource optimization
"""

from youtube_comment_scraper import YouTubeCommentScraper, extract_video_id as yt_extract_id
from tiktok_scraper_fixed import TikTokScraper
from toxic_detector_fixed import VietnameseToxicDetector
import json
from datetime import datetime
from typing import List, Dict
import time
import random
import logging
import sys
import os

# ============================================================================
# ANSI COLOR CODES
# ============================================================================
class Colors:
    """ANSI color codes"""
    RESET = '\033[0m'
    BOLD = '\033[1m'
    
    BLACK = '\033[30m'
    RED = '\033[31m'
    GREEN = '\033[32m'
    YELLOW = '\033[33m'
    BLUE = '\033[34m'
    MAGENTA = '\033[35m'
    CYAN = '\033[36m'
    WHITE = '\033[37m'
    
    BRIGHT_BLACK = '\033[90m'
    BRIGHT_RED = '\033[91m'
    BRIGHT_GREEN = '\033[92m'
    BRIGHT_YELLOW = '\033[93m'
    BRIGHT_BLUE = '\033[94m'
    BRIGHT_MAGENTA = '\033[95m'
    BRIGHT_CYAN = '\033[96m'
    BRIGHT_WHITE = '\033[97m'
    
    BG_RED = '\033[41m'
    BG_GREEN = '\033[42m'
    BG_YELLOW = '\033[43m'
    BG_BLUE = '\033[44m'
    BG_MAGENTA = '\033[45m'
    BG_CYAN = '\033[46m'


# ============================================================================
# CUSTOM COLORED LOGGER
# ============================================================================
class ColoredFormatter(logging.Formatter):
    """Custom formatter with colors"""
    
    FORMATS = {
        logging.DEBUG: Colors.BRIGHT_BLACK + "%(asctime)s [DEBUG] %(message)s" + Colors.RESET,
        logging.INFO: Colors.BRIGHT_CYAN + "%(asctime)s [INFO] %(message)s" + Colors.RESET,
        logging.WARNING: Colors.BRIGHT_YELLOW + "%(asctime)s [WARNING] %(message)s" + Colors.RESET,
        logging.ERROR: Colors.BRIGHT_RED + "%(asctime)s [ERROR] %(message)s" + Colors.RESET,
        logging.CRITICAL: Colors.BG_RED + Colors.WHITE + "%(asctime)s [CRITICAL] %(message)s" + Colors.RESET,
    }
    
    def format(self, record):
        log_fmt = self.FORMATS.get(record.levelno)
        formatter = logging.Formatter(log_fmt, datefmt='%H:%M:%S')
        return formatter.format(record)


# Setup logger
logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)

handler = logging.StreamHandler(sys.stdout)
handler.setFormatter(ColoredFormatter())
logger.addHandler(handler)


# ============================================================================
# HELPER FUNCTIONS
# ============================================================================
def print_header(text: str, color=Colors.BRIGHT_MAGENTA):
    """Print a fancy header"""
    print(f"\n{color}{'='*70}")
    print(f"{text:^70}")
    print(f"{'='*70}{Colors.RESET}\n")


def print_subheader(text: str, color=Colors.BRIGHT_CYAN):
    """Print a subheader"""
    print(f"\n{color}{'─'*70}")
    print(f"  {text}")
    print(f"{'─'*70}{Colors.RESET}")


def print_success(text: str):
    """Print success message"""
    print(f"{Colors.BRIGHT_GREEN}✅ {text}{Colors.RESET}")


def print_error(text: str):
    """Print error message"""
    print(f"{Colors.BRIGHT_RED}❌ {text}{Colors.RESET}")


def print_warning(text: str):
    """Print warning message"""
    print(f"{Colors.BRIGHT_YELLOW}⚠️  {text}{Colors.RESET}")


def print_info(text: str):
    """Print info message"""
    print(f"{Colors.BRIGHT_BLUE}ℹ️  {text}{Colors.RESET}")


def print_stat(label: str, value, color=Colors.BRIGHT_WHITE):
    """Print a stat line"""
    print(f"{Colors.BRIGHT_BLACK}   {label}:{Colors.RESET} {color}{value}{Colors.RESET}")


# ============================================================================
# DATA COLLECTION FUNCTIONS
# ============================================================================
def collect_dataset_from_youtube(
        api_key: str,
        video_urls: List[str],
        max_per_video: int = 500
) -> List[Dict]:
    """Thu thập dataset từ YouTube"""
    scraper = YouTubeCommentScraper(api_key)
    detector = VietnameseToxicDetector()
    all_comments = []

    print_header("📺 THU THẬP TỪ YOUTUBE", Colors.BRIGHT_RED)

    for i, url in enumerate(video_urls, 1):
        video_id = yt_extract_id(url)
        
        print(f"\n{Colors.BRIGHT_YELLOW}[{i}/{len(video_urls)}]{Colors.RESET} "
              f"{Colors.CYAN}Video: {video_id}{Colors.RESET}")

        try:
            video_info = scraper.get_video_info(video_id)
            if video_info:
                print_stat("📹 Title", video_info['title'][:60], Colors.WHITE)
                print_stat("👤 Channel", video_info['channel'], Colors.BRIGHT_CYAN)

            comments = scraper.get_video_comments(
                video_id=video_id,
                max_results=max_per_video,
                include_replies=True,
                vietnamese_only=True
            )

            if not comments:
                print_warning("No comments found")
                continue

            # Label với FIXED detector
            labeled = detector.label_comments_batch(comments)
            toxic_count = sum(1 for c in labeled if c.get('is_toxic', False))
            
            print_success(f"Collected: {len(labeled)} comments "
                         f"({Colors.BRIGHT_RED}{toxic_count} toxic{Colors.RESET})")
            
            # Show toxic samples
            toxic_samples = [c for c in labeled if c.get('is_toxic', False)][:3]
            if toxic_samples:
                print(f"{Colors.BRIGHT_RED}   Sample toxic:{Colors.RESET}")
                for sample in toxic_samples:
                    print(f"{Colors.BRIGHT_RED}      • {sample['text'][:50]}...{Colors.RESET}")
            
            all_comments.extend(labeled)

        except Exception as e:
            print_error(f"Error: {str(e)}")
            continue

    return all_comments


def collect_dataset_from_tiktok(
        video_urls: List[str],
        max_per_video: int = 3000,
        headless: bool = False
) -> List[Dict]:
    """
    Thu thập dataset từ TikTok - INTEGRATED với scraper mới
    ✅ Sử dụng TikTokScraper đã fix
    """
    detector = VietnameseToxicDetector()
    all_comments = []

    print_header("🎵 THU THẬP TỪ TIKTOK - FIXED SCRAPER", Colors.BRIGHT_MAGENTA)
    
    print_info("Lần đầu: Cần login thủ công")
    print_info("Lần sau: Tự động login!")
    print()

    # ✅ Sử dụng TikTokScraper mới (đã fix)
    with TikTokScraper(headless=headless, max_scroll=100) as scraper:
        
        for i, url in enumerate(video_urls, 1):
            video_id = scraper.extract_video_id(url)
            
            print(f"\n{Colors.BRIGHT_YELLOW}[{i}/{len(video_urls)}]{Colors.RESET} "
                  f"{Colors.MAGENTA}Video: {video_id}{Colors.RESET}")

            try:
                # ✅ Get comments + replies (FIXED)
                comments = scraper.get_comments(
                    video_url=url,
                    max_results=max_per_video,
                    vietnamese_only=True,
                    include_replies=True  # ✅ NOW WORKS!
                )

                if not comments:
                    print_warning("No comments found")
                    time.sleep(5)
                    continue

                # Stats
                main_comments = [c for c in comments if not c.get('is_reply')]
                reply_comments = [c for c in comments if c.get('is_reply')]
                
                print_stat("💬 Main", len(main_comments), Colors.BRIGHT_CYAN)
                print_stat("↳  Replies", len(reply_comments), Colors.BRIGHT_GREEN)

                # ✅ Label toxic với FIXED detector
                labeled = detector.label_comments_batch(comments)
                toxic_count = sum(1 for c in labeled if c.get('is_toxic', False))
                
                print_success(f"Collected: {len(labeled)} total "
                            f"({Colors.BRIGHT_RED}{toxic_count} toxic{Colors.RESET})")
                
                # Show toxic samples
                toxic_samples = [c for c in labeled if c.get('is_toxic', False)][:3]
                if toxic_samples:
                    print(f"{Colors.BRIGHT_RED}   Sample toxic:{Colors.RESET}")
                    for sample in toxic_samples:
                        print(f"{Colors.BRIGHT_RED}      • [{sample['author']}] {sample['text'][:50]}...{Colors.RESET}")
                
                all_comments.extend(labeled)

                # Delay between videos
                delay = random.uniform(3, 6)
                print(f"{Colors.BRIGHT_BLACK}   ⏳ Waiting {delay:.1f}s...{Colors.RESET}")
                time.sleep(delay)

            except Exception as e:
                print_error(f"Error: {str(e)}")
                import traceback
                traceback.print_exc()
                print(f"{Colors.BRIGHT_BLACK}   ⏳ Waiting 10s before retry...{Colors.RESET}")
                time.sleep(10)
                continue

    return all_comments


def prepare_training_dataset(labeled_comments, balance=True, min_samples=10):
    """Chuẩn bị dataset cho training"""
    
    print_header("⚙️ CHUẨN BỊ TRAINING DATASET", Colors.BRIGHT_BLUE)

    if not labeled_comments:
        print_error("No comments to process!")
        return []

    # Separate toxic and clean
    toxic_comments = [c for c in labeled_comments if c.get('is_toxic', False)]
    clean_comments = [c for c in labeled_comments if not c.get('is_toxic', False)]

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
            'text': comment['text'],
            'label': 1,
            'toxicity_score': comment.get('toxicity_score', 1.0),
            'categories': comment.get('toxic_categories', []),
            'author': comment.get('author', 'unknown'),
            'source': comment.get('source', 'unknown'),
            'is_reply': comment.get('is_reply', False),
            'likes': comment.get('likes', 0),
            'video_id': comment.get('video_id', 'unknown')
        })

    # Add clean samples (label = 0)
    for comment in clean_comments:
        dataset.append({
            'text': comment['text'],
            'label': 0,
            'toxicity_score': 0.0,
            'categories': [],
            'author': comment.get('author', 'unknown'),
            'source': comment.get('source', 'unknown'),
            'is_reply': comment.get('is_reply', False),
            'likes': comment.get('likes', 0),
            'video_id': comment.get('video_id', 'unknown')
        })

    print(f"\n{Colors.BRIGHT_GREEN}✅ Final dataset: {len(dataset)} samples{Colors.RESET}")
    print_stat("Toxic (label=1)", len(toxic_comments), Colors.BRIGHT_RED)
    print_stat("Clean (label=0)", len(clean_comments), Colors.BRIGHT_GREEN)

    return dataset


def save_dataset(dataset, output_dir='data'):
    """Lưu dataset"""
    os.makedirs(output_dir, exist_ok=True)

    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")

    if not dataset:
        print_error("Cannot save empty dataset!")
        return None, None, None

    # Save JSON
    json_path = f"{output_dir}/toxic_dataset_{timestamp}.json"
    with open(json_path, 'w', encoding='utf-8') as f:
        json.dump(dataset, f, indent=2, ensure_ascii=False)
    print_success(f"Saved JSON: {json_path}")

    # Save CSV
    import csv
    csv_path = f"{output_dir}/toxic_dataset_{timestamp}.csv"
    with open(csv_path, 'w', newline='', encoding='utf-8') as f:
        if dataset:
            writer = csv.DictWriter(f, fieldnames=dataset[0].keys())
            writer.writeheader()
            writer.writerows(dataset)
    print_success(f"Saved CSV: {csv_path}")

    # Save train/test split
    if len(dataset) >= 10:
        try:
            from sklearn.model_selection import train_test_split

            train_data, test_data = train_test_split(
                dataset,
                test_size=0.2,
                random_state=42,
                stratify=[d['label'] for d in dataset]
            )

            train_path = f"{output_dir}/train_dataset_{timestamp}.json"
            test_path = f"{output_dir}/test_dataset_{timestamp}.json"

            with open(train_path, 'w', encoding='utf-8') as f:
                json.dump(train_data, f, indent=2, ensure_ascii=False)
            print_success(f"Saved train: {train_path} ({len(train_data)} samples)")

            with open(test_path, 'w', encoding='utf-8') as f:
                json.dump(test_data, f, indent=2, ensure_ascii=False)
            print_success(f"Saved test: {test_path} ({len(test_data)} samples)")

            return json_path, train_path, test_path
            
        except Exception as e:
            print_warning(f"Could not split dataset: {e}")
            return json_path, None, None
    else:
        print_warning("Dataset too small - skipping train/test split")
        return json_path, None, None


def print_dataset_stats(dataset):
    """In thống kê chi tiết với màu sắc"""
    
    print_header("📊 DATASET STATISTICS", Colors.BRIGHT_GREEN)

    toxic_count = sum(1 for d in dataset if d['label'] == 1)
    clean_count = sum(1 for d in dataset if d['label'] == 0)

    print(f"{Colors.BRIGHT_YELLOW}📈 Overall:{Colors.RESET}")
    print_stat("Total samples", len(dataset), Colors.BRIGHT_WHITE)
    print_stat("Toxic (label=1)", 
               f"{toxic_count} ({toxic_count/len(dataset)*100:.1f}%)", 
               Colors.BRIGHT_RED)
    print_stat("Clean (label=0)", 
               f"{clean_count} ({clean_count/len(dataset)*100:.1f}%)", 
               Colors.BRIGHT_GREEN)

    # Source distribution
    from collections import Counter
    source_counts = Counter(d['source'] for d in dataset)
    
    print(f"\n{Colors.BRIGHT_YELLOW}📱 Source distribution:{Colors.RESET}")
    for source, count in source_counts.items():
        color = Colors.BRIGHT_RED if source == 'youtube' else Colors.BRIGHT_MAGENTA
        print_stat(source.capitalize(), 
                  f"{count} ({count/len(dataset)*100:.1f}%)", 
                  color)

    # Reply vs main comments
    reply_count = sum(1 for d in dataset if d.get('is_reply', False))
    main_count = len(dataset) - reply_count
    
    print(f"\n{Colors.BRIGHT_YELLOW}💬 Comment types:{Colors.RESET}")
    print_stat("Main comments", 
               f"{main_count} ({main_count/len(dataset)*100:.1f}%)", 
               Colors.BRIGHT_CYAN)
    print_stat("Replies", 
               f"{reply_count} ({reply_count/len(dataset)*100:.1f}%)", 
               Colors.BRIGHT_GREEN)

    # Average text length
    avg_len = sum(len(d['text']) for d in dataset) / len(dataset)
    
    print(f"\n{Colors.BRIGHT_YELLOW}📏 Text statistics:{Colors.RESET}")
    print_stat("Average length", f"{avg_len:.1f} characters", Colors.BRIGHT_WHITE)
    
    # Length distribution
    short = sum(1 for d in dataset if len(d['text']) < 50)
    medium = sum(1 for d in dataset if 50 <= len(d['text']) < 150)
    long = sum(1 for d in dataset if len(d['text']) >= 150)
    
    print_stat("Short (<50)", f"{short} ({short/len(dataset)*100:.1f}%)", Colors.BRIGHT_CYAN)
    print_stat("Medium (50-150)", f"{medium} ({medium/len(dataset)*100:.1f}%)", Colors.BRIGHT_BLUE)
    print_stat("Long (>=150)", f"{long} ({long/len(dataset)*100:.1f}%)", Colors.BRIGHT_MAGENTA)

    # Toxic categories
    if any(d.get('categories') for d in dataset):
        all_categories = []
        for d in dataset:
            if d.get('categories'):
                all_categories.extend(d['categories'])
        
        if all_categories:
            cat_counts = Counter(all_categories)
            print(f"\n{Colors.BRIGHT_YELLOW}🚫 Toxic categories (Top 10):{Colors.RESET}")
            for cat, count in cat_counts.most_common(10):
                print_stat(cat, count, Colors.BRIGHT_RED)

    # Sample comments
    print(f"\n{Colors.BRIGHT_YELLOW}📝 Sample toxic comments:{Colors.RESET}")
    toxic_samples = [d for d in dataset if d['label'] == 1][:5]
    for i, sample in enumerate(toxic_samples, 1):
        source_emoji = "📺" if sample['source'] == 'youtube' else "🎵"
        print(f"{Colors.BRIGHT_RED}   {i}. {source_emoji} [{sample['author']}] {sample['text'][:60]}...{Colors.RESET}")
    
    print(f"\n{Colors.BRIGHT_YELLOW}📝 Sample clean comments:{Colors.RESET}")
    clean_samples = [d for d in dataset if d['label'] == 0][:5]
    for i, sample in enumerate(clean_samples, 1):
        source_emoji = "📺" if sample['source'] == 'youtube' else "🎵"
        print(f"{Colors.BRIGHT_GREEN}   {i}. {source_emoji} [{sample['author']}] {sample['text'][:60]}...{Colors.RESET}")


# ============================================================================
# MAIN
# ============================================================================
if __name__ == "__main__":
    
    # =========================================================================
    # CÀI ĐẶT
    # =========================================================================

    YOUTUBE_API_KEY = "AIzaSyCC8sYNhEDpvuqg7l64yWkDTG4AvevGIJw"

    # =========================================================================
    # YOUTUBE VIDEOS
    # =========================================================================
    youtube_video_urls = [
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

    # =========================================================================
    # TIKTOK VIDEOS
    # =========================================================================
    tiktok_video_urls = [
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
"https://www.tiktok.com/@ttnhu8/video/7569485163298589970"
"https://www.tiktok.com/@chilama_gfx_animezz/video/7549748983959293202",
"https://www.tiktok.com/@vietnamhealthnews/video/7563633491913493780",
"https://www.tiktok.com/@ngmhieu266/video/7561286412340006165",
"https://www.tiktok.com/@vietnamexpress.diary/video/7548089579614588178",
"https://www.tiktok.com/@themlon1999/video/7569861284037020949",
"https://www.tiktok.com/@brave_traveler/video/7550590755882962196",
"https://www.tiktok.com/@nguyenthivinh61169/video/7569548031561977108",
"https://www.tiktok.com/@ysan1006/video/7569444081655368978",
"https://www.tiktok.com/@_tma25/video/7569560441144347911",
"https://www.tiktok.com/@_ltvkhongsuyy/video/7569603674624888084"
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

    # =========================================================================
    # THU THẬP COMMENTS
    # =========================================================================

    print_header("🚀 BẮT ĐẦU THU THẬP DATASET - FULLY INTEGRATED", Colors.BRIGHT_CYAN)
    
    print_stat("📺 YouTube videos", len(youtube_video_urls), Colors.BRIGHT_RED)
    print_stat("🎵 TikTok videos", len(tiktok_video_urls), Colors.BRIGHT_MAGENTA)

    all_comments = []

    # Thu thập từ YouTube
    if youtube_video_urls:
        print_subheader("PHASE 1: YOUTUBE", Colors.BRIGHT_RED)
        
        youtube_comments = collect_dataset_from_youtube(
            api_key=YOUTUBE_API_KEY,
            video_urls=youtube_video_urls,
            max_per_video=10000
        )
        all_comments.extend(youtube_comments)
        print_success(f"YouTube done: {len(youtube_comments)} comments")

    # Thu thập từ TikTok
    if tiktok_video_urls:
        print_subheader("PHASE 2: TIKTOK - FIXED SCRAPER", Colors.BRIGHT_MAGENTA)
        
        tiktok_comments = collect_dataset_from_tiktok(
            video_urls=tiktok_video_urls,
            max_per_video=3000,
            headless=True  # Set True for production
        )
        all_comments.extend(tiktok_comments)
        print_success(f"TikTok done: {len(tiktok_comments)} comments (including replies)")

    # =========================================================================
    # KIỂM TRA DỮ LIỆU
    # =========================================================================

    print_header("✅ COLLECTION COMPLETE", Colors.BRIGHT_GREEN)
    print_stat("Total comments", len(all_comments), Colors.BRIGHT_YELLOW)

    if not all_comments:
        print_error("No comments collected!")
        print()
        print_info("Suggestions:")
        print("   1. Add more video URLs")
        print("   2. Check if you logged in to TikTok")
        print("   3. Check API key for YouTube")
        exit(1)

    # =========================================================================
    # CHUẨN BỊ DATASET
    # =========================================================================

    dataset = prepare_training_dataset(
        labeled_comments=all_comments,
        balance=True,
        min_samples=10
    )

    if not dataset:
        print_error("No dataset created! Exiting...")
        exit(1)

    # =========================================================================
    # LƯU DATASET
    # =========================================================================

    print_subheader("💾 SAVING DATASET", Colors.BRIGHT_BLUE)
    json_path, train_path, test_path = save_dataset(dataset, output_dir='data')

    # =========================================================================
    # THỐNG KÊ CHI TIẾT
    # =========================================================================

    print_dataset_stats(dataset)

    # =========================================================================
    # KẾT LUẬN
    # =========================================================================

    print_header("✅ DATASET COLLECTION COMPLETE!", Colors.BRIGHT_GREEN)
    
    print(f"{Colors.BRIGHT_YELLOW}📁 Files saved:{Colors.RESET}")
    print_stat("JSON", json_path, Colors.BRIGHT_CYAN)
    if train_path:
        print_stat("Train", train_path, Colors.BRIGHT_GREEN)
    if test_path:
        print_stat("Test", test_path, Colors.BRIGHT_BLUE)
    
    if len(dataset) >= 100:
        print(f"\n{Colors.BRIGHT_GREEN}🚀 Next steps:{Colors.RESET}")
        print(f"{Colors.BRIGHT_CYAN}   1. Review dataset quality{Colors.RESET}")
        print(f"{Colors.BRIGHT_CYAN}   2. Train model using the dataset{Colors.RESET}")
        print(f"{Colors.BRIGHT_CYAN}   3. Evaluate model performance{Colors.RESET}")
    else:
        print(f"\n{Colors.BRIGHT_YELLOW}⚠️  Dataset is small ({len(dataset)} samples){Colors.RESET}")
        print(f"\n{Colors.BRIGHT_BLUE}💡 Recommendations:{Colors.RESET}")
        print(f"{Colors.BRIGHT_WHITE}   1. Add more videos (aim for 50-100 videos){Colors.RESET}")
        print(f"{Colors.BRIGHT_WHITE}   2. Mix different content types{Colors.RESET}")
        print(f"{Colors.BRIGHT_WHITE}   3. Target: 3000-10000 samples for good model{Colors.RESET}")
    
    print(f"\n{Colors.BRIGHT_MAGENTA}{'='*70}{Colors.RESET}\n")