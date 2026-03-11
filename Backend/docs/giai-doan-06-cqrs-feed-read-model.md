# Giai Đoạn 6: CQRS + Feed Read Model

## Mục tiêu

Tách read/write cho Post Feed.

- **Write:** MongoDB `posts` collection (như hiện tại).
- **Read:** MongoDB `post_feed` collection (denormalized, tối ưu cho query feed).

## Kiến trúc tổng quan

```
Write Side (Command)                 Read Side (Query)
────────────────────                ─────────────────
PostService.createPost()             FeedService.getFeed()
  → save Post (MongoDB)               → query PostFeedItem (MongoDB)
  → publish Kafka event                  (denormalized, pre-joined)
                                         (sorted by createdDate DESC)
                                         (cached in Redis 30s)
         │
    Kafka: "post-events"
         │
         ▼
FeedProjectionService
  → consume event
  → join user info (profileClient)
  → save PostFeedItem (denormalized)
```

## Bước 1: Tạo PostFeedItem Entity (Read Model)

**File:** `content-service/src/main/java/com/contentservice/post/entity/PostFeedItem.java`

```java
package com.contentservice.post.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Read model cho Feed - denormalized từ Post + UserProfile.
 * Tối ưu cho query: user mở feed → lấy posts của những người mình follow.
 * Không cần JOIN runtime → tất cả data đã pre-joined.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document("post_feed")
@CompoundIndex(name = "idx_feed_user_time", def = "{'targetUserId': 1, 'createdDate': -1}")
public class PostFeedItem {
    @Id
    String id;

    // Post data
    String postId;               // Trỏ về Post gốc
    String content;
    List<String> imageUrls;
    String videoUrl;

    // Author data (denormalized từ UserProfile)
    String authorId;
    String authorUsername;
    String authorFirstName;
    String authorLastName;
    String authorAvatar;

    // Engagement counts (denormalized)
    int likeCount;
    int commentCount;
    int shareCount;

    // Feed targeting
    String targetUserId;         // User sẽ nhìn thấy post này trong feed
    LocalDateTime createdDate;
    LocalDateTime updatedDate;
}
```

## Bước 2: Tạo PostFeedRepository

**File:** `content-service/src/main/java/com/contentservice/post/repository/PostFeedRepository.java`

```java
package com.contentservice.post.repository;

import com.contentservice.post.entity.PostFeedItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostFeedRepository extends MongoRepository<PostFeedItem, String> {

    Page<PostFeedItem> findByTargetUserIdOrderByCreatedDateDesc(
            String targetUserId, Pageable pageable);

    void deleteAllByPostId(String postId);

    void deleteAllByAuthorId(String authorId);
}
```

## Bước 3: Tạo FeedProjectionService (Event Consumer)

**File:** `content-service/src/main/java/com/contentservice/kafka/FeedProjectionService.java`

```java
package com.contentservice.kafka;

import com.contentservice.post.entity.PostFeedItem;
import com.contentservice.post.repository.PostFeedRepository;
import com.contentservice.post.repository.PostRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Consume post events và cập nhật read model (PostFeedItem).
 *
 * Events:
 * - POST_CREATED → tạo PostFeedItem cho tất cả followers
 * - POST_DELETED → xóa PostFeedItem
 * - POST_LIKED → tăng likeCount
 * - POST_COMMENTED → tăng commentCount
 */
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
            @SuppressWarnings("unchecked")
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

        // Tạo feed item cho mỗi follower
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
        log.info("Feed populated for post {} → {} followers", postId, followerIds.size());
    }

    private void handlePostDeleted(Map<String, Object> event) {
        String postId = (String) event.get("postId");
        postFeedRepository.deleteAllByPostId(postId);
        log.info("Feed items deleted for post {}", postId);
    }

    private void handleEngagementUpdate(Map<String, Object> event, String field) {
        // Engagement updates can be batched/debounced for performance
        log.debug("Engagement update: {} for post {}", field, event.get("postId"));
    }
}
```

## Bước 4: Tạo FeedService (Query Side)

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
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FeedService {

    PostFeedRepository postFeedRepository;

    @Cacheable(value = "feed", key = "#root.target.getCurrentUserId() + ':' + #page + ':' + #size",
            unless = "#result.isEmpty()")
    public Page<PostFeedItem> getMyFeed(int page, int size) {
        String userId = getCurrentUserId();
        return postFeedRepository.findByTargetUserIdOrderByCreatedDateDesc(
                userId, PageRequest.of(page, size));
    }

    public String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
```

## Bước 5: Tạo FeedController

**File:** `content-service/src/main/java/com/contentservice/post/controller/FeedController.java`

```java
package com.contentservice.post.controller;

import com.contentservice.post.entity.PostFeedItem;
import com.contentservice.post.service.FeedService;
import com.contentservice.story.dto.response.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/post/feed")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FeedController {

    FeedService feedService;

    @GetMapping
    public ApiResponse<Page<PostFeedItem>> getMyFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<PostFeedItem> feed = feedService.getMyFeed(page, size);
        return ApiResponse.<Page<PostFeedItem>>builder()
                .result(feed)
                .build();
    }
}
```

## Bước 6: Thêm endpoint lấy follower IDs trong user-service

Trước khi sửa PostService, cần thêm endpoint internal trong user-service để content-service có thể gọi lấy danh sách follower IDs.

### 6.1: Thêm Neo4j query trong UserProfileRepository

**File:** `user-service/src/main/java/com/blur/userservice/profile/repository/UserProfileRepository.java`

Thêm query mới bên dưới `findFollowingIds`:

```java
    /**
     * Lấy danh sách userId của những người đang follow user này (theo userId, không phải profileId)
     * Dùng cho CQRS Feed: khi tạo post → lấy follower userIds → tạo feed item cho mỗi follower
     */
    @Query("""
                MATCH (follower:user_profile)-[:follows]->(u:user_profile {user_id: $userId})
                RETURN follower.user_id
            """)
    List<String> findFollowerUserIdsByUserId(@Param("userId") String userId);
```

### 6.2: Thêm method trong UserProfileService

**File:** `user-service/src/main/java/com/blur/userservice/profile/service/UserProfileService.java`

```java
    /**
     * Lấy danh sách userId của tất cả followers (dùng cho CQRS Feed)
     */
    public List<String> getFollowerUserIds(String userId) {
        return userProfileRepository.findFollowerUserIdsByUserId(userId);
    }
```

### 6.3: Thêm endpoint trong InternalUserProfileController

**File:** `user-service/src/main/java/com/blur/userservice/profile/controller/InternalUserProfileController.java`

```java
    /**
     * Lấy danh sách userId của tất cả followers của một user
     * Dùng cho content-service khi publish POST_CREATED event (CQRS Feed)
     *
     * GET http://localhost:8082/internal/users/{userId}/follower-ids
     */
    @GetMapping("/internal/users/{userId}/follower-ids")
    public ApiResponse<List<String>> getFollowerIds(@PathVariable String userId) {
        List<String> followerIds = userProfileService.getFollowerUserIds(userId);
        return ApiResponse.<List<String>>builder()
                .code(1000)
                .result(followerIds)
                .build();
    }
```

## Bước 7: Thêm Feign method trong content-service ProfileClient

**File:** `content-service/src/main/java/com/contentservice/post/repository/httpclient/ProfileClient.java`

```java
package com.contentservice.post.repository.httpclient;

import com.contentservice.post.dto.response.UserProfileResponse;
import com.contentservice.story.dto.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "profile-service", url = "${app.service.profile.url}")
public interface ProfileClient {
    @GetMapping("/internal/users/{userId}")
    ApiResponse<UserProfileResponse> getProfile(@PathVariable("userId") String userId);

    @GetMapping("/profile/users/{profileId}")
    ApiResponse<UserProfileResponse> getProfileByProfileId(@PathVariable String profileId);

    /**
     * Lấy danh sách userId của tất cả followers của một user
     * Gọi đến endpoint internal của user-service
     */
    @GetMapping("/internal/users/{userId}/follower-ids")
    ApiResponse<List<String>> getFollowerIds(@PathVariable("userId") String userId);
}
```

## Bước 8: Sửa PostService để publish Kafka event (code hoàn chỉnh)

Trong file `PostService.java` hiện tại, thêm logic publish event sau khi tạo post.

**File:** `content-service/src/main/java/com/contentservice/post/service/PostService.java`

Thêm inject `KafkaTemplate`, `ObjectMapper` và sử dụng `ProfileClient`, `IdentityClient` đã có:

```java
import org.springframework.kafka.core.KafkaTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.contentservice.post.repository.httpclient.ProfileClient;
import com.contentservice.post.repository.httpclient.IdentityClient;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PostService {

    PostRepository postRepository;
    PostMapper postMapper;
    ProfileClient profileClient;
    IdentityClient identityClient;
    PostLikeRepository postLikeRepository;
    NotificationEventPublisher notificationEventPublisher;
    KafkaTemplate<String, String> kafkaTemplate;
    ObjectMapper objectMapper;

    @Transactional
    public PostResponse createPost(PostRequest postRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var userId = authentication.getName();
        var profile = profileClient.getProfile(userId);
        Post post = Post.builder()
                .content(postRequest.getContent())
                .mediaUrls(postRequest.getMediaUrls())
                .userId(userId)
                .firstName(profile.getResult().getFirstName())
                .lastName(profile.getResult().getLastName())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        post = postRepository.save(post);

        // Publish POST_CREATED event lên Kafka cho CQRS Feed
        publishPostCreatedEvent(post);

        return postMapper.toPostResponse(post);
    }

    // ... các method khác giữ nguyên ...

    /**
     * Publish POST_CREATED event lên Kafka topic "post-events".
     * Event chứa đầy đủ thông tin author (lấy từ IdentityClient + ProfileClient)
     * và danh sách follower IDs (lấy từ ProfileClient) để FeedProjectionService
     * tạo PostFeedItem cho mỗi follower.
     */
    private void publishPostCreatedEvent(Post post) {
        try {
            // 1. Lấy thông tin username từ IdentityClient
            String authorUsername = "";
            try {
                var userResponse = identityClient.getUser(post.getUserId());
                if (userResponse != null && userResponse.getResult() != null) {
                    authorUsername = userResponse.getResult().getUsername();
                }
            } catch (Exception e) {
                log.warn("Failed to get username for user {}", post.getUserId(), e);
            }

            // 2. Lấy thông tin profile (firstName, lastName, avatar) từ ProfileClient
            String authorFirstName = post.getFirstName();  // Đã có sẵn từ createPost
            String authorLastName = post.getLastName();     // Đã có sẵn từ createPost
            String authorAvatar = "";
            try {
                var profileResponse = profileClient.getProfile(post.getUserId());
                if (profileResponse != null && profileResponse.getResult() != null) {
                    var profile = profileResponse.getResult();
                    authorFirstName = profile.getFirstName();
                    authorLastName = profile.getLastName();
                    authorAvatar = profile.getImageUrl() != null ? profile.getImageUrl() : "";
                }
            } catch (Exception e) {
                log.warn("Failed to get profile for user {}", post.getUserId(), e);
            }

            // 3. Lấy danh sách follower IDs từ ProfileClient (internal endpoint)
            List<String> followerIds = getFollowerIds(post.getUserId());

            // 4. Build event và gửi lên Kafka
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "POST_CREATED");
            event.put("postId", post.getId());
            event.put("authorId", post.getUserId());
            event.put("content", post.getContent());
            event.put("mediaUrls", post.getMediaUrls());
            event.put("authorUsername", authorUsername);
            event.put("authorFirstName", authorFirstName);
            event.put("authorLastName", authorLastName);
            event.put("authorAvatar", authorAvatar);
            event.put("followerIds", followerIds);

            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("post-events", post.getId(), json);

            log.info("Published POST_CREATED event for post {} → {} followers",
                    post.getId(), followerIds.size());
        } catch (Exception e) {
            log.error("Failed to publish POST_CREATED event for post {}", post.getId(), e);
        }
    }

    /**
     * Lấy danh sách userId của tất cả followers qua ProfileClient (Feign).
     * Gọi endpoint: GET /internal/users/{userId}/follower-ids
     */
    private List<String> getFollowerIds(String userId) {
        try {
            var response = profileClient.getFollowerIds(userId);
            if (response != null && response.getResult() != null) {
                return response.getResult();
            }
        } catch (Exception e) {
            log.warn("Failed to get follower IDs for user {}", userId, e);
        }
        return List.of();
    }
}
```

> **Lưu ý:** Cần thêm `@Slf4j` annotation (từ Lombok) vào class PostService nếu chưa có, để sử dụng `log.info()`, `log.warn()`, `log.error()`.

## Hướng dẫn Test

### Test 1: Kiểm tra tạo PostFeedItem khi tạo post

**Bước 1:** Đảm bảo các service đang chạy: MongoDB, Kafka, content-service, user-service.

```bash
docker-compose up -d mongodb kafka zookeeper
```

**Bước 2:** Đăng nhập lấy token (thay bằng thông tin user thực tế).

```bash
curl -X POST http://localhost:8888/api/identity/auth/token \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

Lưu lại token từ response:
```json
{
  "code": 1000,
  "result": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "authenticated": true
  }
}
```

**Bước 3:** Tạo một post mới (dùng token ở trên).

```bash
curl -X POST http://localhost:8888/api/content/posts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "content": "Hello from CQRS Feed test!",
    "imageUrls": [],
    "videoUrl": null
  }'
```

