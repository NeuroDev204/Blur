"""
Comment Scraper - Main Entry Point
CLI interface cho comment scraping
"""

import asyncio
import argparse
import sys
from typing import List, Optional
from pathlib import Path

from scraper.config import Config, get_config
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


async def scrape_youtube(
    urls: List[str],
    api_key: Optional[str] = None,
    max_per_video: int = 1000,
    vietnamese_only: bool = True
) -> List[Comment]:
    """Scrape comments from YouTube videos"""
    
    print_subheader("📺 YOUTUBE SCRAPING", Colors.BRIGHT_RED)
    print_stat("Videos", len(urls))
    print_stat("Max per video", max_per_video)
    
    config = get_config()
    api_key = api_key or config.youtube.api_key
    
    if not api_key:
        print_error("YouTube API key not found. Set YOUTUBE_API_KEY environment variable.")
        return []
    
    all_comments = []
    
    async with YouTubeScraper(api_key=api_key) as scraper:
        for i, url in enumerate(urls, 1):
            try:
                video_id = scraper.extract_video_id(url)
                print(f"\n{Colors.BRIGHT_YELLOW}[{i}/{len(urls)}]{Colors.RESET} "
                      f"{Colors.CYAN}Video: {video_id}{Colors.RESET}")
                
                # Get video info
                info = await scraper.get_video_info(video_id)
                if info:
                    print_stat("Title", info['title'][:60])
                    print_stat("Channel", info['channel'])
                
                # Get comments
                comments = await scraper.get_comments(
                    video_url=url,
                    max_results=max_per_video,
                    include_replies=True,
                    vietnamese_only=vietnamese_only
                )
                
                all_comments.extend(comments)
                print_success(f"Collected {len(comments)} comments")
                
            except Exception as e:
                print_error(f"Error: {e}")
                continue
    
    print(f"\n{Colors.BRIGHT_GREEN}✅ YouTube total: {len(all_comments)} comments{Colors.RESET}")
    return all_comments


async def scrape_tiktok(
    urls: List[str],
    max_per_video: int = 3000,
    vietnamese_only: bool = True,
    headless: bool = False
) -> List[Comment]:
    """Scrape comments from TikTok videos"""
    
    print_subheader("🎵 TIKTOK SCRAPING", Colors.BRIGHT_MAGENTA)
    print_stat("Videos", len(urls))
    print_stat("Max per video", max_per_video)
    print_info("First time: Manual login required")
    
    all_comments = []
    
    scraper = TikTokScraper(headless=headless, max_scroll=100)
    
    try:
        for i, url in enumerate(urls, 1):
            try:
                video_id = scraper.extract_video_id(url)
                print(f"\n{Colors.BRIGHT_YELLOW}[{i}/{len(urls)}]{Colors.RESET} "
                      f"{Colors.MAGENTA}Video: {video_id}{Colors.RESET}")
                
                comments = await scraper.get_comments(
                    video_url=url,
                    max_results=max_per_video,
                    include_replies=True,
                    vietnamese_only=vietnamese_only
                )
                
                all_comments.extend(comments)
                print_success(f"Collected {len(comments)} comments")
                
                # Delay between videos
                await asyncio.sleep(3)
                
            except Exception as e:
                print_error(f"Error: {e}")
                continue
    finally:
        scraper.close()
    
    print(f"\n{Colors.BRIGHT_MAGENTA}✅ TikTok total: {len(all_comments)} comments{Colors.RESET}")
    return all_comments


