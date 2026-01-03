# 📅 THÁNG 1: KAFKA + AI INTEGRATION
## Chi tiết từng tuần với code mẫu

---

## TUẦN 1: KAFKA BASICS + DOCKER SETUP

### Ngày 1-2: Docker Compose Kafka

**File: `Backend/docker-compose.yml`**
```yaml
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.7.1
    container_name: blur-zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.7.1
    container_name: blur-kafka
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9093,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: blur-kafka-ui
    ports:
      - "9000:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: blur-local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9093
```

**Chạy:**
```bash
cd Backend
docker-compose up -d zookeeper kafka kafka-ui
# Mở http://localhost:9000 để xem Kafka UI
```

---

### Ngày 3-4: Tạo Common Library

**File: `Backend/blur-common-lib/pom.xml`**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.blur</groupId>
    <artifactId>blur-common-lib</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>

    <dependencies>
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-mongodb</artifactId>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
    </dependencies>
</project>
```

---

### Ngày 5-6: BaseEvent Class

**File: `blur-common-lib/src/main/java/com/blur/common/event/BaseEvent.java`**
```java
package com.blur.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.time.Instant;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEvent {
    private String eventId;
    private String eventType;
    private String aggregateId;
    private String aggregateType;
    private Instant timestamp;
    private String correlationId;
    private int version = 1;

    public void initDefaults() {
        if (eventId == null) eventId = UUID.randomUUID().toString();
        if (timestamp == null) timestamp = Instant.now();
        if (eventType == null) eventType = this.getClass().getSimpleName();
    }
}
```

**File: `blur-common-lib/src/main/java/com/blur/common/event/CommentCreatedEvent.java`**
```java
package com.blur.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CommentCreatedEvent extends BaseEvent {
    private String commentId;
    private String postId;
    private String authorId;
    private String authorName;
    private String content;
    private String parentCommentId; // null nếu là root comment

    public static CommentCreatedEvent from(String commentId, String postId,
            String authorId, String authorName, String content) {
        CommentCreatedEvent event = CommentCreatedEvent.builder()
            .commentId(commentId)
            .postId(postId)
            .authorId(authorId)
            .authorName(authorName)
            .content(content)
            .aggregateId(commentId)
            .aggregateType("Comment")
            .build();
        event.initDefaults();
        return event;
    }
}
```

**File: `blur-common-lib/src/main/java/com/blur/common/event/CommentModeratedEvent.java`**
```java
package com.blur.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CommentModeratedEvent extends BaseEvent {
    private String commentId;
    private String status;      // APPROVED, REJECTED
    private double toxicScore;  // 0.0 - 1.0
    private String reason;      // null hoặc "toxic_content"

    public boolean isApproved() {
        return "APPROVED".equals(status);
    }
}
```

---

### Ngày 7: Build và Test

```bash
cd Backend/blur-common-lib
mvn clean install
# Output: BUILD SUCCESS
```

---

## TUẦN 2: OUTBOX PATTERN

### Ngày 1-2: Outbox Entity

**File: `blur-common-lib/src/main/java/com/blur/common/outbox/OutboxEvent.java`**
```java
package com.blur.common.outbox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "outbox_events")
public class OutboxEvent {
    @Id
    private String id;
    private String aggregateType;  // "Comment", "Post"
    private String aggregateId;
    private String eventType;      // "CommentCreatedEvent"
    private String topic;          // "comment.created"
    private String payload;        // JSON string
    private Instant createdAt;
    private OutboxStatus status;

    public static OutboxEvent create(String topic, String aggregateId, 
            String aggregateType, String eventType, String payload) {
        return OutboxEvent.builder()
            .id(UUID.randomUUID().toString())
            .topic(topic)
            .aggregateId(aggregateId)
            .aggregateType(aggregateType)
            .eventType(eventType)
            .payload(payload)
            .createdAt(Instant.now())
            .status(OutboxStatus.PENDING)
            .build();
    }
}
```

**File: `blur-common-lib/src/main/java/com/blur/common/outbox/OutboxStatus.java`**
```java
package com.blur.common.outbox;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
```

---

### Ngày 3-4: Outbox Repository

**File: `blur-common-lib/src/main/java/com/blur/common/outbox/OutboxRepository.java`**
```java
package com.blur.common.outbox;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface OutboxRepository extends MongoRepository<OutboxEvent, String> {
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxStatus status);
    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
