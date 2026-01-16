# Event-Driven Architecture Migration Plan
## Blur Social Network - REST to Kafka Migration

> **Mục tiêu**: Chuyển đổi hệ thống từ REST-based synchronous communication sang Event-Driven Architecture (EDA) sử dụng Apache Kafka, cải thiện performance, scalability và fault tolerance.

---

# 📊 PHẦN 1: PHÂN TÍCH KIẾN TRÚC HIỆN TẠI

## 1.1 Tổng quan Microservices

| Service | Port | Database | Vai trò | Mô tả chi tiết |
|---------|------|----------|---------|----------------|
| **API Gateway** | 8888 | - | Route requests, JWT validation | Cổng vào duy nhất, chuyển tiếp request tới các service |
| **Identity Service** | 8080 | MySQL | Auth, User management | **Source of Truth** cho User, quản lý đăng nhập/đăng ký |
| **Profile Service** | 8081 | Neo4j | User profiles, Follow | Quản lý thông tin profile, quan hệ follow (graph DB) |
| **Notification Service** | 8082 | MongoDB | Real-time notifications | Gửi thông báo qua WebSocket, Email |
| **Chat Service** | 8083/8099 | MongoDB | Messaging, Calls | Nhắn tin real-time, voice/video calls |
| **Post Service** | 8084 | MongoDB | Posts, Comments, Likes | Bài viết, bình luận, thích |
| **Story Service** | 8086 | MongoDB | Stories management | Stories 24h như Instagram |
| **AI Service** | - | - | Toxic detection | Phát hiện nội dung độc hại bằng PhoBERT |

## 1.2 Các File Quan Trọng Trong Hệ Thống Hiện Tại

### 1.2.1 HTTP Clients (Feign) - Gọi REST giữa các services

| File | Vị trí | Tác dụng |
|------|--------|----------|
| `NotificationClient.java` | post-service | Gọi REST tới notification-service để gửi thông báo |
| `ProfileClient.java` | post-service, chat-service, story-service | Lấy thông tin profile user từ profile-service |
| `IdentityClient.java` | các services | Lấy thông tin user (email, username) từ identity-service |
| `AiServiceClient.java` | chat-service | Gọi AI service để kiểm tra nội dung độc hại |

### 1.2.2 Service Classes - Logic nghiệp vụ

| File | Vị trí | Tác dụng | Các hàm quan trọng |
|------|--------|----------|-------------------|
| `PostService.java` | post-service | Xử lý CRUD bài viết | `createPost()`, `likePost()`, `deletePost()` |
| `CommentService.java` | post-service | Xử lý bình luận | `createComment()`, `deleteComment()` |
| `UserProfileService.java` | profile-service | Quản lý profile & follow | `followUser()`, `unfollowUser()` |
| `StoryService.java` | story-service | Quản lý stories | `createStory()`, `deleteStoryById()` |
| `NotificationService.java` | notification-service | Lưu & gửi notifications | `save()`, `handleEvent()` |

### 1.2.3 Kafka Handlers Hiện Có

| File | Vị trí | Tác dụng |
|------|--------|----------|
| `EventListener.java` | notification-service/kafka/consumer | Nhận events từ Kafka, phân phối tới handlers |
| `CommentEventHandler.java` | notification-service/kafka/handler | Xử lý event comment, tạo notification |
| `FollowEventHandler.java` | notification-service/kafka/handler | Xử lý event follow, tạo notification |
| `LikePostEventHandler.java` | notification-service/kafka/handler | Xử lý event like post, tạo notification |
| `LikeStoryEventHandler.java` | notification-service/kafka/handler | Xử lý event like story, tạo notification |
| `ReplyCommentEventHandler.java` | notification-service/kafka/handler | Xử lý event reply comment, tạo notification |

## 1.3 Vấn đề với Kiến trúc Hiện Tại

### 🔴 Tight Coupling - Phân tích từng hàm

#### `PostService.likePost()` - 3 REST calls đồng bộ

```java
// FILE: post-service/service/PostService.java
// HÀM: likePost(String postId)
// TÁC DỤNG: Xử lý khi user like bài viết

@Transactional
public String likePost(String postId) {
    // Bước 1: Lấy user hiện tại từ JWT token
    var userId = authentication.getName();
    
    // Bước 2: Kiểm tra post tồn tại
    var post = postRepository.findById(postId);
    
    // Bước 3: Lưu like vào database
    PostLike like = PostLike.builder()
        .userId(userId)
        .postId(postId)
        .build();
    postLikeRepository.save(like);
    
    // ⚠️ VẤN ĐỀ: 2 REST calls đồng bộ
    var sender = identityClient.getUser(userId);      // REST call #1
    var receiver = identityClient.getUser(post.getUserId()); // REST call #2
    
    // ⚠️ VẤN ĐỀ: REST call #3 - Nếu fail thì like vẫn đã save!
    notificationClient.sendLikePostNotification(event); // REST call #3
    
    return "Post liked successfully";
}
/*
 * VẤN ĐỀ:
 * 1. Latency cao: Chờ 3 REST calls tuần tự
 * 2. Cascade failure: Nếu IdentityService down → Like FAIL
 * 3. Inconsistency: Like saved nhưng notification fail → User không biết
 */
```