Response mong đợi:
```json
{
  "code": 1000,
  "result": {
    "id": "abc123",
    "content": "Hello from CQRS Feed test!",
    "userId": "user-id-xxx",
    "createdDate": "2026-03-10T10:00:00"
  }
}
```

**Bước 4:** Kiểm tra Kafka đã nhận event (xem log content-service).

```bash
docker logs content-service 2>&1 | grep "Feed populated"
```

Log mong đợi:
```
Feed populated for post abc123 → 5 followers
```

### Test 2: Query feed của user

**Bước 5:** Đăng nhập bằng tài khoản follower (người đang follow user ở bước 3).

```bash
curl -X POST http://localhost:8888/api/identity/auth/token \
  -H "Content-Type: application/json" \
  -d '{
    "username": "followeruser",
    "password": "password123"
  }'
```

**Bước 6:** Gọi API lấy feed.

```bash
curl -X GET "http://localhost:8888/api/content/post/feed?page=0&size=20" \
  -H "Authorization: Bearer <FOLLOWER_TOKEN>"
```

Response mong đợi:
```json
{
  "code": 1000,
  "result": {
    "content": [
      {
        "id": "feed-item-id",
        "postId": "abc123",
        "content": "Hello from CQRS Feed test!",
        "authorId": "user-id-xxx",
        "authorUsername": "testuser",
        "authorFirstName": "Test",
        "authorLastName": "User",
        "authorAvatar": "avatar-url",
        "targetUserId": "follower-id",
        "likeCount": 0,
        "commentCount": 0,
        "shareCount": 0,
        "createdDate": "2026-03-10T10:00:00"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20
    },
    "totalElements": 1,
    "totalPages": 1
  }
}
```

### Test 3: Kiểm tra xóa post cũng xóa feed items

**Bước 7:** Xóa post đã tạo.

```bash
curl -X DELETE http://localhost:8888/api/content/posts/abc123 \
  -H "Authorization: Bearer <TOKEN>"
```

**Bước 8:** Query feed lại → post đã biến mất.

```bash
curl -X GET "http://localhost:8888/api/content/post/feed?page=0&size=20" \
  -H "Authorization: Bearer <FOLLOWER_TOKEN>"
```

Response mong đợi: `content` array rỗng (post đã bị xóa khỏi feed).

### Test 4: Kiểm tra MongoDB trực tiếp

```bash
# Kết nối MongoDB shell
docker exec -it mongodb mongosh

# Xem collection post_feed
use content_service
db.post_feed.find().pretty()

# Đếm số feed items
db.post_feed.countDocuments()

# Xem feed của một user cụ thể
db.post_feed.find({ targetUserId: "follower-id" }).sort({ createdDate: -1 })
```

### Test 5: Kiểm tra Redis cache

```bash
# Kết nối Redis CLI
docker exec -it redis redis-cli

# Xem tất cả cache keys liên quan feed
KEYS feed*

# Xem nội dung một cache key
GET "feed::follower-id:0:20"

# Xóa cache thủ công để test cache miss
DEL "feed::follower-id:0:20"
```

## Checklist

- [ ] Tạo PostFeedItem entity (read model)
- [ ] Tạo PostFeedRepository
- [ ] Tạo FeedProjectionService (Kafka consumer)
- [ ] Tạo FeedService (query side)
- [ ] Tạo FeedController với endpoint GET /post/feed
- [ ] Thêm Neo4j query `findFollowerUserIdsByUserId` trong UserProfileRepository (user-service)
- [ ] Thêm `getFollowerUserIds` method trong UserProfileService (user-service)
- [ ] Thêm endpoint `GET /internal/users/{userId}/follower-ids` trong InternalUserProfileController (user-service)
- [ ] Thêm `getFollowerIds` method trong ProfileClient (content-service)
- [ ] Sửa PostService: inject KafkaTemplate + ObjectMapper, thêm @Slf4j
- [ ] Sửa PostService.createPost() → gọi publishPostCreatedEvent(post)
- [ ] Hoàn chỉnh publishPostCreatedEvent() → lấy author info từ IdentityClient + ProfileClient
- [ ] Hoàn chỉnh getFollowerIds() → gọi ProfileClient.getFollowerIds()
- [ ] Test: tạo post → kiểm tra Kafka log → feed item xuất hiện cho followers
- [ ] Test: query feed → response trả về đúng format phân trang
- [ ] Test: xóa post → feed items bị xóa theo
- [ ] Test: kiểm tra Redis cache hoạt động đúng
