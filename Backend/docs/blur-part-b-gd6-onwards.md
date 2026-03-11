
---

## GĐ 6: CQRS + FEED READ MODEL

> **Mục tiêu:** Tách read/write cho Post Feed.
> Write: MongoDB `posts` collection (như hiện tại).
> Read: MongoDB `post_feed` collection (denormalized, tối ưu cho query feed).

### 6.1 Ý tưởng

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

### 6.2 PostFeedItem Entity (Read Model)

**File:** `content-service/src/.../post/entity/PostFeedItem.java`

```java
package com.blur.contentservice.post.entity;

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

### 6.3 PostFeedRepository

**File:** `content-service/src/.../post/repository/PostFeedRepository.java`

```java
package com.blur.contentservice.post.repository;

import com.blur.contentservice.post.entity.PostFeedItem;
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

### 6.4 FeedProjectionService (Event Consumer)

**File:** `content-service/src/.../kafka/FeedProjectionService.java`

```java
package com.blur.contentservice.kafka;

import com.blur.contentservice.post.entity.PostFeedItem;
import com.blur.contentservice.post.repository.PostFeedRepository;
import com.blur.contentservice.post.repository.PostRepository;
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

### 6.5 FeedService (Query Side)

**File:** `content-service/src/.../post/service/FeedService.java`

```java
package com.blur.contentservice.post.service;

import com.blur.contentservice.post.entity.PostFeedItem;
import com.blur.contentservice.post.repository.PostFeedRepository;
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

### 6.6 Checklist GĐ 6

- [ ] Tạo PostFeedItem entity (read model)
- [ ] Tạo PostFeedRepository
- [ ] Tạo FeedProjectionService (Kafka consumer)
- [ ] Tạo FeedService (query side)
- [ ] Sửa PostService.createPost() → publish POST_CREATED event
- [ ] Thêm FeedController endpoint: GET /post/feed?page=0&size=20
- [ ] Test: create post → feed item created cho followers → query feed

---

## GĐ 7: REDIS CACHE NÂNG CAO

> **Mục tiêu:** Cache intelligently: user profiles, feed, recommendations.
> Invalidation qua Kafka events (follow/unfollow → invalidate recommendation cache).

### 7.1 Redis DB Layout

| DB | Service | Dữ liệu |
|----|---------|----------|
| 0 | Identity | Token blacklist, online status |
| 1 | Content | Post cache, feed cache |
| 2 | Profile | Profile cache, recommendation cache |
| 3 | Communication | Session cache, message dedup |

### 7.2 Cache Config cho Profile Service

**File:** `profile-service/src/.../configuration/CacheConfig.java`

```java
package com.blur.profileservice.configuration;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        // Default TTL 10 phút
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        // Custom TTL cho từng cache name
        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
                "profiles", defaultConfig.entryTtl(Duration.ofMinutes(30)),
                "recommendations", defaultConfig.entryTtl(Duration.ofMinutes(5)),
                "followers", defaultConfig.entryTtl(Duration.ofMinutes(15)),
                "following", defaultConfig.entryTtl(Duration.ofMinutes(15))
        );

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
```

### 7.3 Cache Invalidation Service

**File:** `profile-service/src/.../service/CacheInvalidationService.java`

```java
package com.blur.profileservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheInvalidationService {

    @Caching(evict = {
            @CacheEvict(value = "recommendations", allEntries = true),
            @CacheEvict(value = "followers", key = "#userId"),
            @CacheEvict(value = "following", key = "#userId")
    })
    public void invalidateOnFollow(String userId) {
        log.debug("Cache invalidated for follow event: userId={}", userId);
    }

    @Caching(evict = {
            @CacheEvict(value = "profiles", key = "#userId"),
            @CacheEvict(value = "recommendations", allEntries = true)
    })
    public void invalidateOnProfileUpdate(String userId) {
        log.debug("Cache invalidated for profile update: userId={}", userId);
    }
}
```

### 7.4 Checklist GĐ 7

- [ ] Tạo CacheConfig với TTL customized
- [ ] Tạo CacheInvalidationService
- [ ] Thêm @Cacheable cho UserProfileService.getProfile()
- [ ] Thêm @CacheEvict cho follow/unfollow
- [ ] Test: cache hit ratio (expect >80%), invalidation qua follow

---

## GĐ 8: KEYCLOAK MIGRATION

> **Mục tiêu:** Chuyển từ custom JWT → Keycloak cho SSO, OAuth2, role management.
> Giai đoạn này phức tạp, triển khai CUỐI vì ảnh hưởng tất cả services.

### 8.1 Docker Compose - Thêm Keycloak

```yaml
# Thêm vào docker-compose.yml
keycloak:
  image: quay.io/keycloak/keycloak:23.0
  command: start-dev
  environment:
    KC_DB: mysql
    KC_DB_URL: jdbc:mysql://mysql:3306/keycloak
    KC_DB_USERNAME: root
    KC_DB_PASSWORD: root
    KEYCLOAK_ADMIN: admin
    KEYCLOAK_ADMIN_PASSWORD: admin
  ports:
    - "9090:8080"
  depends_on:
    - mysql
```

### 8.2 API Gateway Security Config

**File:** `api-gateway/src/.../configuration/SecurityConfig.java`

```java
package com.blur.apigateway.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/identity/auth/**",
            "/api/identity/users/registration",
            "/actuator/health"
    };

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtDecoder(/* Keycloak JWT decoder */)
                        )
                );

        return http.build();
    }
}
```

### 8.3 application.yaml cho Keycloak

