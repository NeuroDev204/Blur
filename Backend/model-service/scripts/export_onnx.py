"""
scripts/export_onnx.py
Convert PhoBERT model từ PyTorch sang ONNX format.

Chạy 1 lần sau khi train xong:
  python scripts/export_onnx.py

Benefits của ONNX trong production:
  - import onnxruntime: ~2s  vs  import torch: ~20-40s
  - load ONNX model:   ~3-8s vs  load PyTorch: ~15-30s
  - Không cần cài PyTorch trong production Docker image
  - RAM usage: ~800MB  vs  ~1.5GB
"""

import torch
from transformers import AutoTokenizer, AutoModelForSequenceClassification
from pathlib import Path
import sys
import os

sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..'))


def export_to_onnx(model_path: str, output_dir: str):
    print(f"Loading PyTorch model from {model_path}...")
    tokenizer = AutoTokenizer.from_pretrained(model_path)
    model = AutoModelForSequenceClassification.from_pretrained(model_path)
    model.eval()

    # Dummy input cho export
    dummy = tokenizer(
        "test",
        padding='max_length',
        truncation=True,
        max_length=256,
        return_tensors='pt',
    )

    output_path = os.path.join(output_dir, "model.onnx")
    os.makedirs(output_dir, exist_ok=True)

    print(f"Exporting to {output_path}...")
    torch.onnx.export(
        model,
        (dummy['input_ids'], dummy['attention_mask']),
        output_path,
        input_names=['input_ids', 'attention_mask'],
        output_names=['logits'],
        dynamic_axes={
            'input_ids':      {0: 'batch', 1: 'sequence'},
            'attention_mask': {0: 'batch', 1: 'sequence'},
            'logits':         {0: 'batch'},
        },
        opset_version=14,
    )

    # Copy tokenizer files
    tokenizer.save_pretrained(output_dir)

    pytorch_size = sum(
        f.stat().st_size for f in Path(model_path).rglob('*') if f.is_file()
    ) / 1024 ** 2
    onnx_size = Path(output_path).stat().st_size / 1024 ** 2

    print(f"\nExport complete!")
    print(f"  PyTorch size: {pytorch_size:.0f} MB")
    print(f"  ONNX size:    {onnx_size:.0f} MB")
    print(f"  Saved:        {pytorch_size - onnx_size:.0f} MB ({(1 - onnx_size / pytorch_size) * 100:.0f}%)")
    print(f"  Output dir:   {output_dir}")


if __name__ == '__main__':
    import argparse
    parser = argparse.ArgumentParser(description='Export PhoBERT model to ONNX')
    parser.add_argument(
        '--model_path',
        default='models/phobert_toxic_FINAL_20251111_232134',
        help='Path to PyTorch model directory',
    )
    parser.add_argument(
        '--output_dir',
        default='models/phobert_toxic_onnx',
        help='Output directory for ONNX model',
    )
    args = parser.parse_args()
    export_to_onnx(args.model_path, args.output_dir)