```

---

### Ngày 5-6: Outbox Publisher

**File: `blur-common-lib/src/main/java/com/blur/common/outbox/OutboxPublisher.java`**
```java
package com.blur.common.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 100) // Chạy mỗi 100ms
    public void publishPendingEvents() {
        List<OutboxEvent> events = outboxRepository
            .findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getAggregateId(), 
                    event.getPayload()).get();
                
                event.setStatus(OutboxStatus.PUBLISHED);
                outboxRepository.save(event);
                
                log.info("Published event: {} to topic: {}", 
                    event.getId(), event.getTopic());
            } catch (Exception e) {
                log.error("Failed to publish event: {}", event.getId(), e);
                event.setStatus(OutboxStatus.FAILED);
                outboxRepository.save(event);
            }
        }
    }
}
```

---

### Ngày 7: Test trong Post Service

**Thêm dependency vào `post-service/pom.xml`:**
```xml
<dependency>
    <groupId>com.blur</groupId>
    <artifactId>blur-common-lib</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## TUẦN 3: AI SERVICE KAFKA CONSUMER

### Ngày 1-2: Python Kafka Setup

**File: `Backend/ai-service-python/requirements.txt`**
```txt
fastapi==0.109.0
uvicorn==0.27.0
confluent-kafka==2.3.0
transformers==4.36.0
torch==2.1.0
pydantic==2.5.0
python-dotenv==1.0.0
```

**File: `Backend/ai-service-python/app/config.py`**
```python
import os
from dotenv import load_dotenv

load_dotenv()

KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
KAFKA_GROUP_ID = os.getenv("KAFKA_GROUP_ID", "ai-moderation-service")

# Topics
TOPIC_COMMENT_CREATED = "comment.created"
TOPIC_COMMENT_MODERATED = "comment.moderated"
TOPIC_CHAT_MESSAGE_CREATED = "chat.message.created"
TOPIC_CHAT_MESSAGE_MODERATED = "chat.message.moderated"
```

---

### Ngày 3-4: Kafka Consumer

**File: `Backend/ai-service-python/app/kafka_consumer.py`**
```python
import json
import logging
from confluent_kafka import Consumer, KafkaError
from app.config import KAFKA_BOOTSTRAP_SERVERS, KAFKA_GROUP_ID
from app.config import TOPIC_COMMENT_CREATED, TOPIC_CHAT_MESSAGE_CREATED
from app.kafka_producer import publish_moderation_result
from app.model.toxic_detector import ToxicDetector

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Initialize model
detector = ToxicDetector()

def create_consumer():
    config = {
        'bootstrap.servers': KAFKA_BOOTSTRAP_SERVERS,
        'group.id': KAFKA_GROUP_ID,
        'auto.offset.reset': 'earliest',
        'enable.auto.commit': True
    }
    consumer = Consumer(config)
    consumer.subscribe([TOPIC_COMMENT_CREATED, TOPIC_CHAT_MESSAGE_CREATED])
    return consumer

def process_message(msg):
    """Process incoming message and predict toxic"""
    try:
        topic = msg.topic()
        event = json.loads(msg.value().decode('utf-8'))
        
        logger.info(f"Received event from {topic}: {event.get('eventId')}")
        
        # Extract content
        content = event.get('content', '')
        aggregate_id = event.get('aggregateId')
        
        # Predict toxic
        result = detector.predict(content)
        
        # Determine output topic
        if 'comment' in topic:
            output_topic = 'comment.moderated'
        else:
            output_topic = 'chat.message.moderated'
        
        # Publish result
        moderation_result = {
            'eventId': f"mod-{event.get('eventId')}",
            'eventType': 'CommentModeratedEvent' if 'comment' in topic else 'ChatMessageModeratedEvent',
            'aggregateId': aggregate_id,
            'commentId': event.get('commentId'),
            'messageId': event.get('messageId'),
            'status': 'REJECTED' if result['is_toxic'] else 'APPROVED',
            'toxicScore': result['confidence'],
            'reason': 'toxic_content' if result['is_toxic'] else None
        }
        
        publish_moderation_result(output_topic, aggregate_id, moderation_result)
        logger.info(f"Moderation complete: {aggregate_id} -> {moderation_result['status']}")
        
    except Exception as e:
        logger.error(f"Error processing message: {e}")

def run_consumer():
    """Main consumer loop"""
    consumer = create_consumer()
    logger.info("AI Moderation Consumer started...")
    
    try:
        while True:
            msg = consumer.poll(1.0)
            if msg is None:
                continue
            if msg.error():
                if msg.error().code() == KafkaError._PARTITION_EOF:
                    continue
                logger.error(f"Consumer error: {msg.error()}")
                continue
            
            process_message(msg)
            
    except KeyboardInterrupt:
        logger.info("Shutting down consumer...")
    finally:
        consumer.close()

if __name__ == "__main__":
    run_consumer()
```

