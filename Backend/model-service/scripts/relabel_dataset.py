"""
scripts/relabel_dataset.py
Re-label JSON dataset dùng ToxicDetectorV2 thay vì v1.

Mục đích:
- ToxicDetector v1 có base_score=0.3 và nhiều false positive
- ToxicDetectorV2 context-aware, whitelist → nhãn chính xác hơn
- Chạy trước khi train_model_v2.py để cải thiện chất lượng data

Usage:
  python scripts/relabel_dataset.py
  python scripts/relabel_dataset.py --dry-run   # xem thống kê, không ghi file
"""

import json
import glob
import argparse
from datetime import datetime
from pathlib import Path

import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))

from scraper.labelers.toxic_detector import ToxicDetectorV2


def normalize_items(data) -> list:
    """
    Chuẩn hóa về list[{text, label}] từ các format khác nhau:
      Format A: list[{text, label, ...}]           — train_dataset_*.json
      Format B: list[{text, label}]                — đơn giản
      Format C: {meta, comments: [{content, label, ...}]}  — scraped comments
    """
    if isinstance(data, list):
        items = []
        for item in data:
            if isinstance(item, dict):
                text = item.get('text') or item.get('content', '')
                raw_label = item.get('label', 0)
                if isinstance(raw_label, dict):
                    score = raw_label.get('toxic_score', 0.0)
                    label = 1 if score >= 0.45 else 0
                else:
                    label = int(raw_label) if raw_label is not None else 0
                items.append({'text': text, 'label': label})
            elif isinstance(item, str):
                items.append({'text': item, 'label': 0})
        return items
    elif isinstance(data, dict) and 'comments' in data:
        items = []
        for c in data['comments']:
            text = c.get('content') or c.get('text', '')
            raw_label = c.get('label', 0)
            # label có thể là dict {toxic_score, sentiment, topic} từ scraper
            if isinstance(raw_label, dict):
                score = raw_label.get('toxic_score', 0.0)
                label = 1 if score >= 0.45 else 0
            else:
                label = int(raw_label) if raw_label is not None else 0
            items.append({'text': text, 'label': label})
        return items
    return []


def relabel_file(input_path: str, detector: ToxicDetectorV2, dry_run: bool = False):
    with open(input_path, 'r', encoding='utf-8') as f:
        data = json.load(f)

    items = normalize_items(data)
    if not items:
        print(f"  SKIP: unrecognized format")
        return {'total': 0, 'unchanged': 0, 'flip_toxic_to_clean': 0, 'flip_clean_to_toxic': 0}

    stats = {'total': 0, 'unchanged': 0, 'flip_toxic_to_clean': 0, 'flip_clean_to_toxic': 0}
    relabeled = []

    for item in items:
        text = item.get('text', '')
        old_label = int(item.get('label', 0))

        result = detector.analyze(text)
        new_label = 1 if result['is_toxic'] else 0

        stats['total'] += 1
        if old_label == new_label:
            stats['unchanged'] += 1
        elif old_label == 1 and new_label == 0:
            stats['flip_toxic_to_clean'] += 1  # false positive được sửa
        else:
            stats['flip_clean_to_toxic'] += 1  # false negative được sửa

        if text:
            relabeled.append({
                'text': text,
                'label': new_label,
                'toxic_score': result['toxic_score'],
                'toxic_categories': result['toxic_categories'],
            })

    if not dry_run:
        # Lưu file mới, giữ nguyên file gốc
        out_path = input_path.replace('.json', '_relabeled.json')
        with open(out_path, 'w', encoding='utf-8') as f:
            json.dump(relabeled, f, ensure_ascii=False, indent=2)
        print(f"  Saved: {out_path}")

    return stats


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--dry-run', action='store_true', help='Chỉ in thống kê, không ghi file')
    parser.add_argument('--data-dir', default='data', help='Thư mục chứa JSON files')
    args = parser.parse_args()

    detector = ToxicDetectorV2(threshold=0.5)

    json_files = glob.glob(f'{args.data_dir}/*.json')
    # Bỏ qua các file đã relabel
    json_files = [f for f in json_files if '_relabeled' not in f]

    if not json_files:
        print(f"Không tìm thấy JSON file trong {args.data_dir}/")
        return

    print(f"{'DRY RUN — ' if args.dry_run else ''}Re-labeling {len(json_files)} file(s)...\n")

    total_stats = {'total': 0, 'unchanged': 0, 'flip_toxic_to_clean': 0, 'flip_clean_to_toxic': 0}

    for fp in json_files:
        print(f"Processing: {fp}")
        stats = relabel_file(fp, detector, dry_run=args.dry_run)

        changed = stats['flip_toxic_to_clean'] + stats['flip_clean_to_toxic']
        fp_fixed = stats['flip_toxic_to_clean']
        print(f"  Total: {stats['total']:,} | Unchanged: {stats['unchanged']:,} | "
              f"Changed: {changed:,} | FP fixed: {fp_fixed:,} "
              f"({fp_fixed/max(stats['total'],1)*100:.1f}%)\n")

        for k in total_stats:
            total_stats[k] += stats[k]

    total = total_stats['total']
    changed = total_stats['flip_toxic_to_clean'] + total_stats['flip_clean_to_toxic']
    print("=" * 60)
    print(f"SUMMARY")
    print(f"  Total samples:          {total:,}")
    print(f"  Unchanged:              {total_stats['unchanged']:,} ({total_stats['unchanged']/max(total,1)*100:.1f}%)")
    print(f"  Toxic → Clean (FP fix): {total_stats['flip_toxic_to_clean']:,} ({total_stats['flip_toxic_to_clean']/max(total,1)*100:.1f}%)")
    print(f"  Clean → Toxic (FN fix): {total_stats['flip_clean_to_toxic']:,} ({total_stats['flip_clean_to_toxic']/max(total,1)*100:.1f}%)")
    print(f"  Total changed:          {changed:,} ({changed/max(total,1)*100:.1f}%)")

    if args.dry_run:
        print("\n[DRY RUN] Không ghi file. Chạy lại không có --dry-run để áp dụng.")
    else:
        print("\nDone! Dùng file *_relabeled.json trong train_model_v2.py")


if __name__ == '__main__':
    main()
