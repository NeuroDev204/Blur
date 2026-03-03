#!/usr/bin/env bash
set -e

BACKEND_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

SPRING_SERVICES=(
    "IdentityService"
    "api-gateway"
    "chat-service"
    "notification-service"
    "post-service"
    "profile-service"
    "story-service"
    "ai-service"
)

echo "========================================"
echo "  Blur Backend - Build & Package"
echo "========================================"
echo ""

# Build Spring Boot services
for service in "${SPRING_SERVICES[@]}"; do
    SERVICE_DIR="$BACKEND_DIR/$service"
    if [ ! -d "$SERVICE_DIR" ]; then
        echo "[SKIP] $service - directory not found"
        continue
    fi

    echo ">>> Building $service ..."

    if [ -f "$SERVICE_DIR/mvnw" ]; then
        (cd "$SERVICE_DIR" && chmod +x mvnw && ./mvnw clean package -DskipTests -q)
    elif command -v mvn &>/dev/null; then
        (cd "$SERVICE_DIR" && mvn clean package -DskipTests -q)
    else
        echo "[ERROR] Neither mvnw nor mvn found for $service"
        exit 1
    fi

    echo "[OK] $service built successfully"
done

echo ""

# Install Python dependencies for model-service
MODEL_DIR="$BACKEND_DIR/model-service"
if [ -d "$MODEL_DIR" ]; then
    echo ">>> Installing model-service dependencies ..."
    if [ -f "$MODEL_DIR/requirements.txt" ]; then
        pip install -r "$MODEL_DIR/requirements.txt" -q
        echo "[OK] model-service dependencies installed"
    else
        echo "[SKIP] model-service - requirements.txt not found"
    fi
fi

echo ""
echo "========================================"
echo "  All services built successfully!"
echo "========================================"
