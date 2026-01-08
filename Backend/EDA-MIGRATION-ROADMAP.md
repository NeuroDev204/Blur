# 🎓 ĐỒ ÁN TỐT NGHIỆP - EVENT-DRIVEN ARCHITECTURE
## Mạng xã hội Blur: EDA + AI Toxic Detection

> **Core:** Event-Driven Architecture với Apache Kafka  
> **AI:** PhoBERT Toxic Detection qua Kafka  
> **Deadline:** Tháng 7/2026

---

# 📊 KIẾN TRÚC EVENT-DRIVEN

## Hiện tại vs Sau EDA

```
HIỆN TẠI (REST-heavy)                    SAU EDA (Event-Driven)
========================                  ========================

┌─────────┐    REST     ┌─────────┐      ┌─────────┐            ┌─────────┐
│  Post   │────────────▶│  Notif  │      │  Post   │            │  Notif  │
│ Service │             │ Service │      │ Service │            │ Service │
└─────────┘             └─────────┘      └────┬────┘            └────▲────┘
     │                       │                │                      │
     │ REST                  │                │ Kafka                │ Kafka
     ▼                       │                ▼                      │
┌─────────┐    REST     ┌────┴────┐      ┌─────────────────────────────────┐
│   AI    │◀────────────│  Chat   │      │         KAFKA CLUSTER           │
│ Service │             │ Service │      │  post.created │ comment.created │
└─────────┘             └─────────┘      │  *.moderated  │ chat.message.*  │
                                         └─────────────────────────────────┘
                                               │                      │
                                               ▼                      ▼
                                         ┌─────────┐            ┌─────────┐
                                         │   AI    │            │  Chat   │
                                         │ Service │            │ Service │
                                         └─────────┘            └─────────┘
```

## EDA Event Flow

```
┌──────────────────────────────────────────────────────────────────────────┐
│                          EVENT-DRIVEN FLOW                                │
└──────────────────────────────────────────────────────────────────────────┘

1. COMMENT MODERATION FLOW
   User creates comment
         │
         ▼
   ┌─────────────┐     comment.created      ┌─────────────┐
   │Post Service │ ─────────────────────────▶│ AI Service  │
   │ (Producer)  │                           │ (Consumer)  │
   └─────────────┘                           └──────┬──────┘
         ▲                                          │
         │         comment.moderated                │
         └──────────────────────────────────────────┘
                           │
                           ▼
                   ┌───────────────┐
                   │ Notification  │ ──▶ WebSocket ──▶ User
                   │   Service     │
                   └───────────────┘

2. REAL-TIME FEED FLOW
   User creates post
         │
         ▼
   ┌─────────────┐      post.created       ┌─────────────┐
   │Post Service │ ────────────────────────▶│ Notification│
   │ (Producer)  │                          │ (Consumer)  │
   └─────────────┘                          └──────┬──────┘
                                                   │
                                                   ▼
                                            WebSocket Push
                                                   │
                                                   ▼
                                             Followers' Feed
```

---

# 📋 GAP ANALYSIS + EDA

## ✅ ĐÃ CÓ

