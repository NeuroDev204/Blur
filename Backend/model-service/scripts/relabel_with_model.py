"""
scripts/relabel_with_model.py — Dán nhãn lại dùng PhoBERT + rule-based ensemble

Chiến lược:
  - Model confident toxic   (score >= HIGH_CONF)  → label = 1
  - Model confident clean   (score <= LOW_CONF)   → label = 0
  - Vùng uncertain (LOW_CONF < score < HIGH_CONF) → dùng rule-based làm tiebreaker
  - Chỉ đổi nhãn khi model/ensemble đủ tin tưởng → bảo toàn nhãn gốc khi không chắc

Usage:
  python scripts/relabel_with_model.py
  python scripts/relabel_with_model.py --model-dir models/phobert_toxic_FINAL_20251111_232134
  python scripts/relabel_with_model.py --dry-run
  python scripts/relabel_with_model.py --high-conf 0.75 --low-conf 0.25
"""

import sys
import json
import glob
import argparse
from pathlib import Path
from dotenv import load_dotenv

# Load .env từ thư mục gốc model-service
_ROOT = Path(__file__).parent.parent
load_dotenv(_ROOT / '.env')

sys.path.insert(0, str(_ROOT))


# ============================================================================
# MODEL LOADER
# ============================================================================

def find_best_model(models_dir: str = 'models') -> str:
    """Tự động chọn model mới nhất có model.safetensors."""
    candidates = []
    for d in Path(models_dir).iterdir():
        if d.is_dir() and (d / 'model.safetensors').exists():
            candidates.append(d)
    if not candidates:
        raise FileNotFoundError(f"Không tìm thấy model nào trong {models_dir}/")
    # Ưu tiên FINAL, sau đó theo timestamp (tên thư mục)
    finals = [c for c in candidates if 'FINAL' in c.name]
    if finals:
        return str(finals[0])
    return str(sorted(candidates, key=lambda x: x.name)[-1])


def load_phobert(model_dir: str):
    """Load PhoBERT tokenizer + model lên GPU nếu có."""
    import torch
    from transformers import AutoTokenizer, AutoModelForSequenceClassification

    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    print(f"  Device: {device}")

    tokenizer = AutoTokenizer.from_pretrained(model_dir)
    model = AutoModelForSequenceClassification.from_pretrained(model_dir)
    model.to(device)
    model.eval()
    return tokenizer, model, device


# ============================================================================
# BATCH INFERENCE
# ============================================================================

def predict_batch(texts: list, tokenizer, model, device, batch_size: int = 32) -> list:
    """
    Trả về list[float] — toxic probability cho từng text.
    Dùng dynamic padding per-batch để tiết kiệm VRAM.
    """
    import torch

    all_scores = []
    for i in range(0, len(texts), batch_size):
        chunk = texts[i:i + batch_size]
        inputs = tokenizer(
            chunk,
            padding=True,
            truncation=True,
            max_length=256,
            return_tensors='pt',
        )
        inputs = {k: v.to(device) for k, v in inputs.items()}
        with torch.inference_mode():
            logits = model(**inputs).logits
            probs = torch.softmax(logits, dim=1)
        all_scores.extend(probs[:, 1].cpu().tolist())
    return all_scores


# ============================================================================
# RULE-BASED FALLBACK (inline, không import ToxicDetectorV2)
# ============================================================================

import re

_HARD_TOXIC = re.compile(
    r'(?:^|\s)(?:đụ|địt|lồn|cặc|buồi|đéo|đm|đmm|đmmm|đcm|đkm|vcl|dm|dcm|dkm|dmm|dmmm'
    r'|fuck|shit|bitch|fck)(?:\s|$|[!?.,])',
    re.IGNORECASE,
)
_INSULT_PHRASE = re.compile(
    r'(?:thằng ngu|con ngu|đồ ngu|thằng khốn|đồ khốn|mất dạy|vô học|óc chó|não tôm'
    r'|rác rưởi|thằng rác|đồ rác|giết mày|chém mày|cút đi|biến đi|đi chết|chết đi'
    r'|kill you|kill yourself|idiot|moron|retard)',
    re.IGNORECASE,
)
_FAMILY_INSULT = re.compile(
    r'(?:đm|dm|đcm|dcm|đkm|dkm)\s*(?:mẹ|me|má|cha|ba|bố|ông|bà)',
    re.IGNORECASE,
)

