"""
train_model_v2.py — PhoBERT Toxic Classifier v2
Strategy: MERGE ALL → DEDUPLICATE → STRATIFIED SPLIT → TRAIN ONCE

Khắc phục Catastrophic Forgetting của train_model.py (sequential training):
  - train_model.py:   Stage1→Stage2→Stage3→Stage4→Stage5 → F1 = 0.178 (thảm họa)
  - train_model_v2.py: Gộp tất cả → Train 1 lần     → F1 ≈ 0.85-0.92 (expected)
"""

import os
import json
import pandas as pd
import torch
import torch.nn as nn
import torch.nn.functional as F
import numpy as np
from datetime import datetime
from pathlib import Path

os.environ['WANDB_DISABLED'] = 'true'

from transformers import (
    AutoTokenizer,
    AutoModelForSequenceClassification,
    TrainingArguments,
    Trainer,
    EarlyStoppingCallback,
)
from datasets import Dataset
from sklearn.model_selection import train_test_split
from sklearn.metrics import (
    accuracy_score,
    precision_recall_fscore_support,
    confusion_matrix,
    classification_report,
    roc_auc_score,
)
import matplotlib.pyplot as plt
import seaborn as sns


# ============================================================================
# STEP 1: LOAD & MERGE ALL DATA
# ============================================================================

