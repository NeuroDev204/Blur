#!/bin/bash
set -e

DOCKER_USER="vansy1001"
VERSION="latest"

echo "======================================"
echo "Building and Pushing Blur Services"
echo "======================================"

docker login

build_and_push() {
    local SERVICE_NAME=$1
    local CONTEXT_PATH=$2

    echo ""
    echo "📦 Building $SERVICE_NAME..."
    docker build -t $DOCKER_USER/$SERVICE_NAME:$VERSION $CONTEXT_PATH

    if [ $? -eq 0 ]; then
        echo "✅ Build successful: $SERVICE_NAME"
        echo "🚀 Pushing $SERVICE_NAME..."
        docker push $DOCKER_USER/$SERVICE_NAME:$VERSION
        echo "✅ Push successful: $SERVICE_NAME"
    else
        echo "❌ Failed to build: $SERVICE_NAME"
        exit 1
    fi
}

build_and_push "api-gateway" "../api-gateway"
build_and_push "profile-service" "../user-service"
build_and_push "notification-service" "../notification-service"
build_and_push "chat-service" "../chat-service"
build_and_push "post-service" "../post-service"
build_and_push "story-service" "../story-service"
build_and_push "blur-frontend" "../../frontend"

echo ""
echo "✅ ALL DONE!"
