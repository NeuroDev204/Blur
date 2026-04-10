#!/bin/bash

echo "🔍 Troubleshooting Blur Platform"
echo "================================="
echo ""

echo "📋 Service Status:"
docker-compose -f docker-compose.prod.yaml ps
echo ""

echo "💾 Disk Usage:"
docker system df
echo ""

echo "🌐 Network:"
docker network ls | grep blur
echo ""

echo "📊 Resource Usage:"
docker stats --no-stream
echo ""

echo "🔴 Failed Services (if any):"
docker-compose -f docker-compose.prod.yaml ps | grep -v "Up" | grep -v "NAME"
echo ""

echo "📝 Recent Logs from All Services:"
docker-compose -f docker-compose.prod.yaml logs --tail=10
echo ""

echo "======================================"
echo "To view logs for specific service:"
echo "  docker-compose -f docker-compose.prod.yaml logs -f [service-name]"
echo ""
echo "To restart a specific service:"
echo "  docker-compose -f docker-compose.prod.yaml restart [service-name]"
echo ""
echo "To rebuild and restart:"
echo "  docker-compose -f docker-compose.prod.yaml up -d --build [service-name]"
echo "======================================"