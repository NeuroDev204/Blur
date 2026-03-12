# Giai Đoạn 6: CQRS + Feed Read Model (MỘT PHẦN ĐÃ TRIỂN KHAI)

## Mục tiêu

Tách read/write cho Post Feed bằng CQRS pattern.

- **Write:** Neo4j `post` nodes (như hiện tại)
- **Read:** Neo4j `post_feed` nodes (denormalized, tối ưu cho query feed)
- **Sync:** Kafka events từ write side → FeedProjectionService → update read model

## Trạng thái: MỘT PHẦN ĐÃ TRIỂN KHAI

### Đã có:
- PostFeedItem entity (Neo4j Node)
- PostFeedRepository (Neo4jRepository)
- FeedProjectionService (Kafka consumer cho "post-events")
- FeedService (query side với caching)
- PostController endpoint `GET /posts/my-feed`

### Chưa có (cần thêm):
- PostService.createPost() chưa publish POST_CREATED event lên Kafka "post-events"
- Chưa có internal endpoint lấy follower IDs ở user-service
- Chưa có getFollowerIds trong ProfileClient (Feign)
- FeedProjectionService chưa được kích hoạt vì không có event nào publish vào "post-events"

## Kiến trúc tổng quan

```
Write Side (Command)                 Read Side (Query)
────────────────────                ─────────────────
PostService.createPost()             FeedService.getMyFeed()
  → save Post (Neo4j)                 → query PostFeedItem (Neo4j)
  → linkPostToAuthor()                  (denormalized, pre-joined)
  → publish Kafka event                 (cached in Redis)
         │
    Kafka: "post-events"
         │
         ▼
FeedProjectionService
  → consume event
  → tạo PostFeedItem cho mỗi follower
  → save PostFeedItem (Neo4j)
```

## Code hiện tại

### PostFeedItem Entity (Read Model)

**File:** `content-service/src/main/java/com/contentservice/post/entity/PostFeedItem.java`

```java
package com.contentservice.post.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Node("post_feed")
public class PostFeedItem {
    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    String id;

    // post data
    String postId;          // tro ve post goc
    String content;
    List<String> imageUrls;
    String videoUrl;

    // author data
    String authorId;
    String authorUsername;
    String authorFirstName;
    String authorLastName;
    String authorAvatar;

    // engagement counts
    int likeCount;
    int commentCount;
    int shareCount;

    // feed targeting
    String targetUserId;
    LocalDateTime createdDate;
    LocalDateTime updatedDate;
}
```

### PostFeedRepository

**File:** `content-service/src/main/java/com/contentservice/post/repository/PostFeedRepository.java`

```java
package com.contentservice.post.repository;

import com.contentservice.post.entity.PostFeedItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostFeedRepository extends Neo4jRepository<PostFeedItem, String> {
    Page<PostFeedItem> findByTargetUserIdOrderByCreatedDateDesc(String targetUserId, Pageable pageable);
    void deleteAllByPostId(String postId);
    void deleteAllByAuthorId(String authorId);
}
```

### FeedProjectionService (Event Consumer)

**File:** `content-service/src/main/java/com/contentservice/kafka/FeedProjectionService.java`

```java
package com.contentservice.kafka;

import com.contentservice.post.entity.PostFeedItem;
import com.contentservice.post.repository.PostFeedRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FeedProjectionService {
    PostFeedRepository postFeedRepository;
    ObjectMapper objectMapper;

    @KafkaListener(topics = "post-events", groupId = "content-feed-projection")
    public void consume(String json) {
        try {
            Map<String, Object> event = objectMapper.readValue(json, Map.class);
            String eventType = (String) event.get("eventType");
            switch (eventType) {
                case "POST_CREATED" -> handlePostCreated(event);
                case "POST_DELETED" -> handlePostDeleted(event);
                case "POST_LIKED" -> handleEngagementUpdate(event, "likeCount");
                case "POST_COMMENTED" -> handleEngagementUpdate(event, "commentCount");
                default -> log.debug("Ignoring event: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Failed to process post event for feed projection", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void handlePostCreated(Map<String, Object> event) {
        String postId = (String) event.get("postId");
        String authorId = (String) event.get("authorId");
        String content = (String) event.get("content");
        List<String> followerIds = (List<String>) event.get("followerIds");
        if (followerIds == null || followerIds.isEmpty()) {
            return;
        }
        for (String followerId : followerIds) {
            PostFeedItem item = PostFeedItem.builder()
                    .postId(postId)
                    .content(content)
                    .authorId(authorId)
                    .authorUsername((String) event.get("authorUsername"))
                    .authorFirstName((String) event.get("authorFirstName"))
                    .authorLastName((String) event.get("authorLastName"))
                    .authorAvatar((String) event.get("authorAvatar"))
                    .targetUserId(followerId)
                    .likeCount(0)
                    .commentCount(0)
                    .shareCount(0)
                    .build();
            postFeedRepository.save(item);
        }
    }

    private void handlePostDeleted(Map<String, Object> event) {
        String postId = (String) event.get("postId");
        postFeedRepository.deleteAllByPostId(postId);
    }

    private void handleEngagementUpdate(Map<String, Object> event, String field) {
        // TODO: implement engagement count update
    }
}
```

### FeedService (Query Side)

**File:** `content-service/src/main/java/com/contentservice/post/service/FeedService.java`

```java
package com.contentservice.post.service;

import com.contentservice.post.entity.PostFeedItem;
import com.contentservice.post.repository.PostFeedRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PACKAGE, makeFinal = true)
public class FeedService {
    PostFeedRepository postFeedRepository;

    @Cacheable(value = "feed", key = "#root.target.getCurrentUserId() + ':'+#page +':' + #size",
               unless = "#result.isEmpty()")
    public Page<PostFeedItem> getMyFeed(int page, int size) {
        String userId = getCurrentUserId();
        return postFeedRepository
                .findByTargetUserIdOrderByCreatedDateDesc(userId, PageRequest.of(page, size));
    }

    public String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
```

### PostController - Feed Endpoint (đã có)

**File:** `content-service/src/main/java/com/contentservice/post/controller/PostController.java`

```java
@GetMapping("/my-feed")
public ApiResponse<Map<String, Object>> getMyFeed(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "5") int limit) {
    Page<PostFeedItem> feedItems = feedService.getMyFeed(page, limit);
    Map<String, Object> result = new HashMap<>();
    result.put("feeds", feedItems.getContent());
    result.put("currentPage", feedItems.getNumber() + 1);
    result.put("totalPages", feedItems.getTotalPages());
    result.put("hasNextPage", feedItems.hasNext());
    return ApiResponse.<Map<String, Object>>builder()
            .result(result).code(1000).message("OK").build();
}
```

### PostService.createPost() hiện tại (CHƯA CÓ Kafka publish)

**File:** `content-service/src/main/java/com/contentservice/post/service/PostService.java`

```java
@Transactional
public PostResponse createPost(PostRequest postRequest) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    var userId = authentication.getName();
    var profileResult = profileClient.getProfile(userId).getResult();

    Post post = Post.builder()
            .content(postRequest.getContent())
            .mediaUrls(postRequest.getMediaUrls())
            .userId(userId)
            .profileId(profileResult != null ? profileResult.getId() : null)
            .firstName(profileResult != null ? profileResult.getFirstName() : null)
            .lastName(profileResult != null ? profileResult.getLastName() : null)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    post = postRepository.save(post);

    // Graph: (user_profile)-[:POSTED {profileId, createdAt}]->(Post)
    postRepository.linkPostToAuthor(userId, post.getId(), post.getProfileId(), post.getCreatedAt());

    return postMapper.toPostResponse(post);
    // ⚠️ THIẾU: Không publish POST_CREATED event lên Kafka "post-events"
    // → FeedProjectionService không nhận được event
    // → PostFeedItem không được tạo cho followers
    // → /my-feed luôn trả về rỗng
}
```

---

## CẦN THAY ĐỔI GÌ ĐỂ HOÀN THÀNH GIAI ĐOẠN 6

### Thay đổi 1: Thêm endpoint lấy follower IDs trong user-service

**Tại sao cần:** Khi tạo post, content-service cần biết ai đang follow user này để tạo PostFeedItem cho mỗi follower. Phải gọi user-service vì data follows nằm ở Neo4j của user-service.

**File cần sửa:** `user-service/src/main/java/com/blur/userservice/profile/repository/UserProfileRepository.java`

**Thêm query:**

```java
@Query("""
    MATCH (follower:user_profile)-[:follows]->(u:user_profile {user_id: $userId})
    RETURN follower.user_id
""")
List<String> findFollowerUserIdsByUserId(@Param("userId") String userId);
```

**File cần sửa:** `user-service/src/main/java/com/blur/userservice/profile/service/UserProfileService.java`

**Thêm method:**

```java
public List<String> getFollowerUserIds(String userId) {
    return userProfileRepository.findFollowerUserIdsByUserId(userId);
}
```

**File cần sửa:** `user-service/src/main/java/com/blur/userservice/profile/controller/InternalUserProfileController.java`

**Thêm endpoint:**

```java
@GetMapping("/internal/users/{userId}/follower-ids")
public ApiResponse<List<String>> getFollowerIds(@PathVariable String userId) {
    List<String> followerIds = userProfileService.getFollowerUserIds(userId);
    return ApiResponse.<List<String>>builder()
            .code(1000)
            .result(followerIds)
            .build();
}
```

---

### Thay đổi 2: Thêm getFollowerIds vào ProfileClient (Feign) trong content-service

**Tại sao cần:** Content-service cần gọi endpoint mới ở user-service để lấy follower IDs. ProfileClient là Feign client dùng để giao tiếp với user-service.

**File cần sửa:** `content-service/src/main/java/com/contentservice/post/repository/httpclient/ProfileClient.java`

**Thêm method:**

```java
@GetMapping("/internal/users/{userId}/follower-ids")
ApiResponse<List<String>> getFollowerIds(@PathVariable("userId") String userId);
```

---

### Thay đổi 3: Sửa PostService.createPost() để publish POST_CREATED event

**Tại sao cần:** Đây là phần QUAN TRỌNG NHẤT. Hiện tại `createPost()` chỉ save post và link author, KHÔNG publish event lên Kafka. FeedProjectionService đang lắng nghe topic "post-events" nhưng không bao giờ nhận được event → feed luôn rỗng.

**File cần sửa:** `content-service/src/main/java/com/contentservice/post/service/PostService.java`

**Cần thêm inject:**

```java
KafkaTemplate<String, String> kafkaTemplate;
ObjectMapper objectMapper;
```

**Cần thêm vào cuối method createPost(), TRƯỚC return:**

```java
// Publish POST_CREATED event cho CQRS Feed
publishPostCreatedEvent(post);
```

**Cần thêm method mới:**

```java
private void publishPostCreatedEvent(Post post) {
    try {
        // 1. Lấy profile info (đã có từ createPost)
        var profileResponse = profileClient.getProfile(post.getUserId());
        String authorUsername = "";
        String authorAvatar = "";
        if (profileResponse != null && profileResponse.getResult() != null) {
            var profile = profileResponse.getResult();
            authorUsername = profile.getUsername() != null ? profile.getUsername() : "";
            authorAvatar = profile.getImageUrl() != null ? profile.getImageUrl() : "";
        }

        // 2. Lấy danh sách follower IDs
        List<String> followerIds = List.of();
        try {
            var followerResponse = profileClient.getFollowerIds(post.getUserId());
            if (followerResponse != null && followerResponse.getResult() != null) {
                followerIds = followerResponse.getResult();
            }
        } catch (Exception e) {
            log.warn("Failed to get follower IDs for user {}", post.getUserId(), e);
        }

        if (followerIds.isEmpty()) {
            log.debug("No followers for user {}, skipping feed event", post.getUserId());
            return;
        }

        // 3. Build event payload
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "POST_CREATED");
        event.put("postId", post.getId());
        event.put("authorId", post.getUserId());
        event.put("content", post.getContent());
        event.put("mediaUrls", post.getMediaUrls() != null ? post.getMediaUrls() : List.of());
        event.put("authorUsername", authorUsername);
        event.put("authorFirstName", post.getFirstName());
        event.put("authorLastName", post.getLastName());
        event.put("authorAvatar", authorAvatar);
        event.put("followerIds", followerIds);

        // 4. Publish lên Kafka topic "post-events"
        String json = objectMapper.writeValueAsString(event);
        kafkaTemplate.send("post-events", post.getId(), json);

        log.info("Published POST_CREATED event for post {} → {} followers",
                post.getId(), followerIds.size());
    } catch (Exception e) {
        log.error("Failed to publish POST_CREATED event for post {}", post.getId(), e);
        // Không throw exception → post vẫn được tạo, chỉ feed không cập nhật
    }
}
```

---

### Thay đổi 4 (tùy chọn): Publish POST_DELETED event khi xóa post

**Tại sao cần:** Khi user xóa post, cần xóa PostFeedItem tương ứng khỏi feed của tất cả followers. FeedProjectionService đã handle `POST_DELETED` event nhưng chưa ai publish event này.

**File cần sửa:** `content-service/src/main/java/com/contentservice/post/service/PostService.java`

**Thêm vào method deletePost(), sau khi delete post:**

```java
// Publish POST_DELETED event
try {
    Map<String, Object> event = Map.of(
        "eventType", "POST_DELETED",
        "postId", postId
    );
    String json = objectMapper.writeValueAsString(event);
    kafkaTemplate.send("post-events", postId, json);
} catch (Exception e) {
    log.error("Failed to publish POST_DELETED event for post {}", postId, e);
}
```

---

### Thay đổi 5 (tùy chọn): Implement handleEngagementUpdate trong FeedProjectionService

**Tại sao cần:** Hiện tại method `handleEngagementUpdate()` rỗng. Khi user like/comment post, likeCount/commentCount trong PostFeedItem không được cập nhật → feed hiển thị sai engagement counts.

**File cần sửa:** `content-service/src/main/java/com/contentservice/kafka/FeedProjectionService.java`

**Thay thế method rỗng bằng:**

```java
private void handleEngagementUpdate(Map<String, Object> event, String field) {
    String postId = (String) event.get("postId");
    List<PostFeedItem> feedItems = postFeedRepository.findAllByPostId(postId);
    for (PostFeedItem item : feedItems) {
        if ("likeCount".equals(field)) {
            item.setLikeCount(item.getLikeCount() + 1);
        } else if ("commentCount".equals(field)) {
            item.setCommentCount(item.getCommentCount() + 1);
        }
        postFeedRepository.save(item);
    }
    log.debug("Updated {} for post {} across {} feed items", field, postId, feedItems.size());
}
```

**Cần thêm vào PostFeedRepository:**

```java
List<PostFeedItem> findAllByPostId(String postId);
```

---

### Tóm tắt thứ tự thay đổi

| # | File | Service | Thay đổi | Bắt buộc |
|---|------|---------|----------|----------|
| 1 | `UserProfileRepository.java` | user-service | Thêm `findFollowerUserIdsByUserId()` query | Có |
| 2 | `UserProfileService.java` | user-service | Thêm `getFollowerUserIds()` method | Có |
| 3 | `InternalUserProfileController.java` | user-service | Thêm `GET /internal/users/{userId}/follower-ids` | Có |
| 4 | `ProfileClient.java` | content-service | Thêm `getFollowerIds()` Feign method | Có |
| 5 | `PostService.java` | content-service | Inject KafkaTemplate, ObjectMapper; thêm `publishPostCreatedEvent()` | Có |
| 6 | `PostService.java` | content-service | Thêm POST_DELETED event trong deletePost() | Tùy chọn |
| 7 | `FeedProjectionService.java` | content-service | Implement `handleEngagementUpdate()` | Tùy chọn |
| 8 | `PostFeedRepository.java` | content-service | Thêm `findAllByPostId()` | Tùy chọn (cho #7) |

## Hướng dẫn Test

### Test 1: Kiểm tra tạo PostFeedItem khi tạo post

```bash
# Đăng nhập lấy token
curl -X POST http://localhost:8888/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser", "password": "password123"}'

# Tạo post mới
curl -X POST http://localhost:8888/api/post/create \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"content": "Hello from CQRS Feed test!"}'

# Kiểm tra log content-service
docker logs content-service 2>&1 | grep "Feed populated"
```

### Test 2: Query feed của follower

```bash
# Đăng nhập bằng tài khoản follower
# Gọi API lấy feed
curl -X GET "http://localhost:8888/api/post/my-feed?page=1&limit=20" \
  -H "Authorization: Bearer <FOLLOWER_TOKEN>"
```

### Test 3: Kiểm tra Neo4j trực tiếp

```cypher
MATCH (f:post_feed) RETURN f
MATCH (f:post_feed {targetUserId: "follower-user-id"}) RETURN f ORDER BY f.createdDate DESC
MATCH (f:post_feed) RETURN COUNT(f)
```

## Checklist

- [x] Tạo PostFeedItem entity (Neo4j Node)
- [x] Tạo PostFeedRepository (Neo4jRepository)
- [x] Tạo FeedProjectionService (Kafka consumer)
- [x] Tạo FeedService (query side với caching)
- [x] Tạo endpoint GET /posts/my-feed trong PostController
- [ ] **Thêm `findFollowerUserIdsByUserId` query trong UserProfileRepository (user-service)**
- [ ] **Thêm `getFollowerUserIds` method trong UserProfileService (user-service)**
- [ ] **Thêm endpoint `GET /internal/users/{userId}/follower-ids` (user-service)**
- [ ] **Thêm `getFollowerIds` method trong ProfileClient (content-service)**
- [ ] **Sửa PostService.createPost() → publish POST_CREATED event lên Kafka "post-events"**
- [ ] Thêm POST_DELETED event trong PostService.deletePost() (tùy chọn)
- [ ] Implement handleEngagementUpdate trong FeedProjectionService (tùy chọn)
- [ ] Test: tạo post → PostFeedItem tạo cho followers → query feed thành công