---

### Ngày 5-6: Kafka Producer

**File: `Backend/ai-service-python/app/kafka_producer.py`**
```python
import json
import logging
from confluent_kafka import Producer
from app.config import KAFKA_BOOTSTRAP_SERVERS

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

producer = None

def get_producer():
    global producer
    if producer is None:
        producer = Producer({
            'bootstrap.servers': KAFKA_BOOTSTRAP_SERVERS,
            'acks': 'all'
        })
    return producer

def delivery_callback(err, msg):
    if err:
        logger.error(f"Message delivery failed: {err}")
    else:
        logger.info(f"Message delivered to {msg.topic()} [{msg.partition()}]")

def publish_moderation_result(topic: str, key: str, event: dict):
    """Publish moderation result to Kafka"""
    try:
        p = get_producer()
        p.produce(
            topic=topic,
            key=key,
            value=json.dumps(event),
            callback=delivery_callback
        )
        p.flush()
    except Exception as e:
        logger.error(f"Failed to publish to {topic}: {e}")
        raise
```

---

### Ngày 7: Toxic Detector Model

**File: `Backend/ai-service-python/app/model/toxic_detector.py`**
```python
import torch
from transformers import AutoTokenizer, AutoModelForSequenceClassification
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class ToxicDetector:
    def __init__(self, model_path: str = None):
        """Initialize PhoBERT model for toxic detection"""
        self.device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
        logger.info(f"Using device: {self.device}")
        
        # Load model - thay bằng path model của bạn
        if model_path:
            self.tokenizer = AutoTokenizer.from_pretrained(model_path)
            self.model = AutoModelForSequenceClassification.from_pretrained(model_path)
        else:
            # Default: sử dụng PhoBERT base để test
            self.tokenizer = AutoTokenizer.from_pretrained("vinai/phobert-base")
            self.model = AutoModelForSequenceClassification.from_pretrained(
                "vinai/phobert-base", 
                num_labels=2
            )
        
        self.model.to(self.device)
        self.model.eval()
        logger.info("Model loaded successfully")

    def predict(self, text: str) -> dict:
        """Predict if text is toxic"""
        try:
            # Tokenize
            inputs = self.tokenizer(
                text,
                return_tensors="pt",
                truncation=True,
                max_length=256,
                padding=True
            ).to(self.device)
            
            # Predict
            with torch.no_grad():
                outputs = self.model(**inputs)
                probs = torch.softmax(outputs.logits, dim=1)
                
            # Class 1 = Toxic
            toxic_prob = probs[0][1].item()
            is_toxic = toxic_prob > 0.5
            
            return {
                'is_toxic': is_toxic,
                'confidence': toxic_prob,
                'label': 'TOXIC' if is_toxic else 'CLEAN'
            }
            
        except Exception as e:
            logger.error(f"Prediction error: {e}")
            # Fallback: không toxic
            return {
                'is_toxic': False,
                'confidence': 0.0,
                'label': 'CLEAN'
            }
```

---

## TUẦN 4: INTEGRATION + E2E TEST

### Ngày 1-2: Post Service Kafka Producer