#### `CommentService.createComment()` - 4 REST calls đồng bộ

```java
// FILE: post-service/service/CommentService.java
// HÀM: createComment(CreateCommentRequest request, String postId)
// TÁC DỤNG: Tạo bình luận mới cho bài viết

public CommentResponse createComment(CreateCommentRequest request, String postId) {
    String userId = authentication.getName();
    
    // REST call #1: Lấy profile người comment
    var profile = profileClient.getProfile(userId);
    
    // Tạo comment
    Comment comment = Comment.builder()
        .content(request.getContent())
        .userId(userId)
        .firstName(profile.getResult().getFirstName())
        .postId(postId)
        .build();
    comment = commentRepository.save(comment);
    
    // REST call #2 & #3: Lấy profile sender và receiver
    var senderProfile = profileClient.getProfile(senderUserId);
    var receiverProfile = profileClient.getProfile(receiverUserId);
    
    // REST call #4: Lấy info receiver từ Identity
    var receiverIdentity = identityClient.getUser(receiverUserId);
    
    // REST call #5: Gửi notification
    notificationClient.sendCommentNotification(event);
    
    return commentMapper.toCommentResponse(comment);
}
/*
 * VẤN ĐỀ:
 * 1. 5 REST calls cho 1 action đơn giản
 * 2. User phải chờ tất cả REST calls hoàn thành
 * 3. Bất kỳ service nào fail → toàn bộ comment creation fail
 */
```

---

# 📊 PHẦN 2: KIẾN TRÚC EVENT-DRIVEN ĐỀ XUẤT

## 2.1 Kafka trong Hệ Thống

| Vai trò | Mô tả | Ví dụ |
|---------|-------|-------|
| **Event Bus** | Trung tâm truyền tải events | PostService → Kafka → NotificationService |
| **Decoupling** | Tách biệt producer/consumer | PostService không cần biết NotificationService tồn tại |
| **Async Processing** | Xử lý bất đồng bộ | User like xong, notification gửi sau |
| **Load Leveling** | Buffer khi traffic spike | 1000 likes/giây → Kafka buffer → Consumer xử lý dần |

## 2.2 Danh sách Topics và Events

| Topic | Producer | Consumer | Event | Mô tả |
|-------|----------|----------|-------|-------|
| `post.liked` | PostService | NotificationService | PostLikedEvent | Khi user like bài viết |
| `comment.created` | PostService | NotificationService, AIService | CommentCreatedEvent | Khi tạo comment mới |
| `comment.moderated` | AIService | PostService | ContentModeratedEvent | Kết quả kiểm duyệt AI |
| `user.followed` | ProfileService | NotificationService | UserFollowedEvent | Khi user follow người khác |
| `story.liked` | StoryService | NotificationService | StoryLikedEvent | Khi like story |

---

# 📊 PHẦN 3: CODE MẪU CHI TIẾT VỚI GIẢI THÍCH

## 3.1 Domain Events - Các Class Event

### 3.1.1 BaseEvent.java - Class cha cho tất cả events

```java
/*
 * FILE: blur-common-lib/event/BaseEvent.java
 * TÁC DỤNG: Class cha abstract chứa các fields chung cho mọi event
 * 
 * TẠI SAO CẦN?
 * - Đảm bảo mọi event đều có eventId để tracking
 * - correlationId để trace event qua nhiều services
 * - timestamp để ordering và debugging
 */
package com.blur.common.event;

import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.Instant;
import java.util.UUID;

@Data
@SuperBuilder  // Cho phép builder pattern với inheritance
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEvent {
    
    /**
     * ID duy nhất của event
     * Dùng để: Idempotency check - tránh xử lý trùng lặp
     */
    private String eventId;
    
    /**
     * Tên loại event (tự động lấy từ class name)
     * Dùng để: Consumer biết cách deserialize
     */
    private String eventType;
    
    /**
     * ID của entity chính (postId, commentId, userId)
     * Dùng để: Kafka partition key → đảm bảo ordering
     */
    private String aggregateId;
    
    /**
     * Loại entity (Post, Comment, User)
     * Dùng để: Logging và debugging
     */
    private String aggregateType;
    
    /**
     * Thời điểm event được tạo
     * Dùng để: Ordering, audit trail
     */
    private Instant timestamp;
    
    /**
     * ID để trace event qua nhiều services
     * Dùng để: Distributed tracing (liên kết với original request)
     */
    private String correlationId;
    
    /**
     * Phiên bản schema của event
     * Dùng để: Schema evolution - backward compatibility
     */
    private int version = 1;

    /**
     * Khởi tạo các giá trị mặc định
     * GỌI Ở ĐÂU: Trước khi publish event
     */
    public void initDefaults() {
        if (eventId == null) eventId = UUID.randomUUID().toString();
        if (timestamp == null) timestamp = Instant.now();
        if (eventType == null) eventType = this.getClass().getSimpleName();
    }
}
```

### 3.1.2 CommentCreatedEvent.java - Event khi tạo comment

