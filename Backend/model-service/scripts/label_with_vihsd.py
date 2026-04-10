"""
scripts/label_with_vihsd.py — Train labeler từ ViHSD, dùng để label JSON scraper

Pipeline:
  1. Load toàn bộ ViHSD (train + dev + test + test_df) → train mini PhoBERT labeler
  2. Dùng labeler đã train để label lại toàn bộ JSON scraper
  3. Lưu ra file *_vihsd_labeled.json

Ưu điểm so với relabel_with_model.py:
  - Labeler được train từ human-annotated data (không bị circular bias)
  - ViHSD có 3 class (clean/offensive/hate) → thông tin phong phú hơn binary
  - Confidence threshold để bỏ qua sample không chắc chắn

Usage:
  # Train labeler rồi label tất cả JSON
  python scripts/label_with_vihsd.py

  # Chỉ train labeler, lưu ra thư mục chỉ định
  python scripts/label_with_vihsd.py --only-train --labeler-dir models/vihsd_labeler

  # Dùng labeler đã train sẵn (bỏ qua bước train)
  python scripts/label_with_vihsd.py --labeler-dir models/vihsd_labeler

  # Dry run: xem thống kê, không ghi file
  python scripts/label_with_vihsd.py --dry-run
"""

import os
import sys
import json
import glob
import argparse
import numpy as np
from pathlib import Path

_ROOT = Path(__file__).parent.parent
sys.path.insert(0, str(_ROOT))


# ============================================================================
# STEP 1: LOAD VIHSD
# ============================================================================

def load_vihsd(data_dir: str = 'data') -> tuple[list, list]:
    """
    Load tất cả ViHSD splits, trả về (texts, labels).
    label_id: 0=clean, 1=offensive, 2=hate → binary: 0=clean, 1=toxic
    """
    import pandas as pd

    splits = [
        (f'{data_dir}/train.csv',   'free_text', 'label_id'),
        (f'{data_dir}/dev.csv',     'free_text', 'label_id'),
        (f'{data_dir}/test.csv',    'free_text', 'label_id'),
        (f'{data_dir}/test_df.csv', 'cmt_col',   'labels'),
    ]

    texts, labels = [], []
    for path, text_col, label_col in splits:
        if not os.path.exists(path):
            print(f"  SKIP (not found): {path}")
            continue
        df = pd.read_csv(path)
        df = df.dropna(subset=[text_col, label_col])
        df['bin'] = df[label_col].astype(float).astype(int).apply(lambda x: 1 if x >= 1 else 0)
        texts.extend(df[text_col].tolist())
        labels.extend(df['bin'].tolist())
        n_clean = df['bin'].eq(0).sum()
        n_toxic = df['bin'].eq(1).sum()
        print(f"  {path}: {len(df):,} rows | Clean={n_clean:,} Toxic={n_toxic:,}")

    print(f"\n  Total ViHSD: {len(texts):,} | Clean={labels.count(0):,} Toxic={labels.count(1):,}")
    return texts, labels


# ============================================================================
# STEP 2: TRAIN VIHSD LABELER
# ============================================================================