async def run_scraper(
    youtube_urls: Optional[List[str]] = None,
    tiktok_urls: Optional[List[str]] = None,
    youtube_api_key: Optional[str] = None,
    max_per_video: int = 1000,
    vietnamese_only: bool = True,
    headless: bool = False,
    output_dir: str = "data",
    label_comments: bool = True,
    export_csv: bool = False
) -> str:
    """
    Main scraper function
    
    Args:
        youtube_urls: List of YouTube video URLs
        tiktok_urls: List of TikTok video URLs
        youtube_api_key: YouTube API key
        max_per_video: Max comments per video
        vietnamese_only: Vietnamese only filter
        headless: Run TikTok scraper headless
        output_dir: Output directory
        label_comments: Apply NLP labeling
        export_csv: Also export to CSV
        
    Returns:
        Path to exported JSON file
    """
    print_header("🚀 COMMENT SCRAPER - OPTIMIZED", Colors.BRIGHT_CYAN)
    
    all_comments: List[Comment] = []
    
    # Scrape YouTube
    if youtube_urls:
        yt_comments = await scrape_youtube(
            urls=youtube_urls,
            api_key=youtube_api_key,
            max_per_video=max_per_video,
            vietnamese_only=vietnamese_only
        )
        all_comments.extend(yt_comments)
    
    # Scrape TikTok
    if tiktok_urls:
        tt_comments = await scrape_tiktok(
            urls=tiktok_urls,
            max_per_video=max_per_video,
            vietnamese_only=vietnamese_only,
            headless=headless
        )
        all_comments.extend(tt_comments)
    
    if not all_comments:
        print_error("No comments collected!")
        return ""
    
    print_header("📊 PROCESSING", Colors.BRIGHT_BLUE)
    print_stat("Total raw comments", len(all_comments))
    
    # Validate
    validator = CommentValidator(vietnamese_only=vietnamese_only)
    valid_comments, _ = validator.validate_batch(all_comments)
    
    print_stat("Valid comments", len(valid_comments))
    
    # Label
    if label_comments:
        print_subheader("🏷️  LABELING COMMENTS", Colors.BRIGHT_YELLOW)
        labeler = NLPLabeler(use_ml_sentiment=False)
        valid_comments = labeler.label_comments_batch(valid_comments)
        
        # Stats
        toxic_count = sum(
            1 for c in valid_comments 
            if c.label and c.label.toxic_level != 'low'
        )
        print_stat("Toxic comments", f"{toxic_count} ({toxic_count/len(valid_comments)*100:.1f}%)")
    
    # Export
    print_subheader("💾 EXPORTING", Colors.BRIGHT_GREEN)
    
    exporter = JSONExporter(output_dir=output_dir)
    
    # Determine platform
    yt_count = sum(1 for c in valid_comments if c.platform == 'youtube')
    tt_count = sum(1 for c in valid_comments if c.platform == 'tiktok')
    
    if yt_count > 0 and tt_count > 0:
        platform = 'mixed'
    elif yt_count > 0:
        platform = 'youtube'
    else:
        platform = 'tiktok'
    
    json_path = exporter.export_json(valid_comments, platform=platform)
    
    if export_csv:
        csv_path = exporter.export_csv(valid_comments)
        print_stat("CSV", csv_path)
    
    # Summary
    print_header("✅ COMPLETE", Colors.BRIGHT_GREEN)
    print_stat("Total comments", len(valid_comments))
    print_stat("YouTube", yt_count)
    print_stat("TikTok", tt_count)
    print_stat("Output", json_path)
    
    return json_path


def main():
    """CLI entry point"""
    parser = argparse.ArgumentParser(
        description="Comment Scraper - YouTube & TikTok",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Scrape YouTube videos
  python -m scraper.main --youtube "URL1,URL2" --api-key YOUR_KEY

  # Scrape TikTok videos
  python -m scraper.main --tiktok "URL1,URL2"

  # Scrape both
  python -m scraper.main --youtube "URL1" --tiktok "URL2" --max 500
        """
    )
    
    parser.add_argument(
        '--youtube', '-y',
        type=str,
        help='YouTube video URLs (comma-separated)'
    )
    
    parser.add_argument(
        '--tiktok', '-t',
        type=str,
        help='TikTok video URLs (comma-separated)'
    )
    
    parser.add_argument(
        '--api-key',
        type=str,
        help='YouTube API key (or set YOUTUBE_API_KEY env var)'
    )
    
    parser.add_argument(
        '--max', '-m',
        type=int,
        default=1000,
        help='Max comments per video (default: 1000)'
    )
    
    parser.add_argument(
        '--output', '-o',
        type=str,
        default='data',
        help='Output directory (default: data)'
    )
    
    parser.add_argument(
        '--no-label',
        action='store_true',
        help='Skip NLP labeling'
    )
    
    parser.add_argument(
        '--csv',
        action='store_true',
        help='Also export to CSV'
    )
    
    parser.add_argument(
        '--headless',
        action='store_true',
        help='Run TikTok scraper in headless mode'
    )
    
    parser.add_argument(
        '--all-languages',
        action='store_true',
        help='Include all languages (not just Vietnamese)'
    )
    
    args = parser.parse_args()
    
    # Parse URLs
    youtube_urls = None
    tiktok_urls = None
    
    if args.youtube:
        youtube_urls = [url.strip() for url in args.youtube.split(',') if url.strip()]
    
    if args.tiktok:
        tiktok_urls = [url.strip() for url in args.tiktok.split(',') if url.strip()]
    
    if not youtube_urls and not tiktok_urls:
        parser.print_help()
        print("\n❌ Error: At least one URL is required (--youtube or --tiktok)")
        sys.exit(1)
    
    # Run
    try:
        result = asyncio.run(run_scraper(
            youtube_urls=youtube_urls,
            tiktok_urls=tiktok_urls,
            youtube_api_key=args.api_key,
            max_per_video=args.max,
            vietnamese_only=not args.all_languages,
            headless=args.headless,
            output_dir=args.output,
            label_comments=not args.no_label,
            export_csv=args.csv
        ))
        
        if result:
            print(f"\n✅ Done! Output: {result}\n")
        else:
            sys.exit(1)
            
    except KeyboardInterrupt:
        print("\n⚠️  Interrupted by user")
        sys.exit(0)
    except Exception as e:
        print(f"\n❌ Error: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()
