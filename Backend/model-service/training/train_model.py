"""
Bước 2: Train PhoBERT Model cho Toxic Comment Detection
SEQUENTIAL TRAINING với 100% DATA từ mỗi file - CHỈ LƯU 1 MODEL FINAL
"""

import os
import json
import pandas as pd
import torch
import torch.nn as nn
import numpy as np
from datetime import datetime
from pathlib import Path

# ✅ DISABLE WANDB NGAY TỪ ĐẦU
os.environ['WANDB_DISABLED'] = 'true'

from transformers import (
    AutoTokenizer,
    AutoModelForSequenceClassification,
    TrainingArguments,
    Trainer,
)
from datasets import Dataset
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score, precision_recall_fscore_support, confusion_matrix
import matplotlib.pyplot as plt
import seaborn as sns


def force_cuda_device():
    """FORCE sử dụng NVIDIA GPU"""
    if not torch.cuda.is_available():
        raise RuntimeError("❌ CUDA không available!")
    
    os.environ['CUDA_VISIBLE_DEVICES'] = '0'
    torch.cuda.set_device(0)
    
    print(f"\n{'='*70}")
    print(f"🎮 FORCING NVIDIA GPU")
    print(f"{'='*70}")
    
    device = torch.device('cuda:0')
    
    print(f"   Device: {device}")
    print(f"   GPU Name: {torch.cuda.get_device_name(0)}")
    print(f"   GPU Memory: {torch.cuda.get_device_properties(0).total_memory / 1024**3:.2f} GB")
    print(f"   CUDA Version: {torch.version.cuda}")
    
    print(f"\n   🧪 Testing GPU...")
    test_tensor = torch.randn(1000, 1000).cuda()
    result = test_tensor @ test_tensor
    print(f"   ✅ GPU computation successful!")
    del test_tensor, result
    torch.cuda.empty_cache()
    
    print(f"{'='*70}\n")
    
    return device


def check_and_configure_gpus():
    """Kiểm tra và cấu hình GPU"""
    device = force_cuda_device()
    
    gpu_count = 1
    gpu_name = torch.cuda.get_device_name(0)
    gpu_memory = torch.cuda.get_device_properties(0).total_memory / 1024**3
    compute_capability = torch.cuda.get_device_capability(0)
    
    gpu_info = [{
        'id': 0,
        'name': gpu_name,
        'memory_gb': gpu_memory,
        'compute_capability': compute_capability
    }]
    
    print(f"🚀 GPU CONFIGURATION")
    print(f"   GPU: {gpu_name}")
    print(f"   Memory: {gpu_memory:.2f} GB")
    print(f"   Compute: {compute_capability[0]}.{compute_capability[1]}")
    
    torch.backends.cudnn.enabled = True
    torch.backends.cudnn.benchmark = True
    torch.backends.cudnn.deterministic = False
    
    torch.cuda.empty_cache()
    
    return {
        'gpu_count': gpu_count,
        'gpu_info': gpu_info,
        'total_memory': gpu_memory,
        'strategy': 'single_gpu',
        'device': device
    }