**File: `post-service/src/main/java/com/postservice/service/CommentService.java`**
```java
package com.postservice.service;

import com.blur.common.event.CommentCreatedEvent;
import com.blur.common.outbox.OutboxEvent;
import com.blur.common.outbox.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.postservice.entity.Comment;
import com.postservice.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Comment createComment(CreateCommentRequest request, String postId, String userId) {
        // 1. Create comment với status PENDING
        Comment comment = Comment.builder()
            .postId(postId)
            .userId(userId)
            .content(request.getContent())
            .status(CommentStatus.PENDING_MODERATION) // Chờ AI check
            .build();
        
        comment = commentRepository.save(comment);
        log.info("Comment created with PENDING status: {}", comment.getId());

        // 2. Tạo event
        CommentCreatedEvent event = CommentCreatedEvent.from(
            comment.getId(),
            postId,
            userId,
            request.getAuthorName(),
            request.getContent()
        );

        // 3. Lưu vào Outbox (same transaction)
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outbox = OutboxEvent.create(
                "comment.created",
                comment.getId(),
                "Comment",
                "CommentCreatedEvent",
                payload
            );
            outboxRepository.save(outbox);
            log.info("Outbox event saved for comment: {}", comment.getId());
        } catch (Exception e) {
            log.error("Failed to save outbox event", e);
            throw new RuntimeException("Failed to create comment event", e);
        }

        return comment;
    }
}
```

---

### Ngày 3-4: Post Service Kafka Consumer

**File: `post-service/src/main/java/com/postservice/kafka/CommentModerationConsumer.java`**
```java
package com.postservice.kafka;

import com.blur.common.event.CommentModeratedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.postservice.entity.Comment;
import com.postservice.entity.CommentStatus;
import com.postservice.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentModerationConsumer {
    private final CommentRepository commentRepository;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate; // WebSocket

    @KafkaListener(topics = "comment.moderated", groupId = "post-service")
    public void handleCommentModerated(String message) {
        try {
            CommentModeratedEvent event = objectMapper.readValue(
                message, CommentModeratedEvent.class);
            
            log.info("Received moderation result for comment: {} -> {}", 
                event.getCommentId(), event.getStatus());

            // Update comment status
            Comment comment = commentRepository.findById(event.getCommentId())
                .orElseThrow(() -> new RuntimeException("Comment not found"));

            if (event.isApproved()) {
                comment.setStatus(CommentStatus.APPROVED);
                commentRepository.save(comment);
                
                // Push via WebSocket (real-time)
                messagingTemplate.convertAndSend(
                    "/topic/post/" + comment.getPostId() + "/comments",
                    comment
                );
                log.info("Comment approved and pushed via WebSocket");
            } else {
                comment.setStatus(CommentStatus.REJECTED);
                comment.setToxicScore(event.getToxicScore());
                commentRepository.save(comment);
                
                // Notify author that comment was rejected
                messagingTemplate.convertAndSendToUser(
                    comment.getUserId(),
                    "/queue/notification",
                    new CommentRejectedNotification(comment.getId(), event.getReason())
                );
                log.info("Comment rejected: {}", event.getReason());
            }

        } catch (Exception e) {
            log.error("Error processing moderation event", e);
        }
    }
}
```

---

### Ngày 5-6: Kafka Config

**File: `post-service/src/main/resources/application.yml`**
```yaml
spring:
  application:
    name: post-service
  
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: all
    consumer:
      group-id: post-service
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      auto-offset-reset: earliest
```

---

### Ngày 7: E2E Test

**Test flow hoàn chỉnh:**
```bash
# Terminal 1: Start Kafka
docker-compose up -d kafka zookeeper

# Terminal 2: Start AI Service
cd ai-service-python
python -m app.kafka_consumer

# Terminal 3: Start Post Service
cd post-service
mvn spring-boot:run

# Terminal 4: Test với curl
curl -X POST http://localhost:8084/comment/POST_ID/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"content": "Đây là comment bình thường"}'
# Expected: Comment status = APPROVED

curl -X POST http://localhost:8084/comment/POST_ID/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{"content": "Ngu như con lợn"}'
# Expected: Comment status = REJECTED
```

---

## ✅ CHECKLIST THÁNG 1

- [ ] Docker Compose với Kafka + UI
- [ ] blur-common-lib module
- [ ] BaseEvent, CommentCreatedEvent, CommentModeratedEvent
- [ ] OutboxEvent, OutboxRepository, OutboxPublisher
- [ ] AI Service Python Kafka Consumer
- [ ] AI Service Kafka Producer
- [ ] PhoBERT ToxicDetector
- [ ] Post Service Kafka integration
- [ ] E2E Test thành công

---

*Tiếp tục: Tháng 2 - Service Consolidation*