def rule_based_label(text: str) -> int:
    """0=clean, 1=toxic — rule cứng, chỉ dùng làm tiebreaker."""
    t = text.lower()
    if _HARD_TOXIC.search(t) or _INSULT_PHRASE.search(t) or _FAMILY_INSULT.search(t):
        return 1
    return 0


# ============================================================================
# DATA LOADING
# ============================================================================

def normalize_items(data) -> list:
    if isinstance(data, list):
        items = []
        for item in data:
            if not isinstance(item, dict):
                continue
            text = item.get('text') or item.get('content', '')
            raw = item.get('label', 0)
            if isinstance(raw, dict):
                score = raw.get('toxic_score', 0.0)
                label = 1 if score >= 0.45 else 0
            else:
                label = int(raw) if raw is not None else 0
            if text:
                items.append({'text': text, 'label': label})
        return items
    if isinstance(data, dict) and 'comments' in data:
        items = []
        for c in data['comments']:
            text = c.get('content') or c.get('text', '')
            raw = c.get('label', 0)
            if isinstance(raw, dict):
                score = raw.get('toxic_score', 0.0)
                label = 1 if score >= 0.45 else 0
            else:
                label = int(raw) if raw is not None else 0
            if text:
                items.append({'text': text, 'label': label})
        return items
    return []


# ============================================================================
# RELABEL FILE
# ============================================================================

def relabel_file(
    input_path: str,
    tokenizer, model, device,
    high_conf: float,
    low_conf: float,
    dry_run: bool,
) -> dict:
    with open(input_path, 'r', encoding='utf-8') as f:
        raw = json.load(f)

    items = normalize_items(raw)
    if not items:
        print("  SKIP: format không nhận dạng được")
        return {}

    texts = [it['text'] for it in items]
    old_labels = [it['label'] for it in items]

    print(f"  Inferring {len(texts):,} samples...", end='', flush=True)
    scores = predict_batch(texts, tokenizer, model, device)
    print(" done")

    stats = {
        'total': len(items),
        'unchanged': 0,
        'model_confident_flip': 0,   # model đủ confident → đổi nhãn
        'ensemble_flip': 0,          # vùng uncertain, rule đồng thuận → đổi nhãn
        'kept_uncertain': 0,         # uncertain + rule không đồng thuận → giữ nguyên
        'toxic_to_clean': 0,
        'clean_to_toxic': 0,
    }

    relabeled = []
    for text, old_label, score in zip(texts, old_labels, scores):
        # Quyết định nhãn mới
        if score >= high_conf:
            new_label = 1
            method = 'model'
        elif score <= low_conf:
            new_label = 0
            method = 'model'
        else:
            # Vùng uncertain: dùng rule-based làm tiebreaker
            rb = rule_based_label(text)
            new_label = rb
            method = 'ensemble'

        # Thống kê
        if new_label == old_label:
            stats['unchanged'] += 1
        else:
            if method == 'model':
                stats['model_confident_flip'] += 1
            else:
                stats['ensemble_flip'] += 1
            if old_label == 1 and new_label == 0:
                stats['toxic_to_clean'] += 1
            else:
                stats['clean_to_toxic'] += 1

        relabeled.append({
            'text': text,
            'label': new_label,
            'toxic_score': round(score, 4),
            'method': method,
        })

    if not dry_run:
        out_path = input_path.replace('.json', '_relabeled.json')
        with open(out_path, 'w', encoding='utf-8') as f:
            json.dump(relabeled, f, ensure_ascii=False, indent=2)
        print(f"  Saved: {out_path}")

    return stats


# ============================================================================
# MAIN
# ============================================================================