def load_csv_full_data(csv_path, text_column='free_text', label_column='label_id'):
    """Load 100% data từ CSV file (KHÔNG split train/test)"""
    print(f"\n{'='*70}")
    print(f"📂 LOADING CSV DATA (100% - NO SPLIT)")
    print(f"{'='*70}")
    print(f"   File: {csv_path}")
    
    df = pd.read_csv(csv_path)
    print(f"   Total samples: {len(df)}")
    
    if text_column not in df.columns:
        raise ValueError(f"❌ Column '{text_column}' not found! Available: {df.columns.tolist()}")
    if label_column not in df.columns:
        raise ValueError(f"❌ Column '{label_column}' not found! Available: {df.columns.tolist()}")
    
    df = df.dropna(subset=[text_column, label_column])
    print(f"   After removing NA: {len(df)}")
    
    df[label_column] = df[label_column].astype(float).astype(int)
    
    label_dist = df[label_column].value_counts().sort_index()
    print(f"\n   📊 Original Label Distribution:")
    label_names = {0: 'Clean', 1: 'Offensive', 2: 'Toxic'}
    for label, count in label_dist.items():
        label_name = label_names.get(label, f'Label_{label}')
        percentage = (count / len(df)) * 100
        print(f"      {label} ({label_name}): {count:,} ({percentage:.1f}%)")
    
    print(f"\n   🔄 Converting labels: 1,2 (Offensive/Toxic) -> 1 (Toxic)")
    df[label_column] = df[label_column].apply(lambda x: 1 if x in [1, 2] else 0)
    
    label_dist_new = df[label_column].value_counts().sort_index()
    print(f"\n   📊 Final Label Distribution (Binary):")
    for label, count in label_dist_new.items():
        label_name = 'Clean' if label == 0 else 'Toxic'
        percentage = (count / len(df)) * 100
        print(f"      {label} ({label_name}): {count:,} ({percentage:.1f}%)")
    
    texts = df[text_column].tolist()
    labels = df[label_column].tolist()
    
    train_data = [
        {'text': text, 'label': label}
        for text, label in zip(texts, labels)
    ]
    
    print(f"\n   ✅ Loaded 100% data: {len(train_data):,} samples")
    print(f"{'='*70}\n")
    
    return train_data


def load_json_data(train_path, test_path):
    """Load data từ JSON files"""
    print(f"\n{'='*70}")
    print(f"📂 LOADING JSON DATA")
    print(f"{'='*70}")
    
    with open(train_path, 'r', encoding='utf-8') as f:
        train_data = json.load(f)
    
    with open(test_path, 'r', encoding='utf-8') as f:
        test_data = json.load(f)
    
    print(f"   Train: {len(train_data):,} samples")
    print(f"   Test:  {len(test_data):,} samples")
    
    train_labels = [d['label'] for d in train_data]
    test_labels = [d['label'] for d in test_data]
    
    train_dist = pd.Series(train_labels).value_counts().sort_index()
    test_dist = pd.Series(test_labels).value_counts().sort_index()
    
    print(f"\n   📊 Train set:")
    for label, count in train_dist.items():
        label_name = 'Clean' if label == 0 else 'Toxic'
        percentage = (count / len(train_data)) * 100
        print(f"      {label} ({label_name}): {count:,} ({percentage:.1f}%)")
    
    print(f"\n   📊 Test set:")
    for label, count in test_dist.items():
        label_name = 'Clean' if label == 0 else 'Toxic'
        percentage = (count / len(test_data)) * 100
        print(f"      {label} ({label_name}): {count:,} ({percentage:.1f}%)")
    
    print(f"{'='*70}\n")
    
    return train_data, test_data