```java
/*
 * FILE: blur-common-lib/event/CommentCreatedEvent.java
 * TÁC DỤNG: Event được publish khi user tạo comment mới
 * 
 * PRODUCER: PostService (trong hàm createComment)
 * CONSUMERS: 
 *   - NotificationService: Gửi thông báo cho chủ bài viết
 *   - AIService: Kiểm tra nội dung độc hại
 */
package com.blur.common.event;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)  // Kế thừa equals/hashCode từ BaseEvent
public class CommentCreatedEvent extends BaseEvent {
    
    // ===== THÔNG TIN COMMENT =====
    
    /**
     * ID của comment vừa tạo
     * Dùng để: Consumer có thể query chi tiết nếu cần
     */
    private String commentId;
    
    /**
     * ID bài viết chứa comment
     * Dùng để: Link notification tới post, làm partition key
     */
    private String postId;
    
    /**
     * Nội dung comment
     * Dùng để: AI kiểm duyệt, hiển thị preview trong notification
     */
    private String content;
    
    /**
     * ID của comment cha (nếu là reply)
     * Dùng để: Phân biệt comment gốc vs reply
     */
    private String parentCommentId;
    
    // ===== THÔNG TIN NGƯỜI COMMENT (SENDER) =====
    
    /**
     * userId của người comment
     */
    private String authorId;
    
    /**
     * Tên hiển thị của người comment
     * Dùng để: Notification content "Nguyễn Văn A đã bình luận..."
     */
    private String authorName;
    private String authorFirstName;
    private String authorLastName;
    
    /**
     * Avatar người comment
     * Dùng để: Hiển thị trong notification UI
     */
    private String authorImageUrl;
    
    // ===== THÔNG TIN CHỦ BÀI VIẾT (RECEIVER) =====
    
    /**
     * userId của chủ bài viết (người nhận notification)
     */
    private String postOwnerId;
    
    /**
     * Email để gửi notification offline
     */
    private String postOwnerEmail;
    
    /**
     * Tên để hiển thị trong email
     */
    private String postOwnerName;
}
```

### 3.1.3 PostLikedEvent.java - Event khi like bài viết

```java
/*
 * FILE: blur-common-lib/event/PostLikedEvent.java
 * TÁC DỤNG: Event được publish khi user like bài viết
 * 
 * PRODUCER: PostService (trong hàm likePost)
 * CONSUMER: NotificationService
 */
package com.blur.common.event;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PostLikedEvent extends BaseEvent {
    
    /**
     * ID bài viết được like
     */
    private String postId;
    
    // ===== NGƯỜI LIKE =====
    private String likerId;
    private String likerName;
    private String likerFirstName;
    private String likerLastName;
    private String likerImageUrl;
    
    // ===== CHỦ BÀI VIẾT =====
    private String postOwnerId;
    private String postOwnerEmail;
    private String postOwnerName;
}
```

### 3.1.4 UserFollowedEvent.java - Event khi follow user

```java
/*
 * FILE: blur-common-lib/event/UserFollowedEvent.java
 * TÁC DỤNG: Event được publish khi user follow người khác
 * 
 * PRODUCER: ProfileService (trong hàm followUser)
 * CONSUMER: NotificationService
 */
package com.blur.common.event;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserFollowedEvent extends BaseEvent {
    
    // ===== NGƯỜI FOLLOW =====
    private String followerId;        // userId
    private String followerProfileId; // profile ID trong Neo4j
    private String followerName;
    private String followerFirstName;
    private String followerLastName;
    private String followerImageUrl;
    
    // ===== NGƯỜI ĐƯỢC FOLLOW =====
    private String followedUserId;
    private String followedProfileId;
    private String followedEmail;
    private String followedName;
}
```

### 3.1.5 ContentModeratedEvent.java - Kết quả kiểm duyệt AI

```java
/*
 * FILE: blur-common-lib/event/ContentModeratedEvent.java
 * TÁC DỤNG: Event chứa kết quả kiểm duyệt nội dung từ AI
 * 
 * PRODUCER: AIService (sau khi phân tích content)
 * CONSUMERS: 
 *   - PostService: Cập nhật trạng thái comment (APPROVED/REJECTED)
 *   - ChatService: Cập nhật trạng thái message
 */
package com.blur.common.event;

import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ContentModeratedEvent extends BaseEvent {
    
    /**
     * ID của content được kiểm duyệt (commentId hoặc messageId)
     */
    private String contentId;
    
    /**
     * Loại content: "COMMENT", "CHAT_MESSAGE", "POST"
     */
    private String contentType;
    
    /**
     * Kết quả kiểm duyệt
     */
    private ModerationStatus status;
    
    /**
     * Điểm độc hại (0.0 - 1.0)
     * Threshold thường dùng: 0.7
     */
    private double toxicScore;
    
    /**
     * Lý do reject (nếu có)
     * Ví dụ: "hate_speech", "violence", "spam"
     */
    private String reason;
    
    /**
     * Link với event gốc (CommentCreatedEvent.eventId)
     * Dùng để: Trace flow từ create → moderate → update
     */
    private String originalCorrelationId;
    
    /**
     * Enum các trạng thái kiểm duyệt
     */
    public enum ModerationStatus {
        APPROVED,       // Nội dung OK
        REJECTED,       // Nội dung vi phạm, ẩn đi
        PENDING_REVIEW  // Cần human review
    }
}
```

---

## 3.2 Outbox Pattern - Đảm Bảo Event Delivery