def main():
    import os
    parser = argparse.ArgumentParser()
    parser.add_argument('--model-dir', default=None, help='Đường dẫn model (mặc định: MODEL_PATH trong .env)')
    parser.add_argument('--models-root', default='models', help='Thư mục chứa các model (fallback khi không có .env)')
    parser.add_argument('--data-dir', default='data', help='Thư mục chứa JSON files')
    parser.add_argument('--high-conf', type=float, default=0.70, help='Ngưỡng model confident → toxic (default 0.70)')
    parser.add_argument('--low-conf', type=float, default=0.30, help='Ngưỡng model confident → clean (default 0.30)')
    parser.add_argument('--batch-size', type=int, default=64, help='Batch size inference (default 64)')
    parser.add_argument('--dry-run', action='store_true', help='Chỉ thống kê, không ghi file')
    args = parser.parse_args()

    print("=" * 60)
    print("PhoBERT + Rule-based Ensemble Relabeler")
    print(f"  High confidence threshold: {args.high_conf}")
    print(f"  Low  confidence threshold: {args.low_conf}")
    print(f"  Uncertain zone → rule-based tiebreaker")
    print("=" * 60)

    # Load model: --model-dir > MODEL_PATH (.env) > auto-find
    model_dir = args.model_dir or os.getenv('MODEL_PATH') or find_best_model(args.models_root)
    print(f"\nLoading model: {model_dir}")
    tokenizer, model, device = load_phobert(model_dir)
    print("Model ready.\n")

    # Tìm file JSON (bỏ qua file đã relabeled)
    json_files = sorted(glob.glob(f'{args.data_dir}/*.json'))
    json_files = [f for f in json_files if '_relabeled' not in f]

    if not json_files:
        print(f"Không tìm thấy JSON file trong {args.data_dir}/")
        return

    print(f"{'[DRY RUN] ' if args.dry_run else ''}Processing {len(json_files)} file(s)...\n")

    total = {'total': 0, 'unchanged': 0, 'model_confident_flip': 0,
             'ensemble_flip': 0, 'kept_uncertain': 0, 'toxic_to_clean': 0, 'clean_to_toxic': 0}

    for fp in json_files:
        print(f"[{json_files.index(fp)+1}/{len(json_files)}] {fp}")
        stats = relabel_file(fp, tokenizer, model, device,
                             args.high_conf, args.low_conf, args.dry_run)
        if not stats:
            continue

        changed = stats['model_confident_flip'] + stats['ensemble_flip']
        print(f"  Total={stats['total']:,} | Unchanged={stats['unchanged']:,} | "
              f"Changed={changed:,} "
              f"(model={stats['model_confident_flip']:,}, ensemble={stats['ensemble_flip']:,})")
        print(f"  Toxic→Clean={stats['toxic_to_clean']:,} | Clean→Toxic={stats['clean_to_toxic']:,}\n")

        for k in total:
            total[k] += stats.get(k, 0)

    n = total['total']
    changed = total['model_confident_flip'] + total['ensemble_flip']
    print("=" * 60)
    print("SUMMARY")
    print(f"  Total samples      : {n:,}")
    print(f"  Unchanged          : {total['unchanged']:,} ({total['unchanged']/max(n,1)*100:.1f}%)")
    print(f"  Changed (model)    : {total['model_confident_flip']:,} ({total['model_confident_flip']/max(n,1)*100:.1f}%)")
    print(f"  Changed (ensemble) : {total['ensemble_flip']:,} ({total['ensemble_flip']/max(n,1)*100:.1f}%)")
    print(f"  Total changed      : {changed:,} ({changed/max(n,1)*100:.1f}%)")
    print(f"  Toxic → Clean      : {total['toxic_to_clean']:,}")
    print(f"  Clean → Toxic      : {total['clean_to_toxic']:,}")
    print("=" * 60)

    if args.dry_run:
        print("\n[DRY RUN] Không ghi file. Bỏ --dry-run để áp dụng.")
    else:
        print("\nDone! Dùng file *_relabeled.json trong train_model_v2.py")


if __name__ == '__main__':
    main()
