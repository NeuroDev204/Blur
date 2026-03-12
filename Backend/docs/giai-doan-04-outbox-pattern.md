# Giai Đoạn 4: Outbox Pattern + Event-Driven (CHƯA TRIỂN KHAI)

## Mục tiêu

Đảm bảo **exactly-once delivery** cho events giữa các services.
Hiện tại, Kafka events được publish trực tiếp trong business logic → nếu Kafka down hoặc publish fail, data đã save nhưng event bị mất (data inconsistency).

**Outbox Pattern** giải quyết bằng cách: lưu event vào database cùng transaction với business data → scheduler đọc outbox table và publish lên Kafka.

## Trạng thái: CHƯA TRIỂN KHAI

## Vấn đề hiện tại

```
PostService.createPost()
  ├─ 1. postRepository.save(post)          ← DB commit thành công
  ├─ 2. postRepository.linkPostToAuthor()   ← DB commit thành công
  └─ 3. kafkaTemplate.send("post-events")   ← Nếu FAIL → event mất, feed không cập nhật!

CommentService.createComment()
  ├─ 1. commentRepository.save(comment)
  ├─ 2. commentRepository.linkCommentToPost()
  └─ 3. moderationProducer.submit()         ← Nếu FAIL → comment không được moderate!
```

## Giải pháp: Transactional Outbox

```
PostService.createPost()
  ├─ 1. postRepository.save(post)
  ├─ 2. postRepository.linkPostToAuthor()
  └─ 3. outboxRepository.save(outboxEvent)  ← Cùng transaction với post!
                                               Nếu DB fail → cả post lẫn event đều rollback
                                               Nếu DB thành công → event chắc chắn tồn tại

OutboxScheduler (chạy mỗi 1s)
  ├─ 1. Đọc events chưa publish từ outbox table
  ├─ 2. Publish lên Kafka
  └─ 3. Đánh dấu event đã publish (hoặc xóa)
```

## Bước 1: Tạo Outbox Entity

Vì content-service dùng Neo4j, outbox event sẽ là một Neo4j Node.

**File:** `content-service/src/main/java/com/contentservice/outbox/entity/OutboxEvent.java`

```java
package com.contentservice.outbox.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Node("outbox_event")
public class OutboxEvent {
    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    String id;

    String aggregateType;     // "Post", "Comment", "Story"
    String aggregateId;       // ID của entity (postId, commentId)
    String eventType;         // "POST_CREATED", "COMMENT_CREATED", "POST_LIKED"
    String topic;             // Kafka topic: "post-events", "comment-moderation-request"
    String payload;           // JSON payload

    boolean published;        // Đã publish lên Kafka chưa
    int retryCount;           // Số lần retry
    Instant createdAt;
    Instant publishedAt;
}
```

## Bước 2: Tạo OutboxRepository

**File:** `content-service/src/main/java/com/contentservice/outbox/repository/OutboxRepository.java`

```java
package com.contentservice.outbox.repository;

import com.contentservice.outbox.entity.OutboxEvent;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxRepository extends Neo4jRepository<OutboxEvent, String> {

    @Query("""
        MATCH (e:outbox_event)
        WHERE e.published = false AND e.retryCount < 5
        RETURN e
        ORDER BY e.createdAt ASC
        LIMIT 100
    """)
    List<OutboxEvent> findUnpublishedEvents();

    @Query("""
        MATCH (e:outbox_event)
        WHERE e.published = true AND e.publishedAt < $before
        DELETE e
    """)
    void deletePublishedEventsBefore(@Param("before") Instant before);
}
```

## Bước 3: Tạo OutboxService

**File:** `content-service/src/main/java/com/contentservice/outbox/service/OutboxService.java`

```java
package com.contentservice.outbox.service;

import com.contentservice.outbox.entity.OutboxEvent;
import com.contentservice.outbox.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OutboxService {
    OutboxRepository outboxRepository;
    ObjectMapper objectMapper;

    /**
     * Lưu event vào outbox table.
     * Gọi TRONG cùng @Transactional với business logic.
     */
    public void saveEvent(String aggregateType, String aggregateId,
                          String eventType, String topic, Object payload) {
        try {
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .topic(topic)
                    .payload(objectMapper.writeValueAsString(payload))
                    .published(false)
                    .retryCount(0)
                    .createdAt(Instant.now())
                    .build();
            outboxRepository.save(event);
            log.debug("Outbox event saved: type={}, aggregateId={}", eventType, aggregateId);
        } catch (Exception e) {
            log.error("Failed to save outbox event: type={}, aggregateId={}", eventType, aggregateId, e);
            throw new RuntimeException("Failed to save outbox event", e);
        }
    }
}
```

## Bước 4: Tạo OutboxScheduler (Polling Publisher)

**File:** `content-service/src/main/java/com/contentservice/outbox/scheduler/OutboxScheduler.java`

