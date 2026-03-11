# Code full: docker

## `Backend/docker/build-and-push.sh`
```bash
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
    echo "ðŸ“¦ Building $SERVICE_NAME..."
    docker build -t $DOCKER_USER/$SERVICE_NAME:$VERSION $CONTEXT_PATH

    if [ $? -eq 0 ]; then
        echo "âœ… Build successful: $SERVICE_NAME"
        echo "ðŸš€ Pushing $SERVICE_NAME..."
        docker push $DOCKER_USER/$SERVICE_NAME:$VERSION
        echo "âœ… Push successful: $SERVICE_NAME"
    else
        echo "âŒ Failed to build: $SERVICE_NAME"
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
echo "âœ… ALL DONE!"
```

## `Backend/docker/docker-compose.prod.yaml`
```yaml
networks:
  blur-network:
    driver: bridge

services:
  neo4j:
    image: neo4j:latest
    container_name: neo4j
    networks:
      - blur-network
    ports:
      - "7474:7474"
      - "7687:7687"
    environment:
      NEO4J_AUTH: neo4j/12345678
    volumes:
      - neo4j_data:/data
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "wget --no-verbose --tries=1 --spider localhost:7474 || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 40s

  mongo:
    image: mongo:6.0
    container_name: mongo
    networks:
      - blur-network
    ports:
      - "27017:27017"
    environment:
      MONGO_INITDB_ROOT_USERNAME: root
      MONGO_INITDB_ROOT_PASSWORD: root
    volumes:
      - mongo_data:/data/db
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "mongosh", "--eval", "db.adminCommand('ping')"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

  redis:
    image: redis:alpine
    container_name: redis
    networks:
      - blur-network
    ports:
      - "6379:6379"
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 3

  zookeeper:
    image: confluentinc/cp-zookeeper:7.7.1
    container_name: zookeeper
    networks:
      - blur-network
    ports:
      - "2181:2181"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "nc", "-z", "localhost", "2181"]
      interval: 10s
      timeout: 5s
      retries: 5

  kafka:
    image: confluentinc/cp-kafka:7.7.1
    container_name: kafka
    networks:
      - blur-network
    depends_on:
      zookeeper:
        condition: service_healthy
    ports:
      - "9092:9092"
      - "9093:9093"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9093,PLAINTEXT_HOST://0.0.0.0:9092
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9093,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_LOG_RETENTION_HOURS: 24
      KAFKA_DELETE_TOPIC_ENABLE: "true"
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "kafka-topics --bootstrap-server localhost:9092 --list || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 5
      start_period: 60s

  profile-service:
    image: vansy1001/profile-service:1.2
    container_name: profile-service
    networks:
      - blur-network
    ports:
      - "8081:8081"
    environment:
      NEO4J_URI: bolt://neo4j:7687
      NEO4J_PASSWORD: "12345678"
      REDIS_HOST: redis
      NOTIFICATION_SERVICE_URL: http://notification-service:8082/notification
    depends_on:
      neo4j:
        condition: service_healthy
      redis:
        condition: service_healthy
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8081/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 90s

  notification-service:
    image: vansy1001/notification-service:1.2
    container_name: notification-service
    networks:
      - blur-network
    ports:
      - "8082:8082"
    environment:
      MONGODB_URI: mongodb://root:root@mongo:27017/notification-service?authSource=admin
      REDIS_HOST: redis
      KAFKA_BOOTSTRAP_SERVERS: kafka:9093
      PROFILE_SERVICE_URL: http://profile-service:8081
    depends_on:
      kafka:
        condition: service_healthy
      mongo:
        condition: service_healthy
      redis:
        condition: service_healthy
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8082/notification/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 90s

  chat-service:
    image: vansy1001/chat-service:1.2
    container_name: chat-service
    networks:
      - blur-network
    ports:
      - "8083:8083"
      - "8099:8099"
    environment:
      MONGODB_URI: mongodb://root:root@mongo:27017/chat-service?authSource=admin
      REDIS_HOST: redis
      PROFILE_SERVICE_URL: http://profile-service:8081
    depends_on:
      mongo:
        condition: service_healthy
      redis:
        condition: service_healthy
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8083/chat/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 90s

  post-service:
    image: vansy1001/post-service:1.2
    container_name: post-service
    networks:
      - blur-network
    ports:
      - "8084:8084"
    environment:
      NEO4J_URI: bolt://neo4j:7687
      NEO4J_PASSWORD: "12345678"
      REDIS_HOST: redis
      PROFILE_SERVICE_URL: http://profile-service:8081
      NOTIFICATION_SERVICE_URL: http://notification-service:8082/notification
    depends_on:
      neo4j:
        condition: service_healthy
      redis:
        condition: service_healthy
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8084/post/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 90s

  story-service:
    image: vansy1001/story-service:1.2
    container_name: story-service
    networks:
      - blur-network
    ports:
      - "8086:8086"
    environment:
      NEO4J_URI: bolt://neo4j:7687
      NEO4J_PASSWORD: "12345678"
      REDIS_HOST: redis
      PROFILE_SERVICE_URL: http://profile-service:8081
      NOTIFICATION_SERVICE_URL: http://notification-service:8082/notification
    depends_on:
      neo4j:
        condition: service_healthy
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8086/story/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 90s

  api-gateway:
    image: vansy1001/api-gateway:1.2
    container_name: api-gateway
    networks:
      - blur-network
    ports:
      - "8888:8888"
    environment:
      USER_SERVICE_URL: http://profile-service:8081
      NOTIFICATION_SERVICE_URL: http://notification-service:8082
      CHAT_SERVICE_URL: http://chat-service:8083
      POST_SERVICE_URL: http://post-service:8084
      STORY_SERVICE_URL: http://story-service:8086
      CORS_ALLOWED_ORIGIN: http://localhost
    depends_on:
      profile-service:
        condition: service_healthy
      notification-service:
        condition: service_healthy
      chat-service:
        condition: service_healthy
      post-service:
        condition: service_healthy
      story-service:
        condition: service_healthy
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost:8888/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 120s

  nginx:
    image: vansy1001/blur-frontend:1.2
    container_name: nginx
    networks:
      - blur-network
    ports:
      - "80:80"
    depends_on:
      api-gateway:
        condition: service_healthy
      chat-service:
        condition: service_healthy
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "wget", "--no-verbose", "--tries=1", "--spider", "http://localhost/"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 10s

volumes:
  neo4j_data:
  mongo_data:
```