### 3.2.1 OutboxEvent.java - Entity lưu event chờ publish

```java
/*
 * FILE: blur-common-lib/outbox/OutboxEvent.java
 * TÁC DỤNG: Entity MongoDB lưu events chờ được publish lên Kafka
 * 
 * TẠI SAO CẦN OUTBOX PATTERN?
 * 
 * VẤN ĐỀ KHÔNG DÙNG OUTBOX:
 *   1. Save comment to DB  ✅
 *   2. Publish to Kafka    ❌ Network fail!
 *   → Comment đã save nhưng event mất → Inconsistency
 * 
 * GIẢI PHÁP VỚI OUTBOX:
 *   1. BEGIN TRANSACTION
 *   2. Save comment to DB
 *   3. Save OutboxEvent to DB (cùng transaction)
 *   4. COMMIT TRANSACTION
 *   5. Background job poll OutboxEvent → Publish to Kafka
 *   → Nếu step 5 fail, sẽ retry. Event không bao giờ mất.
 */
package com.blur.common.outbox;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "outbox_events")  // MongoDB collection name
public class OutboxEvent {
    
    @Id
    private String id;  // Event ID, dùng làm idempotency key
    
    /**
     * Loại entity: "Comment", "Post", "User"
     * Dùng để: Logging, filtering
     */
    private String aggregateType;
    
    /**
     * ID của entity: commentId, postId
     * Dùng để: Kafka partition key (đảm bảo ordering cho cùng entity)
     */
    private String aggregateId;
    
    /**
     * Tên event: "CommentCreatedEvent", "PostLikedEvent"
     */
    private String eventType;
    
    /**
     * Kafka topic: "comment.created", "post.liked"
     */
    private String topic;
    
    /**
     * JSON payload của event
     */
    private String payload;
    
    /**
     * Thời điểm tạo - để FIFO ordering
     */
    @Indexed
    private Instant createdAt;
    
    /**
     * Trạng thái: PENDING → PUBLISHED hoặc FAILED
     */
    @Indexed
    private OutboxStatus status;
    
    /**
     * Số lần đã thử publish (max 3)
     */
    private int retryCount;
    
    /**
     * Lỗi cuối cùng nếu publish fail
     */
    private String errorMessage;
    
    /**
     * Thời điểm publish thành công
     */
    private Instant publishedAt;
    
    /**
     * Factory method tạo OutboxEvent mới
     * 
     * @param topic Kafka topic
     * @param aggregateType Loại entity
     * @param aggregateId ID entity
     * @param eventType Tên event class
     * @param payload JSON string
     */
    public static OutboxEvent create(String topic, String aggregateType, 
                                      String aggregateId, String eventType, 
                                      String payload) {
        return OutboxEvent.builder()
            .id(java.util.UUID.randomUUID().toString())
            .topic(topic)
            .aggregateType(aggregateType)
            .aggregateId(aggregateId)
            .eventType(eventType)
            .payload(payload)
            .createdAt(Instant.now())
            .status(OutboxStatus.PENDING)
            .retryCount(0)
            .build();
    }
}
```

### 3.2.2 OutboxStatus.java - Enum trạng thái

```java
/*
 * FILE: blur-common-lib/outbox/OutboxStatus.java
 * TÁC DỤNG: Enum định nghĩa các trạng thái của OutboxEvent
 */
package com.blur.common.outbox;

public enum OutboxStatus {
    /**
     * Chờ được publish lên Kafka
     * Đây là trạng thái ban đầu khi tạo OutboxEvent
     */
    PENDING,
    
    /**
     * Đã publish thành công lên Kafka
     * Có thể xóa sau X ngày để tiết kiệm storage
     */
    PUBLISHED,
    
    /**
     * Publish thất bại sau N lần retry
     * Cần manual intervention hoặc alert
     */
    FAILED
}
```

### 3.2.3 OutboxRepository.java - Repository truy vấn

```java
/*
 * FILE: blur-common-lib/outbox/OutboxRepository.java
 * TÁC DỤNG: Repository để CRUD OutboxEvent trong MongoDB
 * 
 * CÁC HÀM QUAN TRỌNG:
 */
package com.blur.common.outbox;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OutboxRepository extends MongoRepository<OutboxEvent, String> {
    
    /**
     * Lấy 100 events PENDING cũ nhất (FIFO)
     * 
     * GỌI BỞI: OutboxPublisher.publishPendingEvents()
     * TẦN SUẤT: Mỗi 100ms
     */
    List<OutboxEvent> findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus status);
    
    /**
     * Đếm số event theo status
     * 
     * GỌI BỞI: Monitoring/alerting
     * MỤC ĐÍCH: Alert nếu PENDING quá nhiều (consumer lag)
     */
    long countByStatus(OutboxStatus status);
    
    /**
     * Lấy danh sách FAILED events
     * 
     * GỌI BỞI: Admin API để manual retry
     */
    List<OutboxEvent> findByStatusOrderByCreatedAtDesc(OutboxStatus status);
}
```

### 3.2.4 OutboxPublisher.java - Background Job Publish Events

