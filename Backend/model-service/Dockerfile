# ── Stage 1: Build dependencies ──
FROM python:3.11-slim AS deps

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc g++ && \
    rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# ── Stage 2: Runtime ──
FROM python:3.11-slim

WORKDIR /app

# Copy installed packages from deps stage
COPY --from=deps /usr/local/lib/python3.11/site-packages /usr/local/lib/python3.11/site-packages
COPY --from=deps /usr/local/bin /usr/local/bin

# Copy source code
COPY app/ ./app/

# Copy model (lớp lớn nhất ~500MB — đặt cuối để cache tốt khi chỉ đổi code)
COPY models/ ./models/

# Env defaults (override via docker-compose environment or .env)
ENV KAFKA_BOOTSTRAP_SERVERS=kafka:9092
ENV MODEL_PATH=models/phobert_toxic_FINAL_20251111_232134
ENV MODEL_BACKEND=pytorch
ENV HOST=0.0.0.0
ENV PORT=8000

# Health check — dùng start_period=60s vì model cần thời gian load
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/api/v1/health')" || exit 1

EXPOSE 8000

# workers=1: mỗi worker load 1 bản model (~500MB RAM), không nên chạy nhiều worker
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "1"]
