#!/bin/bash

echo "╔════════════════════════════════════════╗"
echo "║     Blur Social Network - Deploy       ║"
echo "╚════════════════════════════════════════╝"
echo ""

# Check Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker not found!"
    echo "👉 Install Docker: https://docs.docker.com/get-docker/"
    exit 1
fi

echo "✅ Docker found"
echo ""

# Pull latest images
echo "📥 Pulling latest images from Docker Hub..."
echo "   This may take a few minutes on first run..."
docker compose pull

echo ""

# Stop old containers
echo "🛑 Stopping old containers..."
docker compose down 2>/dev/null || true

echo ""

# Start services
echo "▶️  Starting all services..."
docker compose up -d

echo ""

# Wait
echo "⏳ Waiting for services to start..."
echo "   This takes about 90 seconds..."
for i in {1..90}; do
    printf "."
    sleep 1
    if [ $((i % 30)) -eq 0 ]; then
        printf " ${i}s\n   "
    fi
done
echo ""
echo ""

# Show status
echo "📊 Service Status:"
docker compose ps

echo ""
echo "╔════════════════════════════════════════╗"
echo "║            ✅ READY TO USE!            ║"
echo "╚════════════════════════════════════════╝"
echo ""
echo "🌐 Access Application:"
echo "   → http://localhost"
echo ""
echo "🔧 Admin Panels:"
echo "   → Neo4j: http://localhost:7474 (neo4j/12345678)"
echo ""
echo "📝 Useful Commands:"
echo "   Stop:  ./stop.sh"
echo "   Logs:  docker compose logs -f"
echo "   Clean: docker compose down -v"
echo ""