"""
Auto Pipeline: Collect Dataset → Train Model
Tự động chạy collect_dataset.py, lấy file mới và train PhoBERT model
"""

import subprocess
import sys
import os
import glob
from datetime import datetime

def run_command(cmd, cwd=None, env=None):
    """Run a command and stream output with UTF-8 encoding"""
    print(f"\n{'='*60}")
    print(f"Running: {' '.join(cmd)}")
    print('='*60 + "\n")
    
    # Use UTF-8 encoding for subprocess
    my_env = env if env else os.environ.copy()
    my_env["PYTHONIOENCODING"] = "utf-8"
    
    process = subprocess.Popen(
        cmd,
        cwd=cwd,
        env=my_env,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1,
        encoding='utf-8',
        errors='replace'
    )
    
    for line in process.stdout:
        print(line, end='')
    
    process.wait()
    return process.returncode

def find_latest_file(directory, pattern):
    """Find the most recently modified file matching pattern"""
    files = glob.glob(os.path.join(directory, pattern))
    if not files:
        return None
    return max(files, key=os.path.getmtime)

def main():
    print("""
================================================================
           AUTO PIPELINE: COLLECT -> TRAIN                  
================================================================
    """)
    
    base_dir = os.path.dirname(os.path.abspath(__file__))
    data_dir = os.path.join(base_dir, "data")
    
    # Use current Python executable
    python_exe = sys.executable
    print(f"Using Python: {python_exe}")
    
    # ========================================
    # STEP 1: Run collect_dataset.py
    # ========================================
    print("\n" + "-" * 60)
    print("  STEP 1: Thu thap dataset tu YouTube & TikTok")
    print("-" * 60)
    
    collect_script = os.path.join(base_dir, "collect_dataset.py")
    exit_code = run_command([python_exe, collect_script], cwd=base_dir)
    
    if exit_code != 0:
        print(f"\n[!] collect_dataset.py exited with code {exit_code}")
        print("Continuing to training anyway...")
    
    # ========================================
    # STEP 2: Find latest generated files
    # ========================================
    print("\n" + "-" * 60)
    print("  STEP 2: Tim file dataset moi nhat")
    print("-" * 60)
    
    # Find train and test files
    train_file = find_latest_file(data_dir, "train_*.json")
    test_file = find_latest_file(data_dir, "test_*.json")
    
    if train_file:
        print(f"[+] Train file: {os.path.basename(train_file)}")
    else:
        print("[!] No train file found, will use default")
        
    if test_file:
        print(f"[+] Test file: {os.path.basename(test_file)}")
    else:
        print("[!] No test file found, will use default")
    
    # ========================================
    # STEP 3: Run train_model.py
    # ========================================
    print("\n" + "-" * 60)
    print("  STEP 3: Train PhoBERT Model")
    print("-" * 60)
    
    train_script = os.path.join(base_dir, "training", "train_model.py")
    
    # Set environment variables for the training script to use new files
    env = os.environ.copy()
    env["PYTHONIOENCODING"] = "utf-8"
    if train_file:
        env["TRAIN_FILE"] = train_file
    if test_file:
        env["TEST_FILE"] = test_file
    
    # Run training with venv Python
    exit_code = run_command([python_exe, train_script], cwd=base_dir, env=env)
    
    # ========================================
    # DONE
    # ========================================
    print("\n" + "=" * 60)
    print("PIPELINE HOAN TAT!")
    print("=" * 60)
    
    # Show model location
    models_dir = os.path.join(base_dir, "models")
    latest_model = find_latest_file(models_dir, "phobert_*")
    if latest_model:
        print(f"\nModel moi: {latest_model}")
    
    print(f"\nThoi gian hoan thanh: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")

if __name__ == "__main__":
    main()