```java
/*
 * FILE: blur-common-lib/outbox/OutboxPublisher.java
 * TÁC DỤNG: Scheduled job poll OutboxEvent và publish lên Kafka
 * 
 * FLOW:
 *   1. Poll DB mỗi 100ms lấy PENDING events
 *   2. Publish từng event lên Kafka
 *   3. Update status → PUBLISHED hoặc FAILED
 * 
 * ĐẶC ĐIỂM:
 *   - At-least-once delivery (có thể duplicate, consumer phải idempotent)
 *   - Retry 3 lần trước khi đánh dấu FAILED
 *   - FIFO ordering (lấy theo createdAt ASC)
 */
package com.blur.common.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {
    
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    private static final int MAX_RETRIES = 3;
    
    /**
     * Job chạy mỗi 100ms
     * 
     * TẠI SAO 100ms?
     * - Cân bằng giữa latency thấp và DB load
     * - 100ms đủ nhanh cho real-time notifications
     * - Không quá nhanh gây DB overload
     */
    @Scheduled(fixedDelay = 100)
    public void publishPendingEvents() {
        // Lấy batch 100 events (giới hạn để tránh OOM)
        List<OutboxEvent> pendingEvents = outboxRepository
            .findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        
        if (pendingEvents.isEmpty()) {
            return;  // Không có gì để publish
        }
        
        log.debug("OutboxPublisher: Processing {} pending events", pendingEvents.size());
        
        for (OutboxEvent event : pendingEvents) {
            publishEvent(event);
        }
    }
    
    /**
     * Publish 1 event lên Kafka
     * 
     * @param event OutboxEvent cần publish
     */
    private void publishEvent(OutboxEvent event) {
        try {
            // Gửi lên Kafka
            // - topic: event.getTopic() (e.g., "comment.created")
            // - key: event.getAggregateId() (e.g., postId) → partition ordering
            // - value: event.getPayload() (JSON)
            kafkaTemplate.send(
                event.getTopic(),
                event.getAggregateId(),
                event.getPayload()
            ).get();  // .get() = blocking, đợi ACK từ Kafka broker
            
            // Thành công → update status
            event.setStatus(OutboxStatus.PUBLISHED);
            event.setPublishedAt(Instant.now());
            outboxRepository.save(event);
            
            log.info("OutboxPublisher: Published {} to {} (id: {})", 
                event.getEventType(), event.getTopic(), event.getAggregateId());
            
        } catch (Exception e) {
            handlePublishFailure(event, e);
        }
    }
    
    /**
     * Xử lý khi publish thất bại
     * 
     * STRATEGY:
     * - Retry tối đa 3 lần
     * - Sau 3 lần → đánh dấu FAILED để manual review
     */
    private void handlePublishFailure(OutboxEvent event, Exception e) {
        event.setRetryCount(event.getRetryCount() + 1);
        event.setErrorMessage(e.getMessage());
        
        if (event.getRetryCount() >= MAX_RETRIES) {
            event.setStatus(OutboxStatus.FAILED);
            log.error("OutboxPublisher: FAILED after {} retries - {} (topic: {})", 
                MAX_RETRIES, event.getId(), event.getTopic());
            // TODO: Send alert hoặc publish to DLT
        } else {
            log.warn("OutboxPublisher: Retry {}/{} for event {} - {}", 
                event.getRetryCount(), MAX_RETRIES, event.getId(), e.getMessage());
        }
        
        outboxRepository.save(event);
    }
    
    /**
     * Admin API: Retry tất cả FAILED events
     * 
     * @return Số events đã reset về PENDING
     */
    public int retryFailedEvents() {
        List<OutboxEvent> failedEvents = outboxRepository
            .findByStatusOrderByCreatedAtDesc(OutboxStatus.FAILED);
        
        for (OutboxEvent event : failedEvents) {
            event.setStatus(OutboxStatus.PENDING);
            event.setRetryCount(0);
            event.setErrorMessage(null);
            outboxRepository.save(event);
        }
        
        log.info("OutboxPublisher: Reset {} failed events to PENDING", failedEvents.size());
        return failedEvents.size();
    }
    
    /**
     * Monitoring: Lấy số PENDING events
     * 
     * DÙNG CHO: Grafana dashboard, alerting
     * ALERT NẾU: pending > 1000 (backlog quá lớn)
     */
    public long getPendingCount() {
        return outboxRepository.countByStatus(OutboxStatus.PENDING);
    }
    
    /**
     * Monitoring: Lấy số FAILED events
     * 
     * ALERT NẾU: failed > 0 (cần manual intervention)
     */
    public long getFailedCount() {
        return outboxRepository.countByStatus(OutboxStatus.FAILED);
    }
}
```

### 3.2.5 OutboxService.java - Helper Để Lưu Events

