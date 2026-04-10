#!/bin/bash

echo "🧹 Cleaning up Docker resources..."
echo ""

read -p "This will remove all stopped containers, unused networks, and dangling images. Continue? (y/n) " -n 1 -r
echo ""

if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Stopping all containers..."
    docker-compose -f docker-compose.prod.yaml down

    echo ""
    echo "Removing stopped containers..."
    docker container prune -f

    echo ""
    echo "Removing unused networks..."
    docker network prune -f

    echo ""
    echo "Removing dangling images..."
    docker image prune -f

    echo ""
    echo "✅ Cleanup completed!"

    echo ""
    echo "Current disk usage:"
    docker system df
else
    echo "Cleanup cancelled."
fi