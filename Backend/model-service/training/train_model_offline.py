"""
Bước 2: Train PhoBERT Model (OFFLINE VERSION)
Sử dụng PhoBERT model đã tải về local
"""

import os
import json
import torch
import numpy as np
from datetime import datetime
from transformers import (
    AutoTokenizer,
    AutoModelForSequenceClassification,
    TrainingArguments,
    Trainer,
    EarlyStoppingCallback
)
from datasets import Dataset
from sklearn.metrics import accuracy_score, precision_recall_fscore_support, confusion_matrix
import matplotlib.pyplot as plt
import seaborn as sns


class ToxicCommentTrainer:
    def __init__(self, model_path='models/phobert-base-local', max_length=256):
        """
        Initialize PhoBERT trainer

        Args:
            model_path (str): Path to local PhoBERT model
            max_length (int): Max sequence length
        """
        self.model_path = model_path
        self.max_length = max_length
        self.device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')

        print(f"🚀 Initializing PhoBERT Trainer (OFFLINE)")
        print(f"   Model path: {model_path}")
        print(f"   Device: {self.device}")
        print(f"   Max length: {max_length}")

        # Check if model exists
        if not os.path.exists(model_path):
            raise FileNotFoundError(
                f"Model not found at {model_path}\n"
                f"Please run: python download_phobert.py first"
            )

        # Disable SSL verification for offline mode
        os.environ['CURL_CA_BUNDLE'] = ''
        os.environ['REQUESTS_CA_BUNDLE'] = ''
        os.environ['TRANSFORMERS_OFFLINE'] = '1'

        # Load tokenizer
        self.tokenizer = AutoTokenizer.from_pretrained(
            model_path,
            local_files_only=True
        )

        # Load model for classification
        self.model = AutoModelForSequenceClassification.from_pretrained(
            model_path,
            num_labels=2,  # Binary classification
            local_files_only=True,
            ignore_mismatched_sizes=True  # Important for adding classification head
        )
        self.model.to(self.device)

    def load_dataset(self, train_path, test_path):
        """
        Load training and test datasets

        Args:
            train_path (str): Path to training JSON file
            test_path (str): Path to test JSON file

        Returns:
            tuple: (train_dataset, test_dataset)
        """
        print(f"\n📂 Loading datasets...")

        # Load JSON files
        with open(train_path, 'r', encoding='utf-8') as f:
            train_data = json.load(f)

        with open(test_path, 'r', encoding='utf-8') as f:
            test_data = json.load(f)

        print(f"   Train samples: {len(train_data)}")
        print(f"   Test samples: {len(test_data)}")

        # Convert to Hugging Face Dataset
        train_dataset = Dataset.from_dict({
            'text': [d['text'] for d in train_data],
            'label': [d['label'] for d in train_data]
        })

        test_dataset = Dataset.from_dict({
            'text': [d['text'] for d in test_data],
            'label': [d['label'] for d in test_data]
        })

        # Tokenize
        print(f"\n🔤 Tokenizing...")

        def tokenize_function(examples):
            return self.tokenizer(
                examples['text'],
                padding='max_length',
                truncation=True,
                max_length=self.max_length
            )

        train_dataset = train_dataset.map(tokenize_function, batched=True)
        test_dataset = test_dataset.map(tokenize_function, batched=True)

        # Set format for PyTorch
        train_dataset.set_format(type='torch', columns=['input_ids', 'attention_mask', 'label'])
        test_dataset.set_format(type='torch', columns=['input_ids', 'attention_mask', 'label'])

        print(f"   ✅ Tokenization complete")

        return train_dataset, test_dataset

    def compute_metrics(self, eval_pred):
        """
        Compute evaluation metrics

        Args:
            eval_pred: Evaluation predictions

        Returns:
            dict: Metrics
        """
        predictions, labels = eval_pred
        predictions = np.argmax(predictions, axis=1)

        # Calculate metrics
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

    def train(self, train_dataset, test_dataset, output_dir='models', num_epochs=3, batch_size=16):
        """
        Train the model

        Args:
            train_dataset: Training dataset
            test_dataset: Test dataset
            output_dir (str): Output directory for model
            num_epochs (int): Number of training epochs
            batch_size (int): Batch size

        Returns:
            Trainer: Trained model
        """
        print(f"\n🏋️  Starting training...")
        print(f"   Epochs: {num_epochs}")
        print(f"   Batch size: {batch_size}")

        # Create output directory
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        model_output_dir = f"{output_dir}/phobert_toxic_{timestamp}"
        os.makedirs(model_output_dir, exist_ok=True)

        # Training arguments
        training_args = TrainingArguments(
            output_dir=model_output_dir,
            num_train_epochs=num_epochs,
            per_device_train_batch_size=batch_size,
            per_device_eval_batch_size=batch_size,
            warmup_steps=500,
            weight_decay=0.01,
            logging_dir=f"{model_output_dir}/logs",
            logging_steps=100,
            eval_strategy="epoch",
            save_strategy="epoch",
            load_best_model_at_end=True,
            metric_for_best_model="f1",
            greater_is_better=True,
            save_total_limit=2,
            push_to_hub=False,
            report_to=None,  # Disable wandb/tensorboard
        )

        # Initialize Trainer
        trainer = Trainer(
            model=self.model,
            args=training_args,
            train_dataset=train_dataset,
            eval_dataset=test_dataset,
            compute_metrics=self.compute_metrics,
            callbacks=[EarlyStoppingCallback(early_stopping_patience=2)]
        )

        # Train
        print(f"\n⏳ Training in progress...")
        trainer.train()

        # Save final model
        trainer.save_model(model_output_dir)
        self.tokenizer.save_pretrained(model_output_dir)

        print(f"\n✅ Training complete!")
        print(f"   Model saved to: {model_output_dir}")

        return trainer, model_output_dir

    def evaluate(self, trainer, test_dataset):
        """
        Evaluate the model

        Args:
            trainer: Trained model
            test_dataset: Test dataset

        Returns:
            dict: Evaluation results
        """
        print(f"\n📊 Evaluating model...")

        # Evaluate
        eval_results = trainer.evaluate(test_dataset)

        print(f"\n✅ Evaluation Results:")
        print(f"   Accuracy:  {eval_results['eval_accuracy']:.4f}")
        print(f"   Precision: {eval_results['eval_precision']:.4f}")
        print(f"   Recall:    {eval_results['eval_recall']:.4f}")
        print(f"   F1 Score:  {eval_results['eval_f1']:.4f}")

        return eval_results

    def plot_confusion_matrix(self, trainer, test_dataset, output_path='confusion_matrix.png'):
        """
        Plot confusion matrix

        Args:
            trainer: Trained model
            test_dataset: Test dataset
            output_path (str): Output path for plot
        """
        print(f"\n📈 Generating confusion matrix...")

        # Get predictions
        predictions = trainer.predict(test_dataset)
        pred_labels = np.argmax(predictions.predictions, axis=1)
        true_labels = predictions.label_ids

        # Confusion matrix
        cm = confusion_matrix(true_labels, pred_labels)

        # Plot
        plt.figure(figsize=(8, 6))
        sns.heatmap(
            cm,
            annot=True,
            fmt='d',
            cmap='Blues',
            xticklabels=['Clean', 'Toxic'],
            yticklabels=['Clean', 'Toxic']
        )
        plt.title('Confusion Matrix - Vietnamese Toxic Comment Detection')
        plt.ylabel('True Label')
        plt.xlabel('Predicted Label')
        plt.tight_layout()
        plt.savefig(output_path)
        print(f"   Saved confusion matrix to: {output_path}")

        return cm


def main():
    """Main training pipeline"""

    print("=" * 70)
    print("PHOBERT TOXIC COMMENT CLASSIFIER - TRAINING (OFFLINE)")
    print("=" * 70)

    # =========================================================================
    # CÀI ĐẶT
    # =========================================================================

    # Path to LOCAL PhoBERT model
    LOCAL_MODEL_PATH = "models/phobert-base-local"

    # Check if model exists
    if not os.path.exists(LOCAL_MODEL_PATH):
        print(f"\n❌ ERROR: PhoBERT model not found at {LOCAL_MODEL_PATH}")
        print(f"\n💡 Please download model first:")
        print(f"   python download_phobert.py")
        return

    # Paths to dataset (from step 1)
    # Update these with your actual file paths
    TRAIN_PATH = "data/train_dataset_20241021_000000.json"
    TEST_PATH = "data/test_dataset_20241021_000000.json"

    # Check if dataset exists
    if not os.path.exists(TRAIN_PATH) or not os.path.exists(TEST_PATH):
        print(f"\n❌ ERROR: Dataset not found")
        print(f"   Looking for:")
        print(f"   - {TRAIN_PATH}")
        print(f"   - {TEST_PATH}")
        print(f"\n💡 Please collect dataset first:")
        print(f"   python 1_collect_dataset.py")
        return

    # Training config
    CONFIG = {
        'model_path': LOCAL_MODEL_PATH,
        'max_length': 256,
        'num_epochs': 3,
        'batch_size': 16,  # Giảm xuống 8 nếu thiếu RAM
        'output_dir': 'models'
    }

    # =========================================================================
    # BƯỚC 1: KHỞI TẠO TRAINER
    # =========================================================================

    try:
        trainer_obj = ToxicCommentTrainer(
            model_path=CONFIG['model_path'],
            max_length=CONFIG['max_length']
        )
    except Exception as e:
        print(f"\n❌ Failed to initialize trainer: {e}")
        return

    # =========================================================================
    # BƯỚC 2: LOAD DATASET
    # =========================================================================

    train_dataset, test_dataset = trainer_obj.load_dataset(TRAIN_PATH, TEST_PATH)

    # =========================================================================
    # BƯỚC 3: TRAIN MODEL
    # =========================================================================

    trainer, model_output_dir = trainer_obj.train(
        train_dataset=train_dataset,
        test_dataset=test_dataset,
        output_dir=CONFIG['output_dir'],
        num_epochs=CONFIG['num_epochs'],
        batch_size=CONFIG['batch_size']
    )

    # =========================================================================
    # BƯỚC 4: EVALUATE
    # =========================================================================

    eval_results = trainer_obj.evaluate(trainer, test_dataset)

    # =========================================================================
    # BƯỚC 5: CONFUSION MATRIX
    # =========================================================================

    cm = trainer_obj.plot_confusion_matrix(
        trainer,
        test_dataset,
        output_path=f"{model_output_dir}/confusion_matrix.png"
    )

    # =========================================================================
    # LƯU METRICS
    # =========================================================================

    metrics = {
        'model_name': CONFIG['model_path'],
        'training_date': datetime.now().isoformat(),
        'config': CONFIG,
        'results': eval_results,
        'confusion_matrix': cm.tolist()
    }

    with open(f"{model_output_dir}/metrics.json", 'w') as f:
        json.dump(metrics, f, indent=2)

    print("\n" + "=" * 70)
    print("✅ TRAINING COMPLETE!")
    print("=" * 70)
    print(f"\nModel saved to: {model_output_dir}")
    print(f"Next step: Use model with script 3_inference.py")


if __name__ == "__main__":
    main()