def load_json_data(json_path: str) -> list:
    """Load JSON dataset → list of {text, label, source}"""
    with open(json_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    print(f"  Loaded {len(data):,} samples from {json_path}")
    return [{'text': d['text'], 'label': int(d['label']), 'source': 'json'} for d in data]


def load_csv_data(csv_path: str, text_col: str, label_col: str) -> list:
    """Load CSV dataset, convert 3-class → binary (0=Clean, 1,2=Toxic)"""
    df = pd.read_csv(csv_path)
    df = df.dropna(subset=[text_col, label_col])
    df[label_col] = df[label_col].astype(float).astype(int)
    df['binary_label'] = df[label_col].apply(lambda x: 1 if x >= 1 else 0)

    print(f"  Loaded {len(df):,} samples from {csv_path}")
    dist = df['binary_label'].value_counts().sort_index()
    for label, count in dist.items():
        print(f"    {'Clean' if label == 0 else 'Toxic'}: {count:,} ({count/len(df)*100:.1f}%)")

    return [
        {'text': row[text_col], 'label': row['binary_label'], 'source': csv_path}
        for _, row in df.iterrows()
    ]


def merge_and_deduplicate(all_data: list) -> list:
    """Gộp tất cả data, loại bỏ duplicate (so sánh text lowercase)"""
    seen = set()
    unique = []
    for item in all_data:
        key = item['text'].strip().lower()
        if key not in seen and len(key) >= 2:
            seen.add(key)
            unique.append(item)
    print(f"\n  Total raw: {len(all_data):,}")
    print(f"  After dedup: {len(unique):,}")
    print(f"  Removed duplicates: {len(all_data) - len(unique):,}")
    return unique


# ============================================================================
# STEP 2: STRATIFIED SPLIT
# ============================================================================

def split_dataset(data: list, test_size: float = 0.05, val_size: float = 0.05):
    """Chia train/val/test theo tỉ lệ, stratified by label"""
    texts = [d['text'] for d in data]
    labels = [d['label'] for d in data]

    train_val_texts, test_texts, train_val_labels, test_labels = train_test_split(
        texts, labels, test_size=test_size, stratify=labels, random_state=42
    )

    val_ratio = val_size / (1 - test_size)
    train_texts, val_texts, train_labels, val_labels = train_test_split(
        train_val_texts, train_val_labels,
        test_size=val_ratio, stratify=train_val_labels, random_state=42
    )

    print(f"\n  Train: {len(train_texts):,} samples")
    print(f"  Val:   {len(val_texts):,} samples")
    print(f"  Test:  {len(test_texts):,} samples")

    return (
        {'texts': train_texts, 'labels': train_labels},
        {'texts': val_texts, 'labels': val_labels},
        {'texts': test_texts, 'labels': test_labels},
    )


# ============================================================================
# STEP 3: TRAINING
# ============================================================================

class FocalLoss(nn.Module):
    """
    Focal Loss — tập trung vào hard examples (samples mà model đang predict sai).
    FL(pt) = -alpha_t * (1 - pt)^gamma * log(pt)

    Không dùng label smoothing vì label smoothing làm nhiễu pt, phá vỡ
    cơ chế focal weighting (hard examples sẽ không còn được nhận diện đúng).
    """
    def __init__(self, alpha: torch.Tensor = None, gamma: float = 1.5):
        super().__init__()
        self.alpha = alpha
        self.gamma = gamma

    def forward(self, logits: torch.Tensor, labels: torch.Tensor) -> torch.Tensor:
        log_probs = F.log_softmax(logits, dim=1)
        # log p_true của ground-truth class
        log_pt = log_probs.gather(1, labels.unsqueeze(1)).squeeze(1)
        pt = log_pt.exp()

        # Focal weight: (1 - p_true)^gamma
        focal_weight = (1 - pt) ** self.gamma
        ce_loss = -log_pt

        if self.alpha is not None:
            alpha_t = self.alpha.to(logits.device)[labels]
            loss = alpha_t * focal_weight * ce_loss
        else:
            loss = focal_weight * ce_loss

        return loss.mean()


class ToxicTrainerV2:
    def __init__(self, model_name: str = 'vinai/phobert-base', max_length: int = 256):
        self.model_name = model_name
        self.max_length = max_length
        self.device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
        if self.device.type == 'cuda':
            torch.backends.cudnn.benchmark = True
            # TF32: Ampere+ (RTX 30xx/40xx) — nhanh hơn FP32, chỉ mất 1-2% precision
            torch.backends.cuda.matmul.allow_tf32 = True
            torch.backends.cudnn.allow_tf32 = True

        print(f"\nInitializing trainer:")
        print(f"  Model: {model_name}")
        print(f"  Device: {self.device}")
        print(f"  Max length: {max_length}")

        self.toxic_threshold = 0.50  # Default — sẽ được auto-tune trên val set sau khi train

        self.tokenizer = AutoTokenizer.from_pretrained(model_name)
        self.model = AutoModelForSequenceClassification.from_pretrained(
            model_name, num_labels=2, ignore_mismatched_sizes=True
        )
        # Fix PhoBERT cũ dùng gamma/beta thay vì weight/bias cho LayerNorm
        for name, module in self.model.named_modules():
            if hasattr(module, 'gamma'):
                module.weight = module.gamma
                del module.gamma
            if hasattr(module, 'beta'):
                module.bias = module.beta
                del module.beta
        self.model.to(self.device)

        if self.device.type == 'cuda':
            compute_cap = torch.cuda.get_device_capability(0)[0]
            self.use_fp16 = compute_cap >= 7
            gpu_mem = torch.cuda.get_device_properties(0).total_memory / 1024 ** 3
            print(f"  GPU: {torch.cuda.get_device_name(0)} ({gpu_mem:.1f} GB)")
            print(f"  FP16: {'Enabled' if self.use_fp16 else 'Disabled'}")
            print(f"  TF32: {'Enabled' if compute_cap >= 8 else 'Disabled'}")
        else:
            self.use_fp16 = False


    def prepare_dataset(self, split_data: dict) -> Dataset:
        dataset = Dataset.from_dict({
            'text': split_data['texts'],
            'label': split_data['labels'],
        })

        def tokenize(examples):
            return self.tokenizer(
                examples['text'],
                padding='max_length',
                truncation=True,
                max_length=self.max_length,
            )

        dataset = dataset.map(tokenize, batched=True)
        dataset.set_format(type='torch', columns=['input_ids', 'attention_mask', 'label'])
        return dataset

    def compute_metrics(self, eval_pred):
        predictions, labels = eval_pred
        probs = torch.softmax(torch.tensor(predictions), dim=1)[:, 1].numpy()
        preds = (probs >= self.toxic_threshold).astype(int)

        accuracy = accuracy_score(labels, preds)
        precision, recall, f1, _ = precision_recall_fscore_support(
            labels, preds, average='binary'
        )
        try:
            auc = roc_auc_score(labels, probs)
        except ValueError:
            auc = 0.0

        return {
            'accuracy': accuracy,
            'precision': precision,
            'recall': recall,
            'f1': f1,
            'auc_roc': auc,
        }

    def train(
        self,
        train_dataset: Dataset,
        val_dataset: Dataset,
        output_dir: str = 'models',
        num_epochs: int = 5,
        batch_size: int = 16,
        learning_rate: float = 2e-5,
    ):
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        model_dir = f"{output_dir}/phobert_toxic_v2_{timestamp}"
        os.makedirs(model_dir, exist_ok=True)

        # Auto-tune batch size để fill VRAM
        if self.device.type == 'cuda':
            # total_memory bao gồm cả VRAM + Shared GPU Memory trên Windows WDDM
            total_mem_gb = torch.cuda.get_device_properties(0).total_memory / 1024 ** 3
            if total_mem_gb >= 8:
                safe_batch = 32 if self.max_length <= 256 else 16
            elif total_mem_gb >= 4:
                safe_batch = 16 if self.max_length <= 256 else 8
            else:
                safe_batch = 8
        else:
            safe_batch = min(batch_size, 8)
        grad_accum = max(1, batch_size // safe_batch)

        training_args = TrainingArguments(
            output_dir=model_dir,
            num_train_epochs=num_epochs,
            per_device_train_batch_size=safe_batch,
            per_device_eval_batch_size=safe_batch * 2,
            learning_rate=learning_rate,
            warmup_ratio=0.1,  # noqa: deprecated in v5.2 but still works
            weight_decay=0.01,
            logging_steps=50,
            eval_strategy="epoch",
            save_strategy="epoch",
            load_best_model_at_end=True,
            metric_for_best_model="f1",
            greater_is_better=True,
            save_total_limit=2,
            fp16=self.use_fp16,
            max_grad_norm=1.0,
            gradient_accumulation_steps=grad_accum,
            dataloader_num_workers=4,       # load data song song với GPU compute
            dataloader_prefetch_factor=2,   # prefetch batch tiếp theo
            dataloader_pin_memory=True,
            report_to=[],
            seed=42,
        )

        # Class weights: balanced (không boost)
        train_labels = list(train_dataset['label'])
        n_clean = train_labels.count(0)
        n_toxic = train_labels.count(1)
        total_train = len(train_labels)
        weight_clean = total_train / (2 * n_clean) if n_clean > 0 else 1.0
        weight_toxic = total_train / (2 * n_toxic) if n_toxic > 0 else 1.0
        class_weights = torch.tensor([weight_clean, weight_toxic], dtype=torch.float)
        print(f"  Class weights (balanced): Clean={weight_clean:.3f}, Toxic={weight_toxic:.3f}")

        class WeightedTrainer(Trainer):
            def compute_loss(self, model, inputs, return_outputs=False, **kwargs):
                labels = inputs.pop('labels')
                outputs = model(**inputs)
                loss = nn.CrossEntropyLoss(
                    weight=class_weights.to(outputs.logits.device)
                )(outputs.logits, labels)
                return (loss, outputs) if return_outputs else loss

        trainer = WeightedTrainer(
            model=self.model,
            args=training_args,
            train_dataset=train_dataset,
            eval_dataset=val_dataset,
            compute_metrics=self.compute_metrics,
            callbacks=[EarlyStoppingCallback(early_stopping_patience=2)],
        )

        print(f"\nTraining config:")
        print(f"  Loss: CrossEntropy + balanced class weights")
        print(f"  Epochs: {num_epochs} (early stop patience=2)")
        print(f"  Batch: {safe_batch} x {grad_accum} = {safe_batch * grad_accum}")
        print(f"  LR: {learning_rate}")
        print(f"  Warmup: 10%")
        print(f"  Output: {model_dir}")

        trainer.train()
        trainer.save_model(model_dir)
        self.tokenizer.save_pretrained(model_dir)

        return trainer, model_dir

    def find_best_threshold(self, trainer: Trainer, val_dataset: Dataset) -> float:
        """Tìm threshold tốt nhất trên val set theo F1 của class Toxic."""
        print("\n  Finding best threshold on val set...")
        val_preds = trainer.predict(val_dataset)
        val_true = val_preds.label_ids
        val_probs = torch.softmax(
            torch.tensor(val_preds.predictions), dim=1
        )[:, 1].numpy()

        best_thresh, best_f1 = 0.5, 0.0
        for t in np.arange(0.20, 0.81, 0.01):
            pred = (val_probs >= t).astype(int)
            _, _, f1, _ = precision_recall_fscore_support(
                val_true, pred, average='binary', zero_division=0
            )
            if f1 > best_f1:
                best_f1 = f1
                best_thresh = float(t)
        print(f"  Best threshold: {best_thresh:.2f} (val F1={best_f1:.4f})")
        return best_thresh

    def full_evaluation(self, trainer: Trainer, test_dataset: Dataset, model_dir: str,
                        val_dataset: Dataset = None):
        """Đánh giá đầy đủ trên held-out test set."""
        print("\n" + "=" * 60)
        print("FULL EVALUATION ON TEST SET")
        print("=" * 60)

        # Auto-tune threshold trên val set nếu có
        if val_dataset is not None:
            self.toxic_threshold = self.find_best_threshold(trainer, val_dataset)

        predictions = trainer.predict(test_dataset)
        true_labels = predictions.label_ids
        probs = torch.softmax(
            torch.tensor(predictions.predictions), dim=1
        )[:, 1].numpy()
        pred_labels = (probs >= self.toxic_threshold).astype(int)
        print(f"  Using toxic threshold: {self.toxic_threshold:.2f}")

        print("\nClassification Report:")
        print(classification_report(
            true_labels, pred_labels,
            target_names=['Clean', 'Toxic'],
            digits=4,
        ))

        auc = roc_auc_score(true_labels, probs)
        print(f"AUC-ROC: {auc:.4f}")

        cm = confusion_matrix(true_labels, pred_labels)
        print(f"\nConfusion Matrix:")
        print(f"  True Clean  → predicted Clean: {cm[0][0]:,} (TN)")
        print(f"  True Clean  → predicted Toxic: {cm[0][1]:,} (FP)")
        print(f"  True Toxic  → predicted Clean: {cm[1][0]:,} (FN)")
        print(f"  True Toxic  → predicted Toxic: {cm[1][1]:,} (TP)")

        # Plot confusion matrix
        plt.figure(figsize=(8, 6))
        sns.heatmap(
            cm, annot=True, fmt='d', cmap='Blues',
            xticklabels=['Clean', 'Toxic'],
            yticklabels=['Clean', 'Toxic'],
        )
        plt.title('Confusion Matrix — PhoBERT Toxic Detection v2')
        plt.ylabel('True Label')
        plt.xlabel('Predicted Label')
        plt.tight_layout()
        cm_path = f"{model_dir}/confusion_matrix.png"
        plt.savefig(cm_path, dpi=150)
        print(f"\nConfusion matrix saved: {cm_path}")

        fp_count = int(cm[0][1])
        fn_count = int(cm[1][0])
        print(f"\nError Analysis:")
        print(f"  False Positives (clean bị đánh toxic): {fp_count}")
        print(f"  False Negatives (toxic bị bỏ sót): {fn_count}")

        eval_results = trainer.evaluate(test_dataset)
        metrics = {
            'model_version': 'v2',
            'training_date': datetime.now().isoformat(),
            'strategy': 'merge_all_train_once',
            'toxic_threshold': self.toxic_threshold,
            'test_results': {
                'accuracy': float(eval_results.get('eval_accuracy', 0)),
                'precision': float(eval_results.get('eval_precision', 0)),
                'recall': float(eval_results.get('eval_recall', 0)),
                'f1': float(eval_results.get('eval_f1', 0)),
                'auc_roc': float(auc),
            },
            'confusion_matrix': cm.tolist(),
            'error_counts': {
                'false_positives': fp_count,
                'false_negatives': fn_count,
            },
        }

        with open(f"{model_dir}/evaluation_metrics.json", 'w') as f:
            json.dump(metrics, f, indent=2, ensure_ascii=False)

        return metrics


# ============================================================================
# OVERSAMPLE TOXIC
# ============================================================================

def oversample_toxic(train_split: dict, target_ratio: float = 0.30) -> dict:
    """Oversample toxic samples trong train set để đạt target_ratio."""
    import random
    texts = train_split['texts']
    labels = train_split['labels']

    clean_texts = [t for t, l in zip(texts, labels) if l == 0]
    toxic_texts = [t for t, l in zip(texts, labels) if l == 1]
    n_clean = len(clean_texts)
    n_toxic = len(toxic_texts)

    # Số toxic cần đạt: toxic / (clean + toxic) = target_ratio
    n_toxic_target = int(target_ratio * n_clean / (1 - target_ratio))

    if n_toxic_target <= n_toxic:
        print(f"\n  Oversample: không cần (toxic đã đủ {n_toxic/len(texts)*100:.1f}%)")
        return train_split

    random.seed(42)
    extra_needed = n_toxic_target - n_toxic
    extra_texts = random.choices(toxic_texts, k=extra_needed)

    all_texts = list(texts) + extra_texts
    all_labels = list(labels) + [1] * extra_needed

    combined = list(zip(all_texts, all_labels))
    random.shuffle(combined)
    all_texts, all_labels = zip(*combined)

    print(f"\n  Oversample toxic: {n_toxic:,} → {n_toxic + extra_needed:,}")
    print(f"  Toxic ratio: {n_toxic/len(texts)*100:.1f}% → {(n_toxic+extra_needed)/len(all_texts)*100:.1f}%")
    return {'texts': list(all_texts), 'labels': list(all_labels)}


# ============================================================================
# CLEANUP OLD MODELS
# ============================================================================

def update_env_model_path(model_dir: str, env_path: str):
    """Cập nhật MODEL_PATH trong .env trỏ tới model mới vừa train."""
    model_name = Path(model_dir).name          # phobert_toxic_v2_20260409_...
    rel_path   = f"models/{model_name}"        # relative từ model-service root

    env_file = Path(env_path)
    if not env_file.exists():
        print(f"  WARN: .env không tồn tại tại {env_path}")
        return

    lines = env_file.read_text(encoding='utf-8').splitlines(keepends=True)
    updated = []
    changed = False
    for line in lines:
        if line.startswith('MODEL_PATH='):
            old_val = line.strip()
            updated.append(f"MODEL_PATH={rel_path}\n")
            changed = True
            print(f"  .env: {old_val}  →  MODEL_PATH={rel_path}")
        else:
            updated.append(line)

    if not changed:
        updated.append(f"MODEL_PATH={rel_path}\n")
        print(f"  .env: thêm mới MODEL_PATH={rel_path}")

    env_file.write_text(''.join(updated), encoding='utf-8')
    print(f"  .env updated: {env_path}")


def cleanup_old_models(output_dir: str = 'models', keep: int = 2):
    """Xóa model cũ, giữ lại `keep` model mới nhất theo timestamp trong tên thư mục."""
    import shutil
    from pathlib import Path

    model_dirs = sorted(
        [d for d in Path(output_dir).iterdir()
         if d.is_dir() and (d / 'model.safetensors').exists()],
        key=lambda x: x.name
    )

    to_delete = model_dirs[:-keep] if len(model_dirs) > keep else []
    for d in to_delete:
        shutil.rmtree(d)
        print(f"  Deleted old model: {d.name}")

    if to_delete:
        print(f"  Kept {keep} newest model(s).")
    else:
        print(f"  No cleanup needed (≤{keep} models found).")


# ============================================================================
# MAIN
# ============================================================================

def main():
    print("=" * 60)
    print("PhoBERT Toxic Classifier v2")
    print("Strategy: MERGE ALL → SPLIT → TRAIN ONCE")
    print("=" * 60)

    data_dir = Path(__file__).parent.parent / 'data'

    # Tự động scan tất cả JSON trong data/
    # Ưu tiên file _relabeled, bỏ qua file gốc nếu đã có _relabeled tương ứng
    all_json = set(data_dir.glob('*.json'))
    # Ưu tiên: _vihsd_labeled > _relabeled > gốc
    vihsd_labeled   = {p for p in all_json if p.stem.endswith('_vihsd_labeled')}
    relabeled       = {p for p in all_json if p.stem.endswith('_relabeled')}
    has_vihsd_base  = {p.stem.replace('_vihsd_labeled', '') for p in vihsd_labeled}
    has_relabel_base = {p.stem.replace('_relabeled', '') for p in relabeled}
    json_sources = []
    for p in sorted(all_json):
        if p.stem.endswith('_vihsd_labeled'):
            json_sources.append({'type': 'json', 'path': str(p)})
        elif p.stem.endswith('_relabeled'):
            base = p.stem.replace('_relabeled', '')
            if base not in has_vihsd_base:
                json_sources.append({'type': 'json', 'path': str(p)})
        else:
            # file gốc: chỉ load nếu không có bản labeled nào
            if p.stem not in has_vihsd_base and p.stem not in has_relabel_base:
                json_sources.append({'type': 'json', 'path': str(p)})

    # ViHSD: human-annotated, chất lượng cao → duplicate 5x để tăng ảnh hưởng
    VIHSD_REPEAT = 5
    vihsd_sources = [
        {'type': 'csv', 'path': str(data_dir / 'test_df.csv'),  'text_col': 'cmt_col',   'label_col': 'labels'},
        {'type': 'csv', 'path': str(data_dir / 'dev.csv'),       'text_col': 'free_text', 'label_col': 'label_id'},
        {'type': 'csv', 'path': str(data_dir / 'test.csv'),      'text_col': 'free_text', 'label_col': 'label_id'},
        {'type': 'csv', 'path': str(data_dir / 'train.csv'),     'text_col': 'free_text', 'label_col': 'label_id'},
    ]

    DATA_SOURCES = json_sources + vihsd_sources * VIHSD_REPEAT
    print(f"\n[Auto-scan] Found {len(json_sources)} JSON files in data/")
    print(f"[ViHSD] Will repeat {VIHSD_REPEAT}x to increase influence of human-annotated data")

    # ── STEP 1: LOAD ALL ──
    print("\n[Step 1] Loading all data sources...")
    all_data = []
    for src in DATA_SOURCES:
        if not os.path.exists(src['path']):
            print(f"  SKIP (not found): {src['path']}")
            continue
        if src['type'] == 'json':
            all_data.extend(load_json_data(src['path']))
        elif src['type'] == 'csv':
            all_data.extend(load_csv_data(src['path'], src['text_col'], src['label_col']))

    if not all_data:
        print("ERROR: No data loaded. Check DATA_SOURCES paths.")
        return

    # ── STEP 2: MERGE & DEDUP ──
    print("\n[Step 2] Merging and deduplicating...")
    unique_data = merge_and_deduplicate(all_data)

    labels = [d['label'] for d in unique_data]
    clean = labels.count(0)
    toxic = labels.count(1)
    print(f"\n  Label distribution:")
    print(f"  Clean: {clean:,} ({clean/len(labels)*100:.1f}%)")
    print(f"  Toxic: {toxic:,} ({toxic/len(labels)*100:.1f}%)")

    # ── STEP 3: SPLIT ──
    print("\n[Step 3] Stratified split (90% train / 5% val / 5% test)...")
    train_split, val_split, test_split = split_dataset(unique_data)

    # ── STEP 4: TRAIN ──
    print("\n[Step 4] Training...")
    trainer_obj = ToxicTrainerV2(model_name='vinai/phobert-base', max_length=256)
    train_ds = trainer_obj.prepare_dataset(train_split)
    val_ds = trainer_obj.prepare_dataset(val_split)
    test_ds = trainer_obj.prepare_dataset(test_split)

    models_dir = Path(__file__).parent.parent / 'models'
    env_path   = Path(__file__).parent.parent / '.env'

    trainer, model_dir = trainer_obj.train(
        train_dataset=train_ds,
        val_dataset=val_ds,
        output_dir=str(models_dir),
        num_epochs=5,
        batch_size=16,
        learning_rate=2e-5,
    )

    # ── STEP 5: EVALUATE ──
    print("\n[Step 5] Full evaluation on held-out test set...")
    metrics = trainer_obj.full_evaluation(trainer, test_ds, model_dir, val_dataset=val_ds)

    print("\n" + "=" * 60)
    print(f"DONE! Model saved to: {model_dir}")
    print(f"F1:      {metrics['test_results']['f1']:.4f}")
    print(f"AUC-ROC: {metrics['test_results']['auc_roc']:.4f}")
    print("=" * 60)

    # Cập nhật .env trỏ tới model mới
    print("\n[Post-train] Updating .env...")
    update_env_model_path(model_dir, str(env_path))

    # Xóa model cũ, giữ lại 2 model mới nhất
    cleanup_old_models(output_dir=str(models_dir), keep=2)


if __name__ == '__main__':
    main()