```java
package com.contentservice.outbox.scheduler;

import com.contentservice.outbox.entity.OutboxEvent;
import com.contentservice.outbox.repository.OutboxRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OutboxScheduler {
    OutboxRepository outboxRepository;
    KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Mỗi 1 giây: đọc events chưa publish → gửi lên Kafka → đánh dấu published.
     */
    @Scheduled(fixedDelay = 1000)
    public void publishOutboxEvents() {
        List<OutboxEvent> events = outboxRepository.findUnpublishedEvents();
        if (events.isEmpty()) return;

        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload())
                        .get();  // Đợi Kafka confirm
                event.setPublished(true);
                event.setPublishedAt(Instant.now());
                outboxRepository.save(event);
                log.debug("Published outbox event: id={}, type={}", event.getId(), event.getEventType());
            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                outboxRepository.save(event);
                log.error("Failed to publish outbox event: id={}, retry={}", event.getId(), event.getRetryCount(), e);
            }
        }
        log.info("Outbox: published {}/{} events", events.stream().filter(OutboxEvent::isPublished).count(), events.size());
    }

    /**
     * Mỗi 1 giờ: xóa events đã publish cũ hơn 7 ngày.
     */
    @Scheduled(fixedDelay = 3600000)
    public void cleanupPublishedEvents() {
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        outboxRepository.deletePublishedEventsBefore(sevenDaysAgo);
        log.info("Cleaned up published outbox events older than 7 days");
    }
}
```

## Bước 5: Sửa PostService.createPost() dùng Outbox

**File:** `content-service/src/main/java/com/contentservice/post/service/PostService.java`

Thay vì gọi `kafkaTemplate.send()` trực tiếp, gọi `outboxService.saveEvent()`:

```java
@Transactional
public PostResponse createPost(PostRequest postRequest) {
    // ... tạo post và link to author (giữ nguyên) ...

    // Thay vì: kafkaTemplate.send("post-events", ...)
    // Dùng outbox:
    Map<String, Object> eventPayload = Map.of(
        "eventType", "POST_CREATED",
        "postId", post.getId(),
        "authorId", post.getUserId(),
        "content", post.getContent(),
        "mediaUrls", post.getMediaUrls() != null ? post.getMediaUrls() : List.of()
    );
    outboxService.saveEvent("Post", post.getId(), "POST_CREATED", "post-events", eventPayload);

    return postMapper.toPostResponse(post);
}
```

## Bước 6: Sửa CommentService.createComment() dùng Outbox

```java
@Transactional
public CommentResponse createComment(String postId, CreateCommentRequest request) {
    // ... tạo comment và link (giữ nguyên) ...

    // Thay vì: moderationProducer.submit(...)
    // Dùng outbox:
    Map<String, String> moderationPayload = Map.of(
        "commentId", comment.getId(),
        "postId", postId,
        "userId", userId,
        "content", comment.getContent()
    );
    outboxService.saveEvent("Comment", comment.getId(), "COMMENT_CREATED",
                           "comment-moderation-request", moderationPayload);

    return commentMapper.toCommentResponse(comment);
}
```

## Bước 7: Enable Scheduling

**File:** `content-service/src/main/java/com/contentservice/ContentServiceApplication.java`

```java
@SpringBootApplication
@EnableFeignClients
@EnableScheduling   // ← Thêm annotation này
public class ContentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ContentServiceApplication.class, args);
    }
}
```

## Hướng dẫn Test

### Test 1: Tạo post → outbox event được lưu

```bash
# Tạo post
curl -X POST http://localhost:8888/api/post/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"content": "Test outbox pattern"}'

# Kiểm tra Neo4j - outbox_event node
# Cypher:
MATCH (e:outbox_event) WHERE e.published = false RETURN e
```

### Test 2: Scheduler publish event lên Kafka

Đợi 1-2 giây, kiểm tra lại:

```cypher
MATCH (e:outbox_event) WHERE e.published = true RETURN e
```

### Test 3: Kafka down → event không mất

```bash
# Tắt Kafka
docker-compose stop kafka

# Tạo post → outbox event lưu vào Neo4j
curl -X POST http://localhost:8888/api/post/create ...

# Bật Kafka lại
docker-compose start kafka

# Đợi scheduler retry → event được publish
```

## Checklist

- [ ] Tạo OutboxEvent entity (Neo4j Node)
- [ ] Tạo OutboxRepository với findUnpublishedEvents và deletePublishedEventsBefore
- [ ] Tạo OutboxService.saveEvent()
- [ ] Tạo OutboxScheduler với @Scheduled polling (1s)
- [ ] Thêm cleanup job xóa events cũ hơn 7 ngày
- [ ] Sửa PostService.createPost() → dùng outboxService thay vì kafkaTemplate trực tiếp
- [ ] Sửa CommentService.createComment() → dùng outboxService thay vì moderationProducer trực tiếp
- [ ] Thêm @EnableScheduling vào ContentServiceApplication
- [ ] Test: tạo post → outbox event lưu → scheduler publish → Kafka nhận
- [ ] Test: Kafka down → event vẫn lưu trong outbox → Kafka lên → retry thành công
