# Comment Scraper - YouTube & TikTok

**Phiên bản tối ưu** của tool cào bình luận từ YouTube và TikTok với:
- ✅ Async/await cho hiệu năng cao
- ✅ **PhoBERT** - Mô hình NLP tiếng Việt tốt nhất cho sentiment analysis
- ✅ Auto retry với exponential backoff
- ✅ Pagination đầy đủ (không sót bình luận)
- ✅ Data validation & deduplication
- ✅ NLP labeling (sentiment, toxic, topic)
- ✅ Cấu trúc modular dễ mở rộng

## 📁 Cấu Trúc Thư Mục

```
scraper/
├── __init__.py
├── config.py              # Cấu hình từ .env
├── main.py                # CLI entry point
├── requirements.txt       # Dependencies
├── .env.example           # Template env vars
├── models/
│   └── comment.py         # Data models
├── scrapers/
│   ├── base_scraper.py    # Abstract base class
│   ├── youtube_scraper.py # Async YouTube API
│   └── tiktok_scraper.py  # Selenium TikTok
├── validators/
│   └── comment_validator.py
├── labelers/
│   ├── nlp_labeler.py     # Main labeling
│   ├── sentiment_analyzer.py
│   ├── topic_classifier.py
│   └── toxic_detector.py
├── exporters/
│   └── json_exporter.py
└── utils/
    ├── logger.py          # Colored logging
    ├── retry.py           # Retry decorators
    └── proxy.py           # Proxy rotation
```

## 🚀 Cài Đặt

### 1. Cài dependencies

```bash
cd f:\python
pip install -r scraper/requirements.txt
```

### 2. Cấu hình

Copy file `.env.example` thành `.env` và điền API key:

```bash
cp scraper/.env.example scraper/.env
```

Sửa file `.env`:
```
YOUTUBE_API_KEY=your_youtube_api_key_here
```

## 📖 Cách Sử Dụng

### CLI (Command Line)

```bash
# Cào YouTube
python -m scraper.main --youtube "URL1,URL2" --max 500

# Cào TikTok
python -m scraper.main --tiktok "URL1,URL2" --max 1000

# Cào cả hai
python -m scraper.main --youtube "URL1" --tiktok "URL2" --max 500 --csv

# Headless mode (TikTok không hiện browser)
python -m scraper.main --tiktok "URL" --headless

# Skip labeling (nhanh hơn)
python -m scraper.main --youtube "URL" --no-label
```

### Trong Python Code

```python
import asyncio
from scraper.main import run_scraper

# Async
result = asyncio.run(run_scraper(
    youtube_urls=["https://youtube.com/watch?v=xxx"],
    tiktok_urls=["https://tiktok.com/@user/video/xxx"],
    max_per_video=500,
    label_comments=True
))

print(f"Output: {result}")
```

### Sử dụng từng module riêng

```python
# Label một bình luận
from scraper.labelers import label_comment

result = label_comment("Video hay quá bạn ơi!")
print(result)
# {'sentiment': 'positive', 'toxic_score': 0.0, 'topic': 'khen_video'}

# YouTube scraper
from scraper.scrapers import YouTubeScraper
import asyncio

async def main():
    async with YouTubeScraper(api_key="YOUR_KEY") as scraper:
        comments = await scraper.get_comments(
            video_url="https://youtube.com/watch?v=xxx",
            max_results=100
        )
        print(f"Got {len(comments)} comments")

asyncio.run(main())
```

## 📊 Output Format

File JSON output có cấu trúc:

```json
{
  "meta": {
    "platform": "youtube",
    "total_comments": 1234,
    "crawled_at": "2025-12-12T10:00:00",
    "main_comments": 1000,
    "replies": 234
  },
  "comments": [
    {
      "id": "xxx",
      "user": "username",
      "content": "Nội dung bình luận",
      "timestamp": "2025-12-12T10:00:00",
      "platform": "youtube",
      "like_count": 10,
      "reply_count": 2,
      "parent_id": null,
      "label": {
        "sentiment": "positive",
        "toxic_score": 0.0,
        "topic": "khen_video"
      }
    }
  ]
}
```

## 🏷️ NLP Labels

### Sentiment (Cảm xúc)
- `positive`: Tích cực, khen ngợi
- `neutral`: Trung lập
- `negative`: Tiêu cực, chê bai

### Toxic Score (Mức độ độc hại)
- `0.0 - 0.4`: Low (thấp)
- `0.4 - 0.7`: Medium (trung bình)
- `0.7 - 1.0`: High (cao)

### Topic (Chủ đề)
- `khen_video`: Khen ngợi video
- `che_video`: Chê bai video
- `hoi_thong_tin`: Hỏi thông tin
- `tranh_luan`: Tranh luận
- `spam`: Spam/quảng cáo
- `hai_huoc`: Hài hước
- `khac`: Khác

## ⚙️ Tùy Chọn CLI

| Option | Mô tả |
|--------|-------|
| `--youtube, -y` | YouTube URLs (phân cách bằng dấu phẩy) |
| `--tiktok, -t` | TikTok URLs (phân cách bằng dấu phẩy) |
| `--api-key` | YouTube API key |
| `--max, -m` | Max comments per video (default: 1000) |
| `--output, -o` | Output directory (default: data) |
| `--no-label` | Bỏ qua NLP labeling |
| `--csv` | Export thêm file CSV |
| `--headless` | Chạy TikTok không hiện browser |
| `--all-languages` | Bao gồm tất cả ngôn ngữ |

## 🔧 Mở Rộng

Để thêm platform mới (Facebook, Instagram...), kế thừa `BaseScraper`:

```python
from scraper.scrapers.base_scraper import BaseScraper

class FacebookScraper(BaseScraper):
    PLATFORM = "facebook"
    
    def extract_video_id(self, url: str) -> str:
        # Implementation
        pass
    
    async def get_comments(self, video_url: str, ...) -> List[Comment]:
        # Implementation
        pass
```

## 📝 Notes

1. **TikTok login**: Lần đầu chạy TikTok scraper cần login thủ công. Session sẽ được lưu cho lần sau.

2. **YouTube API quota**: Mỗi ngày có 10,000 quota. Mỗi request comment tốn ~1-2 quota.

3. **Proxy**: Nếu bị rate limit, tạo file `proxies.txt` với format:
   ```
   http://proxy1:8080
   http://user:pass@proxy2:8080
   ```

4. **PhoBERT Sentiment**: Mặc định sử dụng PhoBERT cho sentiment (yêu cầu GPU). Để dùng rule-based (nhanh hơn):
   ```python
   from scraper.labelers import NLPLabeler
   labeler = NLPLabeler(use_ml_sentiment=False)
   ```

## 📄 License

MIT License