| Component | Status | Notes |
|-----------|--------|-------|
| Kafka + Zookeeper | ✅ | docker-compose.yml |
| 5 Kafka Topics | ✅ | user-follow, user-like, user-comment, user-reply, user-like-story |
| 6 Kafka Handlers | ✅ | notification-service/kafka/handler/* |
| Socket.IO | ✅ | SocketHandler.java (782 lines) |
| blur-common-lib | ✅ | dto/response (ApiResponse, UserResponse) |

## ❌ CẦN LÀM CHO EDA

| Component | Priority | Description |
|-----------|----------|-------------|
| **BaseEvent** | 🔴 HIGH | Schema chuẩn cho tất cả events |
| **OutboxEvent** | 🔴 HIGH | Đảm bảo at-least-once delivery |
| **AI Kafka Integration** | 🔴 HIGH | Thay REST bằng Kafka |
| **Kafka Topics mới** | 🔴 HIGH | comment.created, *.moderated |
| **Elasticsearch** | 🟡 MED | User search |
| **Testing** | 🟡 MED | Unit + Integration tests |

---

# 📅 KẾ HOẠCH EDA CHI TIẾT

## THÁNG 1: EDA FOUNDATION

### Tuần 1-2: Event Schema + Outbox Pattern

#### 1.1 BaseEvent (EDA Core)
📁 **Tạo:** `blur-common-lib/.../event/BaseEvent.java`
```java
package com.blur.common.event;

import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.Instant;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEvent {
    // Standard EDA fields
    private String eventId;
    private String eventType;
    private String aggregateId;
    private String aggregateType;
    private Instant timestamp;
    private String correlationId;  // Tracing across services
    private int version = 1;

    public void initDefaults() {
        if (eventId == null) eventId = UUID.randomUUID().toString();
        if (timestamp == null) timestamp = Instant.now();
        if (eventType == null) eventType = this.getClass().getSimpleName();
    }
}
```

#### 1.2 Domain Events
📁 **Tạo:** `blur-common-lib/.../event/CommentCreatedEvent.java`
```java
@Data @SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class CommentCreatedEvent extends BaseEvent {
    private String commentId;
    private String postId;
    private String authorId;
    private String authorName;
    private String content;
    private String parentCommentId;
}
```

📁 **Tạo:** `blur-common-lib/.../event/CommentModeratedEvent.java`
```java
@Data @SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class CommentModeratedEvent extends BaseEvent {
    private String commentId;
    private ModerationStatus status;  // APPROVED, REJECTED
    private double toxicScore;
    private String reason;
}
```

📁 **Tạo:** `blur-common-lib/.../event/PostCreatedEvent.java`
```java
@Data @SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class PostCreatedEvent extends BaseEvent {
    private String postId;
    private String authorId;
    private String authorName;
    private String content;
    private List<String> mediaUrls;
}
```

📁 **Tạo:** `blur-common-lib/.../event/ChatMessageCreatedEvent.java`
```java
@Data @SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ChatMessageCreatedEvent extends BaseEvent {
    private String messageId;
    private String conversationId;
    private String senderId;
    private String content;
}
```

#### 1.3 Transactional Outbox Pattern (EDA Critical)
📁 **Tạo:** `blur-common-lib/.../outbox/OutboxEvent.java`
```java
package com.blur.common.outbox;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data @Builder
@Document(collection = "outbox_events")
public class OutboxEvent {
    @Id
    private String id;
    private String aggregateType;
    private String aggregateId;
    private String eventType;
    private String topic;
    private String payload;
    private Instant createdAt;
    private OutboxStatus status;
    private int retryCount;
    private String errorMessage;
}
```

📁 **Tạo:** `blur-common-lib/.../outbox/OutboxPublisher.java`
```java
package com.blur.common.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {
    private final OutboxRepository outboxRepo;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 100)
    public void publishPendingEvents() {
        var events = outboxRepo.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        
        for (var event : events) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getAggregateId(), 
                    event.getPayload()).get();
                event.setStatus(OutboxStatus.PUBLISHED);
                log.info("EDA: Published {} to {}", event.getEventType(), event.getTopic());
            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                event.setErrorMessage(e.getMessage());
                if (event.getRetryCount() >= 3) {
                    event.setStatus(OutboxStatus.FAILED);
                    // TODO: Send to DLQ
                }
                log.error("EDA: Failed to publish {}", event.getId(), e);
            }
            outboxRepo.save(event);
        }
    }
}
```

---

### Tuần 3-4: AI Kafka Integration

#### 1.4 Kafka Topics Setup
```bash
# Tạo topics cho EDA
docker exec blur-kafka kafka-topics --create --bootstrap-server localhost:9092 \
  --topic comment.created --partitions 3 --replication-factor 1

docker exec blur-kafka kafka-topics --create --bootstrap-server localhost:9092 \
  --topic comment.moderated --partitions 3 --replication-factor 1

docker exec blur-kafka kafka-topics --create --bootstrap-server localhost:9092 \
  --topic chat.message.created --partitions 3 --replication-factor 1

docker exec blur-kafka kafka-topics --create --bootstrap-server localhost:9092 \
  --topic chat.message.moderated --partitions 3 --replication-factor 1

docker exec blur-kafka kafka-topics --create --bootstrap-server localhost:9092 \
  --topic post.created --partitions 6 --replication-factor 1

docker exec blur-kafka kafka-topics --create --bootstrap-server localhost:9092 \
  --topic story.created --partitions 3 --replication-factor 1
```

#### 1.5 AI Service Kafka Consumer (thay REST)
📁 **Tạo:** `ai-service/.../kafka/ContentModerationConsumer.java`
```java
package com.blur.aiservice.kafka;

import com.blur.common.event.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentModerationConsumer {
    private final ToxicDetectionService toxicService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "comment.created", groupId = "ai-moderation-service")
    public void handleComment(String message) {
        try {
            var event = objectMapper.readValue(message, CommentCreatedEvent.class);
            log.info("EDA: Received comment.created - {}", event.getCommentId());
            
            // AI Prediction
            var result = toxicService.detectToxic(event.getContent());
            
            // Publish moderation result
            var moderated = CommentModeratedEvent.builder()
                .commentId(event.getCommentId())
                .status(result.isToxic() ? ModerationStatus.REJECTED : ModerationStatus.APPROVED)
                .toxicScore(result.getScore())
                .reason(result.isToxic() ? "toxic_content" : null)
                .correlationId(event.getCorrelationId())
                .aggregateId(event.getCommentId())
                .build();
            moderated.initDefaults();
            
            kafkaTemplate.send("comment.moderated", event.getCommentId(),
                objectMapper.writeValueAsString(moderated));
            
            log.info("EDA: Published comment.moderated - {} -> {}", 
                event.getCommentId(), moderated.getStatus());
                
        } catch (Exception e) {
            log.error("EDA: Error processing comment.created", e);
        }
    }

    @KafkaListener(topics = "chat.message.created", groupId = "ai-moderation-service")
    public void handleChatMessage(String message) {
        // Similar logic for chat messages
    }
}
```

#### 1.6 Post Service EDA Producer
📁 **Sửa:** `post-service/.../service/CommentService.java`
```java
// THÊM imports
import com.blur.common.event.CommentCreatedEvent;
import com.blur.common.outbox.*;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepo;
    private final OutboxRepository outboxRepo;  // NEW
    private final ObjectMapper objectMapper;

    @Transactional
    public CommentResponse createComment(CreateCommentRequest req, String postId, String userId) {
        // 1. Save comment với status PENDING
        Comment comment = Comment.builder()
            .postId(postId)
            .userId(userId)
            .content(req.getContent())
            .status(CommentStatus.PENDING_MODERATION)
            .build();
        comment = commentRepo.save(comment);

        // 2. EDA: Publish event qua Outbox
        CommentCreatedEvent event = CommentCreatedEvent.builder()
            .commentId(comment.getId())
            .postId(postId)
            .authorId(userId)
            .content(comment.getContent())
            .aggregateId(comment.getId())
            .aggregateType("Comment")
            .build();
        event.initDefaults();

        outboxRepo.save(OutboxEvent.builder()
            .id(UUID.randomUUID().toString())
            .aggregateType("Comment")
            .aggregateId(comment.getId())
            .eventType("CommentCreatedEvent")
            .topic("comment.created")
            .payload(objectMapper.writeValueAsString(event))
            .createdAt(Instant.now())
            .status(OutboxStatus.PENDING)
            .build());

        return CommentResponse.from(comment);
    }
}
```

#### 1.7 Post Service EDA Consumer
📁 **Tạo:** `post-service/.../kafka/CommentModerationConsumer.java`
```java
package com.postservice.kafka;

import com.blur.common.event.CommentModeratedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommentModerationConsumer {
    private final CommentRepository commentRepo;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "comment.moderated", groupId = "post-service")
    public void handleModeration(String message) {
        try {
            var event = objectMapper.readValue(message, CommentModeratedEvent.class);
            log.info("EDA: Received comment.moderated - {}", event.getCommentId());
            
            var comment = commentRepo.findById(event.getCommentId()).orElseThrow();
            
            if (event.getStatus() == ModerationStatus.APPROVED) {
                comment.setStatus(CommentStatus.APPROVED);
                log.info("EDA: Comment APPROVED - {}", comment.getId());
            } else {
                comment.setStatus(CommentStatus.REJECTED);
                comment.setToxicScore(event.getToxicScore());
                log.info("EDA: Comment REJECTED - {} (score: {})", 
                    comment.getId(), event.getToxicScore());
            }
            
            commentRepo.save(comment);
            
        } catch (Exception e) {
            log.error("EDA: Error processing comment.moderated", e);
        }
    }
}
```

---

## THÁNG 2: REAL-TIME FEED (EDA)

### Tuần 5-6: Post/Story Real-time

#### 2.1 Post Created Event
📁 **Sửa:** `post-service/.../service/PostService.java`
```java
@Transactional
public PostResponse createPost(PostRequest req, String userId) {
    Post post = postRepo.save(new Post(req, userId));

    // EDA: Publish for real-time feed
    PostCreatedEvent event = PostCreatedEvent.builder()
        .postId(post.getId())
        .authorId(userId)
        .content(post.getContent())
        .aggregateId(post.getId())
        .build();
    event.initDefaults();

    outboxRepo.save(OutboxEvent.create("post.created", post.getId(),
        objectMapper.writeValueAsString(event)));

    return PostResponse.from(post);
}
```

#### 2.2 Notification Service - Feed Consumer
📁 **Tạo:** `notification-service/.../kafka/FeedEventConsumer.java`
```java
package com.blur.notificationservice.kafka;

import com.blur.common.event.PostCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeedEventConsumer {
    private final SimpMessagingTemplate messaging;
    private final UserFollowService userFollowService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "post.created", groupId = "notification-service")
    public void handlePostCreated(String message) {
        try {
            var event = objectMapper.readValue(message, PostCreatedEvent.class);
            log.info("EDA: Received post.created - {}", event.getPostId());
            
            // Get author's followers
            List<String> followers = userFollowService.getFollowers(event.getAuthorId());
            
            // Push to each follower via WebSocket
            FeedItem feedItem = FeedItem.from(event);
            for (String followerId : followers) {
                messaging.convertAndSendToUser(followerId, "/queue/feed", feedItem);
            }
            
            log.info("EDA: Pushed post to {} followers", followers.size());
            
        } catch (Exception e) {
            log.error("EDA: Error processing post.created", e);
        }
    }

    @KafkaListener(topics = "story.created", groupId = "notification-service")
    public void handleStoryCreated(String message) {
        // Similar logic for stories
    }
}
```

---

## THÁNG 3: ELASTICSEARCH + TESTING

### Tuần 9-10: Elasticsearch với Kafka Sync

📁 **Thêm docker-compose.yml:**
```yaml
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
    ports:
      - "9200:9200"
```

#### 3.1 ES Sync via Kafka
📁 **Tạo:** `profile-service/.../kafka/UserIndexConsumer.java`
```java
@Component
@RequiredArgsConstructor
public class UserIndexConsumer {
    private final UserSearchService searchService;

    @KafkaListener(topics = "user.created", groupId = "user-search-indexer")
    public void handleUserCreated(String message) {
        // Index user to Elasticsearch
    }

    @KafkaListener(topics = "user.updated", groupId = "user-search-indexer")
    public void handleUserUpdated(String message) {
        // Update user in Elasticsearch
    }
}
```

### Tuần 11-12: Testing

📁 **Tạo:** `post-service/src/test/java/.../kafka/CommentModerationConsumerTest.java`
```java
@ExtendWith(MockitoExtension.class)
class CommentModerationConsumerTest {
    @Mock private CommentRepository commentRepo;
    @InjectMocks private CommentModerationConsumer consumer;

    @Test
    void handleModeration_shouldApproveComment() throws Exception {
        String message = """
            {"commentId":"c1","status":"APPROVED","toxicScore":0.1}
        """;
        Comment comment = new Comment();
        comment.setId("c1");
        when(commentRepo.findById("c1")).thenReturn(Optional.of(comment));

        consumer.handleModeration(message);

        assertEquals(CommentStatus.APPROVED, comment.getStatus());
        verify(commentRepo).save(comment);
    }
}
```

---

# ✅ CHECKLIST EDA

## Tháng 1: Foundation
- [ ] BaseEvent class
- [ ] CommentCreatedEvent, CommentModeratedEvent
- [ ] PostCreatedEvent, ChatMessageCreatedEvent
- [ ] OutboxEvent, OutboxStatus
- [ ] OutboxRepository, OutboxPublisher
- [ ] Kafka topics: comment.*, chat.message.*, post.*
- [ ] AI Service Kafka Consumer
- [ ] Post Service Kafka Producer/Consumer

## Tháng 2: Real-time
- [ ] Post/Story events
- [ ] Notification Service Feed Consumer
- [ ] WebSocket push to followers

## Tháng 3: Search + Testing
- [ ] Elasticsearch setup
- [ ] User search với Kafka sync
- [ ] Unit tests
- [ ] Integration tests

## Tháng 4-6: Báo cáo
- [ ] Viết báo cáo 70-100 trang
- [ ] Diagrams (Architecture, Sequence, ERD)
- [ ] Demo

---

# 🎯 KẾT QUẢ MONG ĐỢI

| Metric | Before | After EDA |
|--------|--------|-----------|
| REST calls between services | 15+ | 4 (chỉ auth) |
| Kafka topics | 5 | 15+ |
| Real-time features | Chat only | Feed, Notifications, Chat |
| AI integration | REST (sync) | Kafka (async) |
| Test coverage | 0% | >50% |

---

*Event-Driven Architecture Migration Plan*
*Based on actual codebase analysis*