```java
/*
 * FILE: blur-common-lib/outbox/OutboxService.java
 * TÁC DỤNG: Service helper để lưu event vào outbox table
 * 
 * CÁCH DÙNG:
 *   @Transactional  // QUAN TRỌNG!
 *   public void createComment(...) {
 *       commentRepository.save(comment);
 *       outboxService.saveEvent("comment.created", "Comment", comment.getId(), event);
 *   }
 *   // Cả 2 save trong cùng 1 transaction → atomic
 */
package com.blur.common.outbox;

import com.blur.common.event.BaseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {
    
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;  // Jackson JSON serializer
    
    /**
     * Lưu event vào outbox table
     * 
     * ⚠️ PHẢI GỌI TRONG @Transactional METHOD
     * để đảm bảo atomic với business data
     *
     * @param topic       Kafka topic (e.g., "comment.created")
     * @param aggregateType Loại entity (e.g., "Comment")
     * @param aggregateId   ID entity (e.g., commentId)
     * @param event       Domain event object
     * @return OutboxEvent đã lưu
     */
    public <T extends BaseEvent> OutboxEvent saveEvent(
            String topic, 
            String aggregateType, 
            String aggregateId, 
            T event) {
        try {
            // Khởi tạo eventId, timestamp nếu chưa có
            event.initDefaults();
            
            // Serialize event thành JSON
            String payload = objectMapper.writeValueAsString(event);
            
            // Tạo OutboxEvent
            OutboxEvent outboxEvent = OutboxEvent.create(
                topic,
                aggregateType,
                aggregateId,
                event.getEventType(),
                payload
            );
            
            // Lưu vào MongoDB (cùng transaction với business data)
            outboxEvent = outboxRepository.save(outboxEvent);
            
            log.debug("OutboxService: Saved {} for {}:{}", 
                event.getEventType(), aggregateType, aggregateId);
            
            return outboxEvent;
            
        } catch (JsonProcessingException e) {
            log.error("OutboxService: Failed to serialize {}: {}", 
                event.getClass().getSimpleName(), e.getMessage());
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
```

---

## 3.3 Kafka Producer - Publish Events

### 3.3.1 CommentService SAU KHI REFACTOR

```java
/*
 * FILE: post-service/service/CommentService.java (AFTER REFACTOR)
 * 
 * THAY ĐỔI:
 *   - TRƯỚC: Gọi REST tới ProfileClient, IdentityClient, NotificationClient
 *   - SAU: Chỉ save data + OutboxEvent, publish async qua Kafka
 * 
 * LỢI ÍCH:
 *   - Latency: 50ms → 10ms (chỉ DB write)
 *   - Fault tolerance: Notification service down không ảnh hưởng
 *   - Scalability: Notification xử lý async, có thể scale riêng
 */
package com.postservice.service;

import com.blur.common.event.CommentCreatedEvent;
import com.blur.common.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CommentService {
    
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final OutboxService outboxService;  // NEW: Thay thế NotificationClient
    private final CommentMapper commentMapper;

    /**
     * Tạo comment mới
     * 
     * FLOW MỚI:
     *   1. Lưu comment vào DB
     *   2. Lưu event vào outbox (cùng transaction)
     *   3. Return ngay cho user
     *   4. Background: OutboxPublisher → Kafka → NotificationService
     */
    @Transactional  // QUAN TRỌNG: Đảm bảo atomic
    public CommentResponse createComment(CreateCommentRequest request, String postId) {
        // Lấy user từ JWT token
        String userId = SecurityContextHolder.getContext()
            .getAuthentication().getName();

        // Lấy post để kiểm tra tồn tại và lấy owner
        var post = postRepository.findById(postId)
            .orElseThrow(() -> new BlurException(ErrorCode.POST_NOT_FOUND));

        // Tạo comment
        Comment comment = Comment.builder()
            .content(request.getContent())
            .userId(userId)
            .postId(postId)
            .createdAt(Instant.now())
            .build();
        comment = commentRepository.save(comment);

        // Nếu tự comment bài mình → không cần notification
        if (!post.getUserId().equals(userId)) {
            // Tạo event với thông tin có sẵn
            // LƯU Ý: Không gọi REST lấy profile, để NotificationService tự lấy
            CommentCreatedEvent event = CommentCreatedEvent.builder()
                .commentId(comment.getId())
                .postId(postId)
                .content(comment.getContent())
                .authorId(userId)
                .postOwnerId(post.getUserId())
                .aggregateId(postId)  // Partition key = postId
                .aggregateType("Comment")
                .build();
            
            // Lưu vào outbox (cùng transaction với comment)
            outboxService.saveEvent(
                "comment.created",  // Kafka topic
                "Comment",          // Aggregate type
                comment.getId(),    // Aggregate ID
                event
            );
            
            log.info("Comment created and event queued: {}", comment.getId());
        }

        return commentMapper.toCommentResponse(comment);
    }
}
```

---

## 3.4 Kafka Consumer - Nhận và Xử Lý Events

### 3.4.1 NotificationEventConsumer.java

```java
/*
 * FILE: notification-service/kafka/NotificationEventConsumer.java
 * TÁC DỤNG: Nhận events từ Kafka và tạo notifications
 * 
 * ĐẶC ĐIỂM:
 *   - Idempotent: Check eventId trước khi xử lý
 *   - Manual commit: Chỉ commit sau khi xử lý thành công
 *   - Error handling: Retry với DLT (Dead Letter Topic)
 */
package com.blur.notificationservice.kafka;

import com.blur.common.event.CommentCreatedEvent;
import com.blur.common.event.PostLikedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {
    
    private final NotificationService notificationService;
    private final IdempotencyService idempotencyService;  // Check duplicate
    private final ProfileClient profileClient;  // Lấy thêm info nếu cần
    private final ObjectMapper objectMapper;

    /**
     * Consumer cho topic "comment.created"
     * 
     * ANNOTATIONS:
     *   @KafkaListener: Đăng ký consumer với Kafka
     *   - topics: Topic(s) để subscribe
     *   - groupId: Consumer group (nhiều instances share load)
     *   - containerFactory: Custom factory với error handling
     */
    @KafkaListener(
        topics = "comment.created",
        groupId = "notification-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleCommentCreated(String message, Acknowledgment ack) {
        try {
            // Deserialize JSON → Event object
            CommentCreatedEvent event = objectMapper.readValue(
                message, CommentCreatedEvent.class);
            
            log.info("Received comment.created: commentId={}, postId={}", 
                event.getCommentId(), event.getPostId());
            
            // === IDEMPOTENCY CHECK ===
            // Tránh xử lý trùng lặp khi Kafka retry
            if (idempotencyService.isProcessed(event.getEventId())) {
                log.info("Event already processed, skipping: {}", event.getEventId());
                ack.acknowledge();  // Commit offset, đừng reprocess
                return;
            }
            
            // === LẤY THÊM THÔNG TIN NẾU CẦN ===
            // Event chỉ chứa IDs, lấy thêm profile để hiển thị
            var authorProfile = profileClient.getProfile(event.getAuthorId());
            
            // === TẠO NOTIFICATION ===
            Notification notification = Notification.builder()
                .postId(event.getPostId())
                .senderId(event.getAuthorId())
                .senderName(authorProfile.getFirstName() + " " + authorProfile.getLastName())
                .senderImageUrl(authorProfile.getImageUrl())
                .receiverId(event.getPostOwnerId())
                .type(NotificationType.COMMENT)
                .content(event.getAuthorName() + " đã bình luận về bài viết của bạn")
                .read(false)
                .timestamp(event.getTimestamp())
                .build();
            
            // Lưu vào DB
            notificationService.save(notification);
            
            // Push realtime qua WebSocket
            notificationService.pushToUser(event.getPostOwnerId(), notification);
            
            // === ĐÁNH DẤU ĐÃ XỬ LÝ ===
            idempotencyService.markProcessed(event.getEventId());
            
            // === COMMIT OFFSET ===
            // Chỉ commit SAU KHI xử lý thành công
            ack.acknowledge();
            
            log.info("Successfully processed comment.created: {}", event.getEventId());
            
        } catch (Exception e) {
            log.error("Error processing comment.created: {}", e.getMessage(), e);
            // KHÔNG acknowledge → Kafka sẽ retry
            // Sau N retries → gửi tới DLT (Dead Letter Topic)
            throw new RuntimeException(e);
        }
    }

    /**
     * Consumer cho topic "post.liked"
     */
    @KafkaListener(
        topics = "post.liked",
        groupId = "notification-service",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePostLiked(String message, Acknowledgment ack) {
        try {
            PostLikedEvent event = objectMapper.readValue(message, PostLikedEvent.class);
            
            // Idempotency check
            if (idempotencyService.isProcessed(event.getEventId())) {
                ack.acknowledge();
                return;
            }
            
            // Create notification
            var likerProfile = profileClient.getProfile(event.getLikerId());
            
            Notification notification = Notification.builder()
                .postId(event.getPostId())
                .senderId(event.getLikerId())
                .senderName(likerProfile.getFirstName() + " " + likerProfile.getLastName())
                .senderImageUrl(likerProfile.getImageUrl())
                .receiverId(event.getPostOwnerId())
                .type(NotificationType.LIKE)
                .content(likerProfile.getFirstName() + " đã thích bài viết của bạn")
                .read(false)
                .build();
            
            notificationService.save(notification);
            notificationService.pushToUser(event.getPostOwnerId(), notification);
            
            idempotencyService.markProcessed(event.getEventId());
            ack.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing post.liked: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
```

### 3.4.2 IdempotencyService.java - Tránh Xử Lý Trùng Lặp

```java
/*
 * FILE: notification-service/service/IdempotencyService.java
 * TÁC DỤNG: Check và đánh dấu event đã xử lý
 * 
 * TẠI SAO CẦN?
 * Kafka đảm bảo at-least-once, nghĩa là có thể gửi trùng.
 * Consumer phải tự check để tránh tạo duplicate notifications.
 * 
 * CÁCH HOẠT ĐỘNG:
 * - Lưu eventId vào Redis với TTL 24h
 * - Check eventId trước khi xử lý
 */
package com.blur.notificationservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class IdempotencyService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String KEY_PREFIX = "processed_event:";
    private static final Duration TTL = Duration.ofHours(24);
    
    /**
     * Check event đã được xử lý chưa
     * 
     * @param eventId Event ID cần check
     * @return true nếu đã xử lý, false nếu chưa
     */
    public boolean isProcessed(String eventId) {
        String key = KEY_PREFIX + eventId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    /**
     * Đánh dấu event đã xử lý
     * 
     * @param eventId Event ID cần đánh dấu
     */
    public void markProcessed(String eventId) {
        String key = KEY_PREFIX + eventId;
        redisTemplate.opsForValue().set(key, "1", TTL);
    }
}
```

---

## 3.5 Kafka Configuration

### 3.5.1 KafkaProducerConfig.java