def train_vihsd_labeler(
    texts: list,
    labels: list,
    labeler_dir: str,
    model_name: str = 'vinai/phobert-base',
    num_epochs: int = 5,
    batch_size: int = 8,
    max_length: int = 256,
):
    """Fine-tune PhoBERT trên ViHSD, lưu model ra labeler_dir."""
    import torch
    import torch.nn as nn
    from transformers import (
        AutoTokenizer, AutoModelForSequenceClassification,
        TrainingArguments, Trainer, EarlyStoppingCallback,
    )
    from datasets import Dataset
    from sklearn.model_selection import train_test_split
    from sklearn.metrics import accuracy_score, precision_recall_fscore_support

    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    print(f"\n  Device: {device}")
    if device.type == 'cuda':
        gpu_mem = torch.cuda.get_device_properties(0).total_memory / 1024 ** 3
        print(f"  GPU: {torch.cuda.get_device_name(0)} ({gpu_mem:.1f} GB)")

    # Train/val split từ ViHSD
    tr_texts, val_texts, tr_labels, val_labels = train_test_split(
        texts, labels, test_size=0.1, stratify=labels, random_state=42
    )
    print(f"  Train: {len(tr_texts):,} | Val: {len(val_texts):,}")

    tokenizer = AutoTokenizer.from_pretrained(model_name)
    model = AutoModelForSequenceClassification.from_pretrained(
        model_name, num_labels=2, ignore_mismatched_sizes=True
    )
    # Fix PhoBERT gamma/beta → weight/bias
    for _, module in model.named_modules():
        if hasattr(module, 'gamma'):
            module.weight = module.gamma
            del module.gamma
        if hasattr(module, 'beta'):
            module.bias = module.beta
            del module.beta
    model.to(device)

    def tokenize(examples):
        return tokenizer(
            examples['text'], padding='max_length',
            truncation=True, max_length=max_length,
        )

    def make_ds(t, l):
        ds = Dataset.from_dict({'text': t, 'label': l})
        ds = ds.map(tokenize, batched=True)
        ds.set_format(type='torch', columns=['input_ids', 'attention_mask', 'label'])
        return ds

    train_ds = make_ds(tr_texts, tr_labels)
    val_ds   = make_ds(val_texts, val_labels)

    # Class weights cho imbalance
    n_clean = tr_labels.count(0)
    n_toxic = tr_labels.count(1)
    total   = len(tr_labels)
    w_clean = total / (2 * n_clean) if n_clean else 1.0
    w_toxic = total / (2 * n_toxic) if n_toxic else 1.0
    class_weights = torch.tensor([w_clean, w_toxic], dtype=torch.float).to(device)
    print(f"  Class weights: Clean={w_clean:.3f} Toxic={w_toxic:.3f}")

    def compute_metrics(eval_pred):
        preds = np.argmax(eval_pred.predictions, axis=1)
        labels_ = eval_pred.label_ids
        acc = accuracy_score(labels_, preds)
        p, r, f1, _ = precision_recall_fscore_support(labels_, preds, average='binary')
        return {'accuracy': acc, 'precision': p, 'recall': r, 'f1': f1}

    use_fp16 = device.type == 'cuda' and torch.cuda.get_device_capability(0)[0] >= 7
    grad_accum = max(1, 16 // batch_size)

    args = TrainingArguments(
        output_dir=labeler_dir,
        num_train_epochs=num_epochs,
        per_device_train_batch_size=batch_size,
        per_device_eval_batch_size=batch_size * 2,
        learning_rate=2e-5,
        warmup_ratio=0.1,
        weight_decay=0.01,
        fp16=use_fp16,
        max_grad_norm=1.0,
        gradient_accumulation_steps=grad_accum,
        logging_steps=50,
        eval_strategy='epoch',
        save_strategy='epoch',
        load_best_model_at_end=True,
        metric_for_best_model='f1',
        greater_is_better=True,
        save_total_limit=1,
        report_to=[],
        seed=42,
    )

    class WeightedTrainer(Trainer):
        def compute_loss(self, model, inputs, return_outputs=False, **kwargs):
            lbl = inputs.pop('labels')
            out = model(**inputs)
            loss = nn.CrossEntropyLoss(weight=class_weights)(out.logits, lbl)
            return (loss, out) if return_outputs else loss

    trainer = WeightedTrainer(
        model=model,
        args=args,
        train_dataset=train_ds,
        eval_dataset=val_ds,
        compute_metrics=compute_metrics,
        callbacks=[EarlyStoppingCallback(early_stopping_patience=2)],
    )

    print(f"\n  Training ViHSD labeler ({num_epochs} epochs)...")
    trainer.train()

    os.makedirs(labeler_dir, exist_ok=True)
    trainer.save_model(labeler_dir)
    tokenizer.save_pretrained(labeler_dir)
    print(f"  Labeler saved: {labeler_dir}")

    return tokenizer, model, device


# ============================================================================
# STEP 3: LABEL JSON SCRAPER DATA
# ============================================================================

def load_labeler(labeler_dir: str):
    """Load model đã train."""
    import torch
    from transformers import AutoTokenizer, AutoModelForSequenceClassification

    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    tokenizer = AutoTokenizer.from_pretrained(labeler_dir)
    model = AutoModelForSequenceClassification.from_pretrained(labeler_dir)
    model.to(device)
    model.eval()
    print(f"  Loaded labeler from {labeler_dir} | Device: {device}")
    return tokenizer, model, device


def predict_scores(texts: list, tokenizer, model, device, batch_size: int = 64) -> list:
    """Trả về list[(toxic_prob, clean_prob)] cho từng text."""
    import torch

    all_probs = []
    for i in range(0, len(texts), batch_size):
        chunk = texts[i:i + batch_size]
        inputs = tokenizer(
            chunk, padding=True, truncation=True,
            max_length=256, return_tensors='pt',
        )
        inputs = {k: v.to(device) for k, v in inputs.items()}
        with torch.inference_mode():
            probs = torch.softmax(model(**inputs).logits, dim=1).cpu()
        all_probs.extend(probs.tolist())
    return all_probs


def normalize_items(data) -> list:
    if isinstance(data, list):
        return [
            {'text': (it.get('text') or it.get('content', '')).strip()}
            for it in data if isinstance(it, dict)
            if (it.get('text') or it.get('content', '')).strip()
        ]
    if isinstance(data, dict) and 'comments' in data:
        return [
            {'text': (c.get('content') or c.get('text', '')).strip()}
            for c in data['comments']
            if (c.get('content') or c.get('text', '')).strip()
        ]
    return []


def label_file(
    input_path: str,
    tokenizer, model, device,
    confidence: float,
    dry_run: bool,
) -> dict:
    with open(input_path, 'r', encoding='utf-8') as f:
        raw = json.load(f)

    items = normalize_items(raw)
    if not items:
        print("  SKIP: format không nhận dạng được")
        return {}

    texts = [it['text'] for it in items]
    print(f"  Labeling {len(texts):,} samples...", end='', flush=True)
    all_probs = predict_scores(texts, tokenizer, model, device)
    print(" done")

    labeled = []
    stats = {'total': len(texts), 'toxic': 0, 'clean': 0, 'skipped': 0}

    for text, probs in zip(texts, all_probs):
        clean_prob, toxic_prob = probs[0], probs[1]
        confidence_score = max(clean_prob, toxic_prob)

        if confidence_score < confidence:
            # Không đủ confident → skip sample này khỏi dataset
            stats['skipped'] += 1
            continue

        label = 1 if toxic_prob > clean_prob else 0
        stats['toxic' if label == 1 else 'clean'] += 1
        labeled.append({
            'text': text,
            'label': label,
            'toxic_score': round(toxic_prob, 4),
            'confidence': round(confidence_score, 4),
        })

    kept = stats['total'] - stats['skipped']
    print(f"  Kept={kept:,}/{stats['total']:,} | Toxic={stats['toxic']:,} Clean={stats['clean']:,} Skipped={stats['skipped']:,}")

    if not dry_run and labeled:
        out_path = input_path.replace('.json', '_vihsd_labeled.json')
        with open(out_path, 'w', encoding='utf-8') as f:
            json.dump(labeled, f, ensure_ascii=False, indent=2)
        print(f"  Saved: {out_path}")

    return stats


# ============================================================================
# MAIN
# ============================================================================

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--labeler-dir', default='models/vihsd_labeler',
                        help='Thư mục lưu/load labeler model (default: models/vihsd_labeler)')
    parser.add_argument('--data-dir', default='data',
                        help='Thư mục chứa JSON files cần label')
    parser.add_argument('--confidence', type=float, default=0.75,
                        help='Ngưỡng confidence tối thiểu để giữ sample (default: 0.75)')
    parser.add_argument('--epochs', type=int, default=5,
                        help='Số epoch train labeler (default: 5)')
    parser.add_argument('--batch-size', type=int, default=8,
                        help='Batch size khi train (default: 8)')
    parser.add_argument('--only-train', action='store_true',
                        help='Chỉ train labeler, không label JSON')
    parser.add_argument('--skip-train', action='store_true',
                        help='Bỏ qua train, dùng labeler đã có ở --labeler-dir')
    parser.add_argument('--dry-run', action='store_true',
                        help='Không ghi file output')
    args = parser.parse_args()

    os.environ['WANDB_DISABLED'] = 'true'

    print("=" * 60)
    print("ViHSD-based Labeler")
    print(f"  Labeler dir : {args.labeler_dir}")
    print(f"  Confidence  : {args.confidence}")
    print("=" * 60)

    labeler_exists = (Path(args.labeler_dir) / 'model.safetensors').exists()

    # ── TRAIN ──
    if args.skip_train and not labeler_exists:
        print(f"\nERROR: --skip-train nhưng không tìm thấy labeler tại {args.labeler_dir}")
        return

    if not args.skip_train:
        if labeler_exists:
            print(f"\nLabeler đã tồn tại tại {args.labeler_dir}, bỏ qua train.")
            print("Dùng --skip-train để dùng lại, hoặc xóa thư mục để train lại.")
            tokenizer, model, device = load_labeler(args.labeler_dir)
        else:
            print("\n[Step 1] Loading ViHSD...")
            texts, labels = load_vihsd(args.data_dir)

            print("\n[Step 2] Training ViHSD labeler...")
            tokenizer, model, device = train_vihsd_labeler(
                texts, labels,
                labeler_dir=args.labeler_dir,
                num_epochs=args.epochs,
                batch_size=args.batch_size,
            )
    else:
        print(f"\nSkipping train, loading labeler from {args.labeler_dir}...")
        tokenizer, model, device = load_labeler(args.labeler_dir)

    if args.only_train:
        print("\nDone. (--only-train, bỏ qua bước label JSON)")
        return

    # ── LABEL JSON FILES ──
    print(f"\n[Step 3] Labeling JSON scraper files in {args.data_dir}/...")
    json_files = sorted(glob.glob(f'{args.data_dir}/*.json'))
    # Bỏ qua file đã relabeled/labeled và file ViHSD (không có JSON)
    json_files = [
        f for f in json_files
        if '_relabeled' not in f
        and '_vihsd_labeled' not in f
    ]

    if not json_files:
        print(f"Không tìm thấy JSON file trong {args.data_dir}/")
        return

    print(f"Found {len(json_files)} files to label.\n")

    total = {'total': 0, 'toxic': 0, 'clean': 0, 'skipped': 0}
    for i, fp in enumerate(json_files, 1):
        print(f"[{i}/{len(json_files)}] {fp}")
        stats = label_file(fp, tokenizer, model, device, args.confidence, args.dry_run)
        for k in total:
            total[k] += stats.get(k, 0)
        print()

    kept = total['total'] - total['skipped']
    print("=" * 60)
    print("SUMMARY")
    print(f"  Total processed : {total['total']:,}")
    print(f"  Kept            : {kept:,} ({kept/max(total['total'],1)*100:.1f}%)")
    print(f"  Toxic           : {total['toxic']:,} ({total['toxic']/max(kept,1)*100:.1f}%)")
    print(f"  Clean           : {total['clean']:,} ({total['clean']/max(kept,1)*100:.1f}%)")
    print(f"  Skipped (low confidence): {total['skipped']:,}")
    print("=" * 60)

    if args.dry_run:
        print("\n[DRY RUN] Không ghi file. Bỏ --dry-run để áp dụng.")
    else:
        print("\nDone! Dùng file *_vihsd_labeled.json trong train_model_v2.py")
        print("Cập nhật DATA_SOURCES để load *_vihsd_labeled.json thay vì *_relabeled.json")


if __name__ == '__main__':
    main()
