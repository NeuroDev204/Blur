"""
JSON Exporter Module
Export comments to clean JSON format
"""

import json
import csv
from pathlib import Path
from datetime import datetime
from typing import List, Dict, Any, Optional, Literal
from scraper.models.comment import Comment, CommentMetadata
from scraper.utils.logger import get_logger

logger = get_logger(__name__)


class JSONExporter:
    """
    JSON Comment Exporter
    
    Features:
        - UTF-8 encoding (không lỗi encoding)
        - Proper JSON structure với meta và comments
        - Optional CSV export
        - Deduplication before export
    """
    
    def __init__(self, output_dir: str = "data"):
        """
        Initialize exporter
        
        Args:
            output_dir: Directory to save files
        """
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)
    
    def _deduplicate(self, comments: List[Comment]) -> List[Comment]:
        """Remove duplicate comments"""
        seen = set()
        unique = []
        
        for comment in comments:
            # Hash by content + platform
            content_hash = (comment.content.lower().strip(), comment.platform)
            
            if content_hash not in seen:
                seen.add(content_hash)
                unique.append(comment)
        
        removed = len(comments) - len(unique)
        if removed > 0:
            logger.info(f"Removed {removed} duplicate comments")
        
        return unique
    
    def _format_comment(self, comment: Comment) -> Dict[str, Any]:
        """Format comment for JSON output"""
        data = {
            'id': comment.id,
            'user': comment.user,
            'content': comment.content,
            'timestamp': comment.timestamp,
            'platform': comment.platform,
            'like_count': comment.like_count,
            'reply_count': comment.reply_count,
            'parent_id': comment.parent_id
        }
        
        # Add label if present
        if comment.label:
            data['label'] = {
                'sentiment': comment.label.sentiment,
                'toxic_score': round(comment.label.toxic_score, 3),
                'topic': comment.label.topic
            }
        
        return data
    
    def export_json(
        self,
        comments: List[Comment],
        platform: Literal['youtube', 'tiktok', 'mixed'] = 'mixed',
        filename: Optional[str] = None,
        deduplicate: bool = True
    ) -> str:
        """
        Export comments to JSON file
        
        Output format:
        {
            "meta": {
                "platform": "...",
                "total_comments": 1234,
                "crawled_at": "2025-12-12T10:00:00"
            },
            "comments": [...]
        }
        
        Args:
            comments: List of comments
            platform: Platform name
            filename: Custom filename (optional)
            deduplicate: Remove duplicates before export
            
        Returns:
            Path to saved file
        """
        # Deduplicate
        if deduplicate:
            comments = self._deduplicate(comments)
        
        if not comments:
            logger.warning("No comments to export")
            return ""
        
        # Create metadata
        meta = {
            'platform': platform,
            'total_comments': len(comments),
            'crawled_at': datetime.now().isoformat(),
            'main_comments': sum(1 for c in comments if c.parent_id is None),
            'replies': sum(1 for c in comments if c.parent_id is not None)
        }
        
        # Format comments
        formatted_comments = [self._format_comment(c) for c in comments]
        
        # Build output
        output = {
            'meta': meta,
            'comments': formatted_comments
        }
        
        # Generate filename
        if not filename:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            filename = f"comments_{platform}_{timestamp}.json"
        
        filepath = self.output_dir / filename
        
        # Write JSON with UTF-8
        with open(filepath, 'w', encoding='utf-8') as f:
            json.dump(output, f, indent=2, ensure_ascii=False)
        
        logger.info(f"Exported {len(comments)} comments to {filepath}")
        
        return str(filepath)
    
    def export_csv(
        self,
        comments: List[Comment],
        filename: Optional[str] = None,
        deduplicate: bool = True
    ) -> str:
        """
        Export comments to CSV file
        
        Args:
            comments: List of comments
            filename: Custom filename
            deduplicate: Remove duplicates
            
        Returns:
            Path to saved file
        """
        if deduplicate:
            comments = self._deduplicate(comments)
        
        if not comments:
            logger.warning("No comments to export")
            return ""
        
        # Generate filename
        if not filename:
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            filename = f"comments_{timestamp}.csv"
        
        filepath = self.output_dir / filename
        
        # Define fields
        fieldnames = [
            'id', 'user', 'content', 'timestamp', 'platform',
            'like_count', 'reply_count', 'parent_id',
            'sentiment', 'toxic_score', 'topic'
        ]
        
        # Write CSV
        with open(filepath, 'w', newline='', encoding='utf-8') as f:
            writer = csv.DictWriter(f, fieldnames=fieldnames)
            writer.writeheader()
            
            for comment in comments:
                row = {
                    'id': comment.id,
                    'user': comment.user,
                    'content': comment.content,
                    'timestamp': comment.timestamp,
                    'platform': comment.platform,
                    'like_count': comment.like_count,
                    'reply_count': comment.reply_count,
                    'parent_id': comment.parent_id or ''
                }
                
                if comment.label:
                    row['sentiment'] = comment.label.sentiment
                    row['toxic_score'] = round(comment.label.toxic_score, 3)
                    row['topic'] = comment.label.topic
                else:
                    row['sentiment'] = ''
                    row['toxic_score'] = ''
                    row['topic'] = ''
                
                writer.writerow(row)
        
        logger.info(f"Exported {len(comments)} comments to {filepath}")
        
        return str(filepath)
    
    def export_training_data(
        self,
        comments: List[Comment],
        label_type: Literal['toxic', 'sentiment'] = 'toxic',
        test_split: float = 0.2
    ) -> Dict[str, str]:
        """
        Export comments as training data
        
        Args:
            comments: List of comments
            label_type: 'toxic' hoặc 'sentiment'
            test_split: Ratio for test set
            
        Returns:
            Dict with paths to train and test files
        """
        import random
        
        # Filter comments with labels
        labeled = [c for c in comments if c.label is not None]
        
        if not labeled:
            logger.warning("No labeled comments to export")
            return {}
        
        # Shuffle
        random.shuffle(labeled)
        
        # Split
        split_idx = int(len(labeled) * (1 - test_split))
        train_comments = labeled[:split_idx]
        test_comments = labeled[split_idx:]
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        
        # Format for training
        def format_training_sample(comment: Comment) -> Dict:
            if label_type == 'toxic':
                label = 1 if comment.label.toxic_score >= 0.45 else 0
            else:
                label_map = {'positive': 2, 'neutral': 1, 'negative': 0}
                label = label_map.get(comment.label.sentiment, 1)
            
            return {
                'text': comment.content,
                'label': label
            }
        
        # Export train set
        train_data = [format_training_sample(c) for c in train_comments]
        train_path = self.output_dir / f"train_{label_type}_{timestamp}.json"
        
        with open(train_path, 'w', encoding='utf-8') as f:
            json.dump(train_data, f, indent=2, ensure_ascii=False)
        
        # Export test set
        test_data = [format_training_sample(c) for c in test_comments]
        test_path = self.output_dir / f"test_{label_type}_{timestamp}.json"
        
        with open(test_path, 'w', encoding='utf-8') as f:
            json.dump(test_data, f, indent=2, ensure_ascii=False)
        
        logger.info(f"Exported training data: {len(train_data)} train, {len(test_data)} test")
        
        return {
            'train': str(train_path),
            'test': str(test_path)
        }