class ToxicCommentTrainer:
    def __init__(self, model_name='vinai/phobert-base', max_length=256):
        """Initialize PhoBERT trainer với FORCED GPU - CHỈ LOAD 1 LẦN"""
        self.model_name = model_name
        self.max_length = max_length
        
        self.gpu_config = check_and_configure_gpus()
        self.gpu_count = 1
        self.device = self.gpu_config['device']

        print(f"\n🚀 Initializing PhoBERT Trainer (Single Model Mode)")
        print(f"   Model: {model_name}")
        print(f"   Device: {self.device}")
        print(f"   Max length: {max_length}")

        print(f"\n📥 Loading tokenizer and model...")
        self.tokenizer = AutoTokenizer.from_pretrained(model_name)
        self.model = AutoModelForSequenceClassification.from_pretrained(
            model_name,
            num_labels=2
        )
        
        print(f"\n⚡ Moving model to GPU...")
        self.model = self.model.to(self.device)
        
        param_device = next(self.model.parameters()).device
        print(f"   ✅ Model is on: {param_device}")
        
        if param_device.type != 'cuda':
            raise RuntimeError(f"❌ Model KHÔNG trên GPU! Device: {param_device}")
        
        print(f"\n   🧪 Testing model forward pass on GPU...")
        dummy_input = torch.randint(0, 1000, (2, 128)).to(self.device)
        dummy_mask = torch.ones(2, 128).to(self.device)
        
        with torch.no_grad():
            output = self.model(input_ids=dummy_input, attention_mask=dummy_mask)
        
        print(f"   ✅ Forward pass successful on GPU!")
        print(f"   📊 GPU Memory after model load: {torch.cuda.memory_allocated(0) / 1024**3:.2f} GB")
        
        del dummy_input, dummy_mask, output
        torch.cuda.empty_cache()
        
        compute_cap = self.gpu_config['gpu_info'][0]['compute_capability'][0]
        self.use_fp16 = compute_cap >= 7
        
        if self.use_fp16:
            print(f"\n   🚀 Mixed Precision (FP16) ENABLED")
        else:
            print(f"   ℹ️  Mixed Precision DISABLED")

    def prepare_dataset(self, data_list):
        """Prepare dataset từ list of dicts"""
        print(f"\n🔤 Preparing dataset ({len(data_list):,} samples)...")
        
        dataset = Dataset.from_dict({
            'text': [d['text'] for d in data_list],
            'label': [d['label'] for d in data_list]
        })
        
        def tokenize_function(examples):
            return self.tokenizer(
                examples['text'],
                padding='max_length',
                truncation=True,
                max_length=self.max_length
            )
        
        dataset = dataset.map(tokenize_function, batched=True)
        dataset.set_format(type='torch', columns=['input_ids', 'attention_mask', 'label'])
        
        print(f"   ✅ Dataset prepared")
        
        return dataset

    def compute_metrics(self, eval_pred):
        """Compute metrics"""
        predictions, labels = eval_pred
        predictions = np.argmax(predictions, axis=1)

        accuracy = accuracy_score(labels, predictions)
        precision, recall, f1, _ = precision_recall_fscore_support(
            labels, predictions, average='binary'
        )

        return {
            'accuracy': accuracy,
            'precision': precision,
            'recall': recall,
            'f1': f1
        }

    def train_stage(self, train_dataset, test_dataset, 
                   num_epochs=3, batch_size=8, learning_rate=2e-5, 
                   stage_name="stage", temp_output_dir='temp_training'):
        """
        Train 1 stage - KHÔNG LƯU MODEL
        Chỉ cập nhật model weights trong memory
        """
        print(f"\n{'='*70}")
        print(f"🏋️  TRAINING {stage_name.upper()} (IN-MEMORY ONLY)")
        print(f"{'='*70}")
        
        total_memory = self.gpu_config['total_memory']
        safe_batch = min(batch_size, 8 if self.max_length <= 256 else 4)
        
        print(f"\n📊 Configuration:")
        print(f"   Stage: {stage_name}")
        print(f"   Epochs: {num_epochs}")
        print(f"   Batch size: {safe_batch}")
        print(f"   Learning rate: {learning_rate}")
        print(f"   GPU Memory: {total_memory:.2f} GB")
        
        # Tạo temp output dir (sẽ bị xóa sau)
        os.makedirs(temp_output_dir, exist_ok=True)

        training_args = TrainingArguments(
            output_dir=temp_output_dir,
            num_train_epochs=num_epochs,
            per_device_train_batch_size=safe_batch,
            per_device_eval_batch_size=safe_batch,
            
            learning_rate=learning_rate,
            warmup_steps=100,
            weight_decay=0.01,
            
            logging_dir=f"{temp_output_dir}/logs",
            logging_steps=10,
            logging_first_step=True,
            
            eval_strategy="epoch",
            save_strategy="no",  # ❌ KHÔNG LƯU CHECKPOINT
            
            push_to_hub=False,
            report_to=[],
            
            fp16=self.use_fp16,
            fp16_opt_level="O1",
            
            dataloader_num_workers=0,
            dataloader_pin_memory=True,
            dataloader_drop_last=False,
            
            gradient_accumulation_steps=max(1, batch_size // safe_batch),
            max_grad_norm=1.0,
            
            optim="adamw_torch",
            
            no_cuda=False,
            use_cpu=False,
            
            seed=42,
        )
        
        print(f"\n   ⚙️  Learning rate: {training_args.learning_rate}")
        print(f"   ⚙️  Gradient accumulation: {training_args.gradient_accumulation_steps}")
        print(f"   📦 Effective batch: {safe_batch * training_args.gradient_accumulation_steps}")

        print(f"\n📦 Initializing Trainer...")
        # ❌ BỎ EarlyStoppingCallback
        trainer = Trainer(
            model=self.model,  # Sử dụng model instance duy nhất
            args=training_args,
            train_dataset=train_dataset,
            eval_dataset=test_dataset,
            compute_metrics=self.compute_metrics,
            # ❌ KHÔNG DÙNG callbacks
        )
        
        print(f"   📍 Trainer device: {trainer.args.device}")
        if trainer.args.device.type != 'cuda':
            raise RuntimeError(f"❌ Trainer device is {trainer.args.device}, not CUDA!")
        
        print(f"   ✅ Trainer on GPU: {trainer.args.device}")

        print(f"\n{'='*70}")
        print(f"⏳ TRAINING {stage_name.upper()} STARTING...")
        print(f"{'='*70}")
        
        import time
        time.sleep(2)
        
        print(f"\n📊 GPU State Before Training:")
        print(f"   Allocated: {torch.cuda.memory_allocated(0) / 1024**3:.2f} GB")
        print(f"   Reserved: {torch.cuda.memory_reserved(0) / 1024**3:.2f} GB")
        print()
        
        try:
            train_result = trainer.train()
            
            print(f"\n{'='*70}")
            print(f"✅ {stage_name.upper()} TRAINING COMPLETED")
            print(f"{'='*70}")
            print(f"   Time: {train_result.metrics.get('train_runtime', 0):.2f}s")
            print(f"   Samples/sec: {train_result.metrics.get('train_samples_per_second', 0):.2f}")
            
        except RuntimeError as e:
            if "out of memory" in str(e).lower():
                print(f"\n❌ GPU OOM!")
                print(f"\n   Try: batch_size=4, max_length=128")
                torch.cuda.empty_cache()
                raise
            else:
                raise
        
        print(f"\n📊 Final GPU Memory:")
        print(f"   Allocated: {torch.cuda.memory_allocated(0) / 1024**3:.2f} GB")
        print(f"   Peak: {torch.cuda.max_memory_allocated(0) / 1024**3:.2f} GB")

        # ❌ KHÔNG LƯU MODEL - chỉ return trainer để evaluate
        return trainer

    def evaluate(self, trainer, test_dataset, stage_name=""):
        """Evaluate model"""
        print(f"\n📊 Evaluating {stage_name}...")

        eval_results = trainer.evaluate(test_dataset)

        print(f"\n✅ {stage_name} Results:")
        print(f"   Accuracy:  {eval_results['eval_accuracy']:.4f}")
        print(f"   Precision: {eval_results['eval_precision']:.4f}")
        print(f"   Recall:    {eval_results['eval_recall']:.4f}")
        print(f"   F1 Score:  {eval_results['eval_f1']:.4f}")

        return eval_results

    def save_final_model(self, output_dir, stage_info):
        """LƯU MODEL FINAL DUY NHẤT"""
        print(f"\n{'='*70}")
        print(f"💾 SAVING FINAL MODEL")
        print(f"{'='*70}")
        
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        final_model_dir = f"{output_dir}/phobert_toxic_FINAL_{timestamp}"
        os.makedirs(final_model_dir, exist_ok=True)
        
        print(f"   Saving to: {final_model_dir}")
        
        # Save model and tokenizer
        self.model.save_pretrained(final_model_dir)
        self.tokenizer.save_pretrained(final_model_dir)
        
        # Save training info
        with open(f"{final_model_dir}/training_info.json", 'w', encoding='utf-8') as f:
            json.dump(stage_info, f, indent=2, default=str, ensure_ascii=False)
        
        print(f"   ✅ Model saved successfully!")
        print(f"{'='*70}\n")
        
        return final_model_dir


def main():
    """Main pipeline: SEQUENTIAL TRAINING - CHỈ LƯU 1 MODEL FINAL"""

    print("=" * 70)
    print("PHOBERT TOXIC COMMENT CLASSIFIER")
    print("SEQUENTIAL TRAINING - SINGLE FINAL MODEL OUTPUT")
    print("🔥 100% DATA - IN-MEMORY TRAINING 🔥")
    print("=" * 70)

    # ========================================
    # CONFIGURATION
    # ========================================
    CONFIG = {
        'model_name': 'vinai/phobert-base',
        'max_length': 256,
        'output_dir': 'models',
        
        'stage1_json': {
            'enabled': True,
            'train_json': 'data/train_dataset_20251111_162400.json',
            'test_json': 'data/test_dataset_20251111_162400.json',
            'num_epochs': 5,
            'batch_size': 8,
            'learning_rate': 5e-5,
        },
        
        'stage2_csv1': {
            'enabled': True,
            'csv_path': 'data/test_df.csv',
            'text_column': 'cmt_col',
            'label_column': 'labels',
            'num_epochs': 3,
            'batch_size': 8,
            'learning_rate': 2e-5,
        },
        
        'stage3_csv2': {
            'enabled': True,
            'csv_path': 'data/dev.csv',
            'text_column': 'free_text',
            'label_column': 'label_id',
            'num_epochs': 3,
            'batch_size': 8,
            'learning_rate': 2e-5,
        },
        
        'stage4_csv3': {
            'enabled': True,
            'csv_path': 'data/test.csv',
            'text_column': 'free_text',
            'label_column': 'label_id',
            'num_epochs': 2,
            'batch_size': 8,
            'learning_rate': 1e-5,
        },
        
        'stage5_csv4': { 
            'enabled': True,
            'csv_path': 'data/train.csv',
            'text_column': 'free_text',
            'label_column': 'label_id',
            'num_epochs': 2,
            'batch_size': 8,
            'learning_rate': 1e-5,
        },
    }
    
    print("\n📋 Configuration:")
    print(f"   Base model: {CONFIG['model_name']}")
    print(f"   Max length: {CONFIG['max_length']}")
    print(f"   💾 Output: 1 FINAL MODEL ONLY")
    
    # ✅ KHỞI TẠO TRAINER 1 LẦN DUY NHẤT
    print("\n" + "🔷" * 35)
    print("INITIALIZING SINGLE MODEL INSTANCE")
    print("🔷" * 35)
    
    trainer_obj = ToxicCommentTrainer(
        model_name=CONFIG['model_name'],
        max_length=CONFIG['max_length']
    )
    
    all_results = {}
    total_train_samples = 0
    
    # Load JSON test set (dùng xuyên suốt để evaluate)
    print("\n" + "📘" * 35)
    print("LOADING GLOBAL TEST SET (JSON)")
    print("📘" * 35)
    
    _, test_data_json = load_json_data(
        train_path=CONFIG['stage1_json']['train_json'],
        test_path=CONFIG['stage1_json']['test_json']
    )
    test_dataset_json = trainer_obj.prepare_dataset(test_data_json)

    # ========================================
    # STAGE 1: JSON DATA
    # ========================================
    if CONFIG['stage1_json']['enabled']:
        print("\n" + "🔵" * 35)
        print("STAGE 1: JSON DATA")
        print("🔵" * 35)
        
        train_data, _ = load_json_data(
            train_path=CONFIG['stage1_json']['train_json'],
            test_path=CONFIG['stage1_json']['test_json']
        )
        train_dataset = trainer_obj.prepare_dataset(train_data)
        
        trainer = trainer_obj.train_stage(
            train_dataset=train_dataset,
            test_dataset=test_dataset_json,
            num_epochs=CONFIG['stage1_json']['num_epochs'],
            batch_size=CONFIG['stage1_json']['batch_size'],
            learning_rate=CONFIG['stage1_json']['learning_rate'],
            stage_name="stage1_json"
        )
        
        eval_results = trainer_obj.evaluate(trainer, test_dataset_json, "STAGE 1")
        
        all_results['stage1_json'] = {
            'train_samples': len(train_data),
            'results': eval_results
        }
        total_train_samples += len(train_data)
        
        del trainer
        torch.cuda.empty_cache()

    # ========================================
    # STAGE 2: test_df.csv
    # ========================================
    if CONFIG['stage2_csv1']['enabled']:
        print("\n" + "🟢" * 35)
        print("STAGE 2: test_df.csv")
        print("🟢" * 35)
        
        train_data = load_csv_full_data(
            csv_path=CONFIG['stage2_csv1']['csv_path'],
            text_column=CONFIG['stage2_csv1']['text_column'],
            label_column=CONFIG['stage2_csv1']['label_column']
        )
        train_dataset = trainer_obj.prepare_dataset(train_data)
        
        trainer = trainer_obj.train_stage(
            train_dataset=train_dataset,
            test_dataset=test_dataset_json,
            num_epochs=CONFIG['stage2_csv1']['num_epochs'],
            batch_size=CONFIG['stage2_csv1']['batch_size'],
            learning_rate=CONFIG['stage2_csv1']['learning_rate'],
            stage_name="stage2_csv1"
        )
        
        eval_results = trainer_obj.evaluate(trainer, test_dataset_json, "STAGE 2")
        
        all_results['stage2_csv1'] = {
            'train_samples': len(train_data),
            'results': eval_results
        }
        total_train_samples += len(train_data)
        
        del trainer
        torch.cuda.empty_cache()

    # ========================================
    # STAGE 3: dev.csv
    # ========================================
    if CONFIG['stage3_csv2']['enabled']:
        print("\n" + "🟡" * 35)
        print("STAGE 3: dev.csv")
        print("🟡" * 35)
        
        train_data = load_csv_full_data(
            csv_path=CONFIG['stage3_csv2']['csv_path'],
            text_column=CONFIG['stage3_csv2']['text_column'],
            label_column=CONFIG['stage3_csv2']['label_column']
        )
        train_dataset = trainer_obj.prepare_dataset(train_data)
        
        trainer = trainer_obj.train_stage(
            train_dataset=train_dataset,
            test_dataset=test_dataset_json,
            num_epochs=CONFIG['stage3_csv2']['num_epochs'],
            batch_size=CONFIG['stage3_csv2']['batch_size'],
            learning_rate=CONFIG['stage3_csv2']['learning_rate'],
            stage_name="stage3_csv2"
        )
        
        eval_results = trainer_obj.evaluate(trainer, test_dataset_json, "STAGE 3")
        
        all_results['stage3_csv2'] = {
            'train_samples': len(train_data),
            'results': eval_results
        }
        total_train_samples += len(train_data)
        
        del trainer
        torch.cuda.empty_cache()

    # ========================================
    # STAGE 4: test.csv
    # ========================================
    if CONFIG['stage4_csv3']['enabled']:
        print("\n" + "🟠" * 35)
        print("STAGE 4: test.csv")
        print("🟠" * 35)
        
        train_data = load_csv_full_data(
            csv_path=CONFIG['stage4_csv3']['csv_path'],
            text_column=CONFIG['stage4_csv3']['text_column'],
            label_column=CONFIG['stage4_csv3']['label_column']
        )
        train_dataset = trainer_obj.prepare_dataset(train_data)
        
        trainer = trainer_obj.train_stage(
            train_dataset=train_dataset,
            test_dataset=test_dataset_json,
            num_epochs=CONFIG['stage4_csv3']['num_epochs'],
            batch_size=CONFIG['stage4_csv3']['batch_size'],
            learning_rate=CONFIG['stage4_csv3']['learning_rate'],
            stage_name="stage4_csv3"
        )
        
        eval_results = trainer_obj.evaluate(trainer, test_dataset_json, "STAGE 4")
        
        all_results['stage4_csv3'] = {
            'train_samples': len(train_data),
            'results': eval_results
        }
        total_train_samples += len(train_data)
        
        del trainer
        torch.cuda.empty_cache()

    # ========================================
    # STAGE 5: train.csv (FINAL)
    # ========================================
    if CONFIG['stage5_csv4']['enabled']:
        print("\n" + "🔴" * 35)
        print("STAGE 5: train.csv (FINAL)")
        print("🔴" * 35)
        
        train_data = load_csv_full_data(
            csv_path=CONFIG['stage5_csv4']['csv_path'],
            text_column=CONFIG['stage5_csv4']['text_column'],
            label_column=CONFIG['stage5_csv4']['label_column']
        )
        train_dataset = trainer_obj.prepare_dataset(train_data)
        
        trainer = trainer_obj.train_stage(
            train_dataset=train_dataset,
            test_dataset=test_dataset_json,
            num_epochs=CONFIG['stage5_csv4']['num_epochs'],
            batch_size=CONFIG['stage5_csv4']['batch_size'],
            learning_rate=CONFIG['stage5_csv4']['learning_rate'],
            stage_name="stage5_csv4_FINAL"
        )
        
        eval_results = trainer_obj.evaluate(trainer, test_dataset_json, "STAGE 5 (FINAL)")
        
        all_results['stage5_csv4_FINAL'] = {
            'train_samples': len(train_data),
            'results': eval_results
        }
        total_train_samples += len(train_data)
        
        del trainer
        torch.cuda.empty_cache()

    # ========================================
    # 💾 LƯU 1 MODEL FINAL DUY NHẤT
    # ========================================
    print("\n" + "🎯" * 35)
    print("SAVING FINAL MODEL (ONLY ONE)")
    print("🎯" * 35)
    
    stage_info = {
        'training_strategy': 'sequential_continual_learning_single_model',
        'training_date': datetime.now().isoformat(),
        'total_train_samples': total_train_samples,
        'test_samples': len(test_data_json),
        'config': CONFIG,
        'all_stage_results': all_results
    }
    
    final_model_path = trainer_obj.save_final_model(
        output_dir=CONFIG['output_dir'],
        stage_info=stage_info
    )

    # ========================================
    # FINAL SUMMARY
    # ========================================
    print("\n" + "=" * 70)
    print("📊 FINAL RESULTS SUMMARY")
    print("=" * 70)
    
    for stage_key, stage_data in all_results.items():
        stage_name = stage_key.replace('_', ' ').upper()
        print(f"\n{'🔥' if 'FINAL' in stage_key else '📌'} {stage_name}:")
        print(f"   Train samples: {stage_data['train_samples']:,}")
        print(f"   Accuracy:  {stage_data['results']['eval_accuracy']:.4f}")
        print(f"   Precision: {stage_data['results']['eval_precision']:.4f}")
        print(f"   Recall:    {stage_data['results']['eval_recall']:.4f}")
        print(f"   F1 Score:  {stage_data['results']['eval_f1']:.4f}")
    
    print(f"\n{'='*70}")
    print(f"📊 TOTAL TRAINING DATA: {total_train_samples:,} samples")
    print(f"📊 TEST SET SIZE: {len(test_data_json):,} samples")
    print(f"{'='*70}")
    
    print("\n" + "=" * 70)
    print("✅ TRAINING COMPLETE!")
    print("=" * 70)
    print(f"\n💾 FINAL MODEL: {final_model_path}")
    print(f"📊 Training info: {final_model_path}/training_info.json")
    print(f"\n🎯 Trained on {total_train_samples:,} samples across {len(all_results)} stages")
    print(f"🎯 Evaluated on {len(test_data_json):,} test samples")
    
    torch.cuda.empty_cache()


if __name__ == "__main__":
    main()