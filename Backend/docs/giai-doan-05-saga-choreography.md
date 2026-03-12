# Giai Đoạn 5: Saga Choreography - Delete User (CHƯA TRIỂN KHAI)

## Mục tiêu

Khi xóa user, cần xóa data liên quan ở TẤT CẢ services:
- **user-service:** UserProfile node + follows relationships
- **content-service:** Posts, Comments, CommentReplies, Stories, PostFeedItems + tất cả relationships
- **communication-service:** Conversations (participant), ChatMessages, Notifications, AI conversations

Hiện tại chưa có cơ chế distributed delete. Saga Choreography dùng Kafka events để các service tự xử lý phần của mình.

## Trạng thái: CHƯA TRIỂN KHAI

## Vấn đề hiện tại

```
UserController.deleteUser()
  └─ userProfileRepository.deleteById(userId)   ← Chỉ xóa user profile
                                                    Posts, comments, conversations vẫn còn!
```

## Giải pháp: Saga Choreography qua Kafka

```
User Service                Content Service             Communication Service
────────────               ─────────────────           ──────────────────────
1. Mark user DELETING
2. Publish "user-delete-saga"
   {userId, step: INITIATED}
        │
        ├──────────────────→ 3. Delete posts
        │                       Delete comments
        │                       Delete stories
        │                       Delete feed items
        │                    4. Publish
        │                       {userId, step: CONTENT_CLEANED}
        │                           │
        │                           ├──────────────────→ 5. Remove from conversations
        │                           │                       Delete notifications
        │                           │                       Delete AI conversations
        │                           │                    6. Publish
        │                           │                       {userId, step: COMMUNICATION_CLEANED}
        │←──────────────────────────┘
7. Delete user profile
8. Publish {userId, step: COMPLETED}
```

## Bước 1: Tạo Saga Event Schema

### Kafka Topic: `user-delete-saga`

```json
{
    "sagaId": "uuid-saga-123",
    "userId": "user-uuid-456",
    "step": "INITIATED",
    "timestamp": "2026-03-12T10:00:00",
    "error": null
}
```

**Steps:**
- `INITIATED` → User service bắt đầu saga
- `CONTENT_CLEANED` → Content service đã xóa xong
- `COMMUNICATION_CLEANED` → Communication service đã xóa xong
- `COMPLETED` → User service xóa profile, saga hoàn thành
- `FAILED` → Một service lỗi, cần compensate

## Bước 2: User Service - Saga Initiator

### SagaEvent DTO

**File:** `user-service/src/main/java/com/blur/userservice/profile/dto/event/SagaEvent.java`

```java
package com.blur.userservice.profile.dto.event;

import lombok.*;
import java.time.Instant;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SagaEvent {
    String sagaId;
    String userId;
    String step;        // INITIATED, CONTENT_CLEANED, COMMUNICATION_CLEANED, COMPLETED, FAILED
    Instant timestamp;
    String error;
}
```

### UserDeleteSagaService

**File:** `user-service/src/main/java/com/blur/userservice/profile/service/UserDeleteSagaService.java`

```java
package com.blur.userservice.profile.service;

import com.blur.userservice.profile.dto.event.SagaEvent;
import com.blur.userservice.profile.repository.UserProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserDeleteSagaService {
    KafkaTemplate<String, String> kafkaTemplate;
    ObjectMapper objectMapper;
    UserProfileRepository userProfileRepository;

    /**
     * Bắt đầu saga xóa user
     */
    public void initiateDeleteUser(String userId) {
        try {
            SagaEvent event = SagaEvent.builder()
                    .sagaId(UUID.randomUUID().toString())
                    .userId(userId)
                    .step("INITIATED")
                    .timestamp(Instant.now())
                    .build();
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("user-delete-saga", userId, json);
            log.info("Delete user saga initiated for user {}", userId);
        } catch (Exception e) {
            log.error("Failed to initiate delete saga for user {}", userId, e);
        }
    }

    /**
     * Lắng nghe saga events để hoàn thành bước cuối
     */
    @KafkaListener(topics = "user-delete-saga", groupId = "user-service-saga")
    public void handleSagaEvent(String message) {
        try {
            SagaEvent event = objectMapper.readValue(message, SagaEvent.class);

            if ("COMMUNICATION_CLEANED".equals(event.getStep())) {
                // Bước cuối: xóa user profile
                userProfileRepository.deleteByUserId(event.getUserId());
                log.info("User profile deleted for user {}", event.getUserId());

                // Publish COMPLETED
                event.setStep("COMPLETED");
                event.setTimestamp(Instant.now());
                String json = objectMapper.writeValueAsString(event);
                kafkaTemplate.send("user-delete-saga", event.getUserId(), json);
                log.info("Delete user saga COMPLETED for user {}", event.getUserId());
            }

            if ("FAILED".equals(event.getStep())) {
                log.error("Delete user saga FAILED for user {}: {}", event.getUserId(), event.getError());
                // TODO: implement compensation logic
            }
        } catch (Exception e) {
            log.error("Failed to handle saga event", e);
        }
    }
}
```

## Bước 3: Content Service - Saga Participant

**File:** `content-service/src/main/java/com/contentservice/kafka/UserDeleteSagaConsumer.java`

```java
package com.contentservice.kafka;

import com.contentservice.post.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserDeleteSagaConsumer {
    PostRepository postRepository;
    CommentRepository commentRepository;
    CommentReplyRepository commentReplyRepository;
    StoryRepository storyRepository;
    PostFeedRepository postFeedRepository;
    KafkaTemplate<String, String> kafkaTemplate;
    ObjectMapper objectMapper;

    @KafkaListener(topics = "user-delete-saga", groupId = "content-service-saga")
    @Transactional
    public void handleSagaEvent(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            String step = (String) event.get("step");
            String userId = (String) event.get("userId");

            if (!"INITIATED".equals(step)) return;

            log.info("Content cleanup started for user {}", userId);

            // Xóa tất cả content của user
            // Neo4j: xóa nodes và relationships liên quan
            postRepository.deleteAllByUserId(userId);
            commentRepository.deleteAllByUserId(userId);
            commentReplyRepository.deleteAllByUserId(userId);
            storyRepository.deleteAllByAuthorId(userId);
            postFeedRepository.deleteAllByAuthorId(userId);

            log.info("Content cleanup completed for user {}", userId);

            // Publish CONTENT_CLEANED
            event.put("step", "CONTENT_CLEANED");
            event.put("timestamp", Instant.now().toString());
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("user-delete-saga", userId, json);
        } catch (Exception e) {
            log.error("Content cleanup failed", e);
            publishFailure(message, e.getMessage());
        }
    }

    private void publishFailure(String originalMessage, String error) {
        try {
            Map<String, Object> event = objectMapper.readValue(originalMessage, Map.class);
            event.put("step", "FAILED");
            event.put("error", "Content cleanup failed: " + error);
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("user-delete-saga", (String) event.get("userId"), json);
        } catch (Exception e) {
            log.error("Failed to publish failure event", e);
        }
    }
}
```

## Bước 4: Communication Service - Saga Participant

**File:** `communication-service/src/main/java/com/blur/communicationservice/kafka/UserDeleteSagaConsumer.java`

```java
package com.blur.communicationservice.kafka;

import com.blur.communicationservice.chat.repository.ChatMessageRepository;
import com.blur.communicationservice.chat.repository.ConversationRepository;
import com.blur.communicationservice.notification.repository.NotificationRepository;
import com.blur.communicationservice.ai.repository.AiConversationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserDeleteSagaConsumer {
    ChatMessageRepository chatMessageRepository;
    ConversationRepository conversationRepository;
    NotificationRepository notificationRepository;
    AiConversationRepository aiConversationRepository;
    KafkaTemplate<String, String> kafkaTemplate;
    ObjectMapper objectMapper;

    @KafkaListener(topics = "user-delete-saga", groupId = "communication-service-saga")
    public void handleSagaEvent(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            String step = (String) event.get("step");
            String userId = (String) event.get("userId");

            if (!"CONTENT_CLEANED".equals(step)) return;

            log.info("Communication cleanup started for user {}", userId);

            // Xóa messages, notifications, AI conversations
            chatMessageRepository.deleteBySenderUserId(userId);
            notificationRepository.deleteByReceiverId(userId);
            aiConversationRepository.deleteByUserId(userId);
            // Conversations: remove participant hoặc xóa nếu chỉ còn 1 participant

            log.info("Communication cleanup completed for user {}", userId);

            // Publish COMMUNICATION_CLEANED
            event.put("step", "COMMUNICATION_CLEANED");
            event.put("timestamp", Instant.now().toString());
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("user-delete-saga", userId, json);
        } catch (Exception e) {
            log.error("Communication cleanup failed", e);
            publishFailure(message, e.getMessage());
        }
    }

    private void publishFailure(String originalMessage, String error) {
        try {
            Map<String, Object> event = objectMapper.readValue(originalMessage, Map.class);
            event.put("step", "FAILED");
            event.put("error", "Communication cleanup failed: " + error);
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("user-delete-saga", (String) event.get("userId"), json);
        } catch (Exception e) {
            log.error("Failed to publish failure event", e);
        }
    }
}
```

## Bước 5: Sửa UserController.deleteUser()

**File:** `user-service/src/main/java/com/blur/userservice/profile/controller/UserController.java`

```java
@DeleteMapping("/{userId}")
public ApiResponse<Void> deleteUser(@PathVariable String userId) {
    // Thay vì xóa trực tiếp:
    // userService.deleteUser(userId);

    // Dùng saga:
    userDeleteSagaService.initiateDeleteUser(userId);

    return ApiResponse.<Void>builder()
            .code(1000)
            .message("User deletion initiated. Processing asynchronously.")
            .build();
}
```

## Sequence Diagram

```
User        User Service        Kafka              Content Service      Communication Service
 │               │                │                      │                       │
 │  DELETE user   │                │                      │                       │
 │──────────────>│                │                      │                       │
 │               │  INITIATED     │                      │                       │
 │               │───────────────>│                      │                       │
 │  202 Accepted │                │  INITIATED           │                       │
 │<──────────────│                │─────────────────────>│                       │
 │               │                │                      │  Delete posts,        │
 │               │                │                      │  comments, stories    │
 │               │                │  CONTENT_CLEANED     │                       │
 │               │                │<─────────────────────│                       │
 │               │                │  CONTENT_CLEANED     │                       │
 │               │                │────────────────────────────────────────────>│
 │               │                │                      │                       │ Delete messages,
 │               │                │                      │                       │ notifications
 │               │                │  COMMUNICATION_CLEANED                       │
 │               │                │<────────────────────────────────────────────│
 │               │ COMM_CLEANED   │                      │                       │
 │               │<───────────────│                      │                       │
 │               │ Delete profile │                      │                       │
 │               │  COMPLETED     │                      │                       │
 │               │───────────────>│                      │                       │
```

## Neo4j Delete Queries cần thêm

### PostRepository

```java
@Query("MATCH (p:post {userId: $userId}) DETACH DELETE p")
void deleteAllByUserId(@Param("userId") String userId);
```

### CommentRepository

```java
@Query("MATCH (c:comment {userId: $userId}) DETACH DELETE c")
void deleteAllByUserId(@Param("userId") String userId);
```

### CommentReplyRepository

```java
@Query("MATCH (cr:comment_reply {userId: $userId}) DETACH DELETE cr")
void deleteAllByUserId(@Param("userId") String userId);
```

### StoryRepository

```java
@Query("MATCH (s:story {authorId: $userId}) DETACH DELETE s")
void deleteAllByAuthorId(@Param("userId") String userId);
```

### UserProfileRepository

```java
@Query("MATCH (u:user_profile {user_id: $userId}) DETACH DELETE u")
void deleteByUserId(@Param("userId") String userId);
```

## Checklist

- [ ] Tạo SagaEvent DTO
- [ ] Tạo UserDeleteSagaService trong user-service (initiator + listener)
- [ ] Tạo UserDeleteSagaConsumer trong content-service (xóa posts, comments, stories, feed items)
- [ ] Tạo UserDeleteSagaConsumer trong communication-service (xóa messages, notifications, AI conversations)
- [ ] Thêm Neo4j delete queries: deleteAllByUserId cho Post, Comment, CommentReply
- [ ] Thêm deleteByUserId cho UserProfileRepository
- [ ] Sửa UserController.deleteUser() → gọi saga thay vì xóa trực tiếp
- [ ] Test: xóa user → tất cả data ở 3 services đều bị xóa
- [ ] Test: content-service down → saga dừng ở INITIATED → content-service lên → retry thành công
- [ ] Test: FAILED event → log error và giữ data để manual cleanup