```java
/*
 * FILE: blur-common-lib/configuration/KafkaProducerConfig.java
 * TÁC DỤNG: Cấu hình Kafka Producer
 * 
 * CÁC SETTINGS QUAN TRỌNG:
 *   - acks=all: Đợi tất cả replicas confirm
 *   - retries=3: Tự động retry khi fail
 *   - enable.idempotence=true: Tránh duplicate ở broker
 */
package com.blur.common.configuration;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.*;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {
    
    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;
    
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        
        // Kafka broker address
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        
        // Serializers
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        
        // === RELIABILITY SETTINGS ===
        
        // Đợi tất cả replicas acknowledge
        // "all" = highest durability, "1" = leader only, "0" = fire and forget
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        
        // Retry khi fail (network issues, etc.)
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        
        // Enable idempotence - tránh duplicate messages khi retry
        // Kafka sẽ dedup dựa trên producer ID + sequence number
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        // === PERFORMANCE SETTINGS ===
        
        // Batch size (bytes) - gom nhiều messages gửi 1 lần
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384); // 16KB
        
        // Đợi tối đa 5ms để gom batch (trade-off latency vs throughput)
        config.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        
        return new DefaultKafkaProducerFactory<>(config);
    }
    
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

### 3.5.2 KafkaConsumerConfig.java

```java
/*
 * FILE: notification-service/configuration/KafkaConsumerConfig.java
 * TÁC DỤNG: Cấu hình Kafka Consumer với retry và DLT
 */
package com.blur.notificationservice.configuration;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {
    
    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;
    
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        
        // === RELIABILITY SETTINGS ===
        
        // Không auto commit offset - manual commit sau khi xử lý
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        
        // Khi consumer mới join, đọc từ đầu (không mất events)
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        // === PERFORMANCE SETTINGS ===
        
        // Max messages lấy mỗi poll
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        
        return new DefaultKafkaConsumerFactory<>(config);
    }
    
    /**
     * Factory với error handling và DLT
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> 
            kafkaListenerContainerFactory(KafkaTemplate<String, String> kafkaTemplate) {
        
        ConcurrentKafkaListenerContainerFactory<String, String> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(consumerFactory());
        
        // Manual commit mode
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        
        // === ERROR HANDLING ===
        // Retry 3 lần với exponential backoff: 1s, 2s, 4s
        // Sau đó gửi tới Dead Letter Topic
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(1000L);  // 1 second
        backOff.setMultiplier(2.0);         // x2 mỗi lần
        backOff.setMaxInterval(10000L);     // Max 10 seconds
        
        factory.setCommonErrorHandler(new DefaultErrorHandler(
            new DeadLetterPublishingRecoverer(kafkaTemplate),  // Send to DLT
            backOff
        ));
        
        return factory;
    }
}
```

---

# 📊 PHẦN 4: CHIẾN LƯỢC MIGRATION

## 4.1 Roadmap 4 Giai Đoạn

| Giai đoạn | Timeline | Mô tả | Risk |
|-----------|----------|-------|------|
| 1. Song song | Tuần 1-2 | REST + Kafka, compare data | Low |
| 2. Hybrid | Tuần 3-4 | Kafka primary, REST fallback | Medium |
| 3. Event-first | Tuần 5-6 | Kafka only, REST deprecated | Medium |
| 4. Cleanup | Tuần 7-8 | Remove REST clients | Low |

## 4.2 Service Migration Priority

| Priority | Service | Reason |
|----------|---------|--------|
| 🔴 1 | post-service → notification | Nhiều REST calls nhất |
| 🔴 2 | profile-service → notification | Follow notifications |
| 🟡 3 | story-service → notification | Similar pattern |
| 🟡 4 | chat-service → ai-service | AI moderation async |

---

# 📊 PHẦN 5: SO SÁNH TRƯỚC & SAU

| Aspect | REST (Hiện tại) | Event-Driven (Sau) |
|--------|-----------------|-------------------|
| **Latency** | 50-200ms (đợi REST) | 5-10ms (chỉ DB write) |
| **Coupling** | Tight (gọi trực tiếp) | Loose (qua events) |
| **Fault Tolerance** | Low (cascade fail) | High (isolated) |
| **Scalability** | Limited | High (consumers độc lập) |
| **Debugging** | Easy (sync trace) | Harder (need event trace) |
| **Consistency** | Strong | Eventual |

---

# 📊 PHẦN 6: CHECKLIST TRIỂN KHAI

## Phase 1: Foundation
- [ ] Tạo domain events trong blur-common-lib
- [ ] Implement Outbox Pattern
- [ ] Cấu hình Kafka Producer/Consumer
- [ ] Setup monitoring (pending/failed count)

## Phase 2: Migration
- [ ] PostService.likePost() → Kafka
- [ ] CommentService.createComment() → Kafka
- [ ] ProfileService.followUser() → Kafka
- [ ] StoryService.likeStory() → Kafka

## Phase 3: Verification
- [ ] Unit tests cho producers
- [ ] Unit tests cho consumers
- [ ] Integration tests end-to-end
- [ ] Compare REST vs Kafka data

## Phase 4: Cleanup
- [ ] Remove NotificationClient
- [ ] Deprecate REST endpoints
- [ ] Update documentation