```yaml
# Thêm vào api-gateway/application.yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://localhost:9090/realms/blur}
          jwk-set-uri: ${KEYCLOAK_JWK_URI:http://localhost:9090/realms/blur/protocol/openid-connect/certs}
```

### 8.4 Checklist GĐ 8

- [ ] Setup Keycloak container + realm "blur"
- [ ] Tạo clients: api-gateway, user-service, content-service, communication-service
- [ ] Migrate users từ MySQL → Keycloak (script)
- [ ] Cập nhật Gateway SecurityConfig → OAuth2 Resource Server
- [ ] Cập nhật tất cả services → validate Keycloak JWT
- [ ] Test: login via Keycloak → access API → token refresh

---

## GĐ 9: RESILIENCE4J

> **Mục tiêu:** Circuit breaker + retry + rate limiting cho inter-service calls.

### 9.1 pom.xml dependency

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>
```

### 9.2 application.yaml config

```yaml
# Thêm vào application.yaml của mỗi service
resilience4j:
  circuitbreaker:
    instances:
      profileService:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        permitted-number-of-calls-in-half-open-state: 3
      identityService:
        sliding-window-size: 10
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s

  retry:
    instances:
      profileService:
        max-attempts: 3
        wait-duration: 500ms
        retry-exceptions:
          - java.io.IOException
          - java.util.concurrent.TimeoutException

  timelimiter:
    instances:
      profileService:
        timeout-duration: 3s
```

### 9.3 Sử dụng trong Service

```java
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@Service
public class ContentService {

    @CircuitBreaker(name = "profileService", fallbackMethod = "getProfileFallback")
    @Retry(name = "profileService")
    public UserProfileResponse getProfile(String userId) {
        return profileClient.getProfile(userId).getResult();
    }

    public UserProfileResponse getProfileFallback(String userId, Exception e) {
        // Trả về cached version hoặc default profile
        return UserProfileResponse.builder()
                .userId(userId)
                .username("user_" + userId.substring(0, 6))
                .firstName("Unknown")
                .lastName("User")
                .build();
    }
}
```

### 9.4 Checklist GĐ 9

- [ ] Thêm resilience4j dependency cho tất cả services
- [ ] Config circuit breaker cho inter-service calls
- [ ] Config retry cho transient failures
- [ ] Implement fallback methods
- [ ] Test: kill profile-service → circuit opens → fallback → recover

---

## GĐ 10: RATE LIMITING + FRONTEND UPGRADES

> **Mục tiêu:** Bảo vệ API khỏi abuse + cập nhật frontend tương thích.

### 10.1 Rate Limiting trong API Gateway

**pom.xml:**
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
</dependency>
```

**application.yaml:**
```yaml
spring:
  cloud:
    gateway:
      default-filters:
        - name: RequestRateLimiter
          args:
            redis-rate-limiter:
              replenishRate: 10       # 10 requests/second
              burstCapacity: 20      # burst tối đa 20
              requestedTokens: 1
            key-resolver: "#{@userKeyResolver}"
```

**UserKeyResolver:**
```java
package com.blur.apigateway.configuration;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    @Bean
    public KeyResolver userKeyResolver() {
        // Rate limit theo user ID (từ JWT) hoặc IP
        return exchange -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            if (userId != null) {
                return Mono.just(userId);
            }
            return Mono.just(
                    exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
            );
        };
    }
}
```

### 10.2 Frontend - Cập nhật API calls

**Thay đổi chính:**
1. WebSocket: Socket.IO → STOMP over SockJS
2. Notifications: Polling → STOMP subscription
3. Moderation status: Hiển thị PENDING → APPROVED/REJECTED badge

```typescript
// frontend/src/api/websocket.ts
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const GATEWAY_WS = 'http://localhost:8888/ws';

export const createStompClient = (token: string): Client => {
    return new Client({
        webSocketFactory: () => new SockJS(GATEWAY_WS),
        connectHeaders: { Authorization: `Bearer ${token}` },
        onConnect: () => console.log('STOMP connected'),
        onDisconnect: () => console.log('STOMP disconnected'),
        reconnectDelay: 5000,
    });
};
```

### 10.3 Checklist GĐ 10

- [ ] Thêm rate limiter vào API Gateway
- [ ] Config Redis cho rate limit store
- [ ] Frontend: migrate Socket.IO → STOMP
- [ ] Frontend: update notification subscription
- [ ] Frontend: hiển thị moderation status
- [ ] Test: rate limit khi vượt 10 req/s → 429 Too Many Requests

---

## TỔNG KẾT

| GĐ | Mô tả | Độ phức tạp | Dependencies |
|----|-------|------------|-------------|
| 1 | Refactor Model Service | ⭐⭐ | Không |
| 2 | Async AI Moderation | ⭐⭐⭐ | GĐ 1 |
| 3 | Neo4j Recommendations | ⭐⭐ | Không |
| 4 | Outbox Pattern | ⭐⭐⭐ | Không |
| 5 | Saga Choreography | ⭐⭐⭐⭐ | GĐ 4 |
| 6 | CQRS Feed | ⭐⭐⭐ | GĐ 4 |
| 7 | Redis Cache | ⭐⭐ | Không |
| 8 | Keycloak | ⭐⭐⭐⭐⭐ | Không |
| 9 | Resilience4j | ⭐⭐ | Không |
| 10 | Rate Limiting + FE | ⭐⭐⭐ | GĐ 8 |

**Thứ tự triển khai đề nghị:**
1 → 4 → 5 → 2 → 3 → 7 → 6 → 9 → 8 → 10