## `Backend/docker/test-service.sh`
```bash
#!/bin/bash

echo "ðŸ§ª Testing All Services"
echo "======================="
echo ""

test_endpoint() {
    local NAME=$1
    local URL=$2

    echo -n "Testing $NAME... "
    if curl -sf "$URL" > /dev/null 2>&1; then
        echo "âœ… OK"
        return 0
    else
        echo "âŒ FAILED"
        return 1
    fi
}

echo "Backend Services:"
test_endpoint "Profile Service" "http://localhost:8081/actuator/health"
test_endpoint "Notification Service" "http://localhost:8082/notification/actuator/health"
test_endpoint "Chat Service" "http://localhost:8083/chat/actuator/health"
test_endpoint "Post Service" "http://localhost:8084/post/actuator/health"
test_endpoint "Story Service" "http://localhost:8086/stories/actuator/health"
test_endpoint "API Gateway" "http://localhost:8888/actuator/health"

echo ""
echo "Frontend:"
test_endpoint "React App" "http://localhost"

echo ""
echo "======================================"
echo "âœ… All tests completed!"
echo "======================================"
```

## `Backend/docker-compose.yml`
```yaml
services:
  neo4j:
    image: neo4j:latest
    container_name: neo4j
    ports:
      - "7474:7474"
      - "7687:7687"
    environment:
      - NEO4J_AUTH=neo4j/12345678

  mongo:
    image: mongo:6.0
    container_name: mongo
    ports:
      - "27017:27017"
    environment:
      - MONGO_INITDB_ROOT_USERNAME=root
      - MONGO_INITDB_ROOT_PASSWORD=root

  redis:
    image: redis:alpine
    container_name: redis
    ports:
      - "6379:6379"

  zookeeper:
    image: confluentinc/cp-zookeeper:7.7.1
    container_name: zookeeper
    ports:
      - "2181:2181"
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000

  kafka:
    image: confluentinc/cp-kafka:7.7.1
    container_name: kafka
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
      - "9093:9093"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181

      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9093,PLAINTEXT_HOST://0.0.0.0:9092
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9093,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_LOG_RETENTION_HOURS: 24
      KAFKA_DELETE_TOPIC_ENABLE: "true"

```

