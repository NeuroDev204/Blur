# Giai Doan 7: Redis Cache Nang Cao

## Muc tieu

Nang cap he thong Redis cache hien tai len production-grade. Giai doan nay gom 2 phan:

- **Phan A:** Fix cac van de nghiem trong cua he thong cache hien tai (BẮT BUỘC)
- **Phan B:** Them cac tinh nang nang cao (Multi-level Cache, Distributed Lock, Pub/Sub, Lua Script, Cache Warming)

---

## Redis DB Layout hien tai

| DB | Service | Du lieu |
|----|---------|--------|
| 0 | User Service | Token blacklist, online status (RedisMultiDbConfig) |
| 1 | Content Service | Post cache, feed cache, comment cache (RedisConfig) |
| 2 | User Service | Profile cache, recommendation cache (RedisMultiDbConfig) |
| 0 | Communication Service | Session cache, message dedup, unread counts |

> **Luu y:** Communication Service dung DB 0 (default) - trung voi User Service DB 0. Nen chuyen sang DB 3 de tranh xung dot.

---

## PHAN A: FIX CAC VAN DE HIEN TAI

### A1. Fix Bao Mat - Thay LaissezFaireSubTypeValidator

**Van de:** `LaissezFaireSubTypeValidator` cho phep deserialize BAT KY class nao, tao lo hong bao mat (deserialization gadget attack).

**File can sua:**
- `content-service/.../configuration/RedisConfig.java`
- `user-service/.../profile/configuration/RedisMultiDbConfig.java`

**Truoc (KHONG AN TOAN):**
```java
mapper.activateDefaultTyping(
    LaissezFaireSubTypeValidator.instance,
    ObjectMapper.DefaultTyping.NON_FINAL,
    JsonTypeInfo.As.PROPERTY
);
```

**Sau (AN TOAN):**
```java
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
    .allowIfSubType("com.contentservice.")   // Chi cho phep class cua project
    .allowIfSubType("com.blur.")
    .allowIfSubType("java.util.")
    .allowIfSubType("java.time.")
    .allowIfSubType("java.lang.")
    .allowIfSubType("org.springframework.data.domain.")
    .build();

mapper.activateDefaultTyping(
    ptv,
    ObjectMapper.DefaultTyping.NON_FINAL,
    JsonTypeInfo.As.PROPERTY
);
```

**Ap dung cho:**
1. **content-service/RedisConfig.java** - method `redisObjectMapper()`
2. **user-service/RedisMultiDbConfig.java** - method `jsonSerializer()`

---

### A2. Thay KEYS bang SCAN - Tranh Block Redis

**Van de:** Lenh `KEYS *` la O(N), block toan bo Redis server khi co nhieu key. Day la van de NGHIEM TRONG nhat trong production.

**File can sua:**
- `content-service/.../post/service/CacheService.java`
- `communication-service/.../service/RedisCacheService.java`
- `communication-service/.../configuration/RedisConfig.java`

**Truoc (NGUY HIEM):**
```java
Set<String> keys = redisTemplate.keys("post-service:*");
if (keys != null && !keys.isEmpty()) {
    redisTemplate.delete(keys);
}
```

**Sau (AN TOAN):**
```java
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.Cursor;

private Set<String> scanKeys(String pattern) {
    Set<String> keys = new HashSet<>();
    ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
    try (Cursor<String> cursor = redisTemplate.scan(options)) {
        while (cursor.hasNext()) {
            keys.add(cursor.next());
        }
    }
    return keys;
}
```

**Ap dung cho tat ca cac method sau:**

**CacheService (content-service):**
```java
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CacheService {

    RedisTemplate<String, Object> redisTemplate;

    // ==================== SCAN HELPER (thay the KEYS) ====================

    private Set<String> scanKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        }
        return keys;
    }

    private void deleteByPattern(String pattern) {
        try {
            Set<String> keys = scanKeys(pattern);
            if (!keys.isEmpty()) {
                redisTemplate.unlink(keys);  // UNLINK thay DELETE (non-blocking)
            }
        } catch (Exception e) {
            log.warn("Failed to delete cache by pattern {}: {}", pattern, e.getMessage());
        }
    }

    // ==================== EVICT METHODS ====================

    public void evictPostCaches(String postId, String userId) {
        try {
            deleteByPattern("content-service:post::" + postId);
            deleteByPattern("content-service:userPosts::" + userId + "*");
            deleteByPattern("content-service:posts::*");
        } catch (Exception e) {
            log.warn("Failed to evict post caches for postId={}, userId={}: {}",
                postId, userId, e.getMessage());
        }
    }

    public void evictPostLikeCache(String postId) {
        try {
            deleteByPattern("content-service:postLikes::" + postId);
            deleteByPattern("content-service:post::" + postId);
        } catch (Exception e) {
            log.warn("Failed to evict post like cache for postId={}: {}", postId, e.getMessage());
        }
    }

    public void evictSavedPostsCache(String userId) {
        try {
            deleteByPattern("content-service:savedPosts::" + userId);
        } catch (Exception e) {
            log.warn("Failed to evict saved posts cache for userId={}: {}", userId, e.getMessage());
        }
    }

    public void evictCommentCaches(String postId, String commentId) {
        try {
            deleteByPattern("content-service:comments::" + postId);
            deleteByPattern("content-service:commentReplies::" + commentId);
        } catch (Exception e) {
            log.warn("Failed to evict comment caches for postId={}, commentId={}: {}",
                postId, commentId, e.getMessage());
        }
    }

    public void evictCommentReplyCaches(String commentId, String replyId, String parentReplyId) {
        try {
            deleteByPattern("content-service:commentReplies::" + commentId);
            if (replyId != null) {
                deleteByPattern("content-service:commentReplyById::" + replyId);
            }
            if (parentReplyId != null) {
                deleteByPattern("content-service:nestedReplies::" + parentReplyId);
            }
        } catch (Exception e) {
            log.warn("Failed to evict comment reply caches: {}", e.getMessage());
        }
    }

    // ==================== UTILITY METHODS ====================

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        } catch (Exception e) {
            log.warn("Failed to set cache key={}: {}", key, e.getMessage());
        }
    }

    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Failed to get cache key={}: {}", key, e.getMessage());
            return null;
        }
    }

    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("Failed to check cache key={}: {}", key, e.getMessage());
            return false;
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.unlink(key);  // non-blocking delete
        } catch (Exception e) {
            log.warn("Failed to delete cache key={}: {}", key, e.getMessage());
        }
    }

    public long getCacheSize() {
        try {
            return scanKeys("content-service:*").size();
        } catch (Exception e) {
            log.warn("Failed to get cache size: {}", e.getMessage());
            return -1;
        }
    }

    public long getCacheSizeByPattern(String pattern) {
        try {
            return scanKeys("content-service:" + pattern).size();
        } catch (Exception e) {
            log.warn("Failed to get cache size by pattern={}: {}", pattern, e.getMessage());
            return -1;
        }
    }

    public void clearAllCaches() {
        try {
            deleteByPattern("content-service:*");
            log.info("All content-service caches cleared");
        } catch (Exception e) {
            log.warn("Failed to clear all caches: {}", e.getMessage());
        }
    }

    public void warmUp(String cacheName, String key, Object value) {
        try {
            String fullKey = "content-service:" + cacheName + "::" + key;
            redisTemplate.opsForValue().set(fullKey, value, 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Failed to warm up cache {}::{}: {}", cacheName, key, e.getMessage());
        }
    }
}
```

**RedisCacheService (communication-service) - sua method `deleteByPattern`:**
```java
private void deleteByPattern(String pattern) {
    try {
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        }
        if (!keys.isEmpty()) {
            redisTemplate.unlink(keys);
        }
    } catch (Exception e) {
        log.warn("Failed to delete by pattern {}: {}", pattern, e.getMessage());
    }
}
```

**Cac method `getAllActiveCalls()`, `getAllUsersInCalls()` cung can doi:**
```java
public Set<String> getAllActiveCalls() {
    try {
        return scanKeys(CALL_STATE_PREFIX + "*");
    } catch (Exception e) {
        log.warn("Failed to get active calls: {}", e.getMessage());
        return Set.of();
    }
}

public Set<String> getAllUsersInCalls() {
    try {
        return scanKeys(USER_CALL_PREFIX + "*");
    } catch (Exception e) {
        log.warn("Failed to get users in calls: {}", e.getMessage());
        return Set.of();
    }
}
```

**Communication RedisConfig - startup cleanup:**
```java
@EventListener(ApplicationReadyEvent.class)
public void cleanupChatServiceCacheOnStartup() {
    try {
        RedisTemplate<String, Object> template = redisTemplate(connectionFactory);
        ScanOptions options = ScanOptions.scanOptions()
            .match("communication-service:*").count(100).build();
        Set<String> keys = new HashSet<>();
        try (Cursor<String> cursor = template.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        }
        if (!keys.isEmpty()) {
            template.unlink(keys);
            log.info("Cleaned up {} communication-service cache keys on startup", keys.size());
        }
    } catch (Exception e) {
        log.warn("Failed to cleanup cache on startup: {}", e.getMessage());
    }
}
```

---

### A3. Them Error Logging - Khong Silent Fail

**Van de:** Tat ca catch block deu rong `catch (Exception e) {}`, khong log gi ca. Khi cache co loi, hoan toan khong biet.

**Quy tac:**
- Moi catch block phai `log.warn(...)` voi context (key, method name)
- KHONG throw exception (van graceful degrade)
- Dung `@Slf4j` cua Lombok

**Ap dung cho:**
1. `content-service/CacheService.java` - tat ca method (da lam o A2)
2. `communication-service/RedisCacheService.java` - tat ca 30+ method
3. `user-service/RedisService.java` - tat ca method

**Vi du cho RedisCacheService (communication-service):**
```java
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j  // THEM ANNOTATION NAY
public class RedisCacheService {
    // ...

    public void cacheCallSession(String callId, Object session, long ttlSeconds) {
        try {
            String key = CALL_STATE_PREFIX + callId;
            redisTemplate.opsForValue().set(key, session, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to cache call session callId={}: {}", callId, e.getMessage());
        }
    }

    // Tuong tu cho TAT CA cac method khac...
}
```

**Vi du cho RedisService (user-service):**
```java
public void setOnline(String userId) {
    if (userId == null || userId.trim().isEmpty()) return;
    String key = ONLINE_KEY_PREFIX + userId;
    try {
        redisTemplate.opsForValue().set(key, System.currentTimeMillis(), ONLINE_TTL);
    } catch (RedisConnectionFailureException e) {
        log.warn("Redis connection failed for setOnline userId={}: {}", userId, e.getMessage());
    } catch (Exception e) {
        log.warn("Failed to set online status userId={}: {}", userId, e.getMessage());
    }
}
```

---

### A4. Fix Loi Chinh Ta Cache Name

**Van de:** Cache name bi sai chinh ta, dan den eviction KHONG hoat dong.

**File:** `user-service/.../profile/service/UserProfileService.java`

**Truoc (SAI):**
```java
@CacheEvict(value = "recommnedations:city", allEntries = true),    // THIEU 'e'
@CacheEvict(value = "recommedations:popular", allEntries = true),  // THIEU 'n'
```

**Sau (DUNG):**
```java
@CacheEvict(value = "recommendations:city", allEntries = true),
@CacheEvict(value = "recommendations:popular", allEntries = true),
```

**Cung fix o `@Cacheable`:**
```java
// Truoc (SAI):
@Cacheable(value = "recommnedations:city", ...)
@Cacheable(value = "recommedations:popular", ...)

// Sau (DUNG):
@Cacheable(value = "recommendations:city", ...)
@Cacheable(value = "recommendations:popular", ...)
```

**Va fix o method `invalidateRecommnedationCache()`:**
```java
// Truoc (SAI):
@CacheEvict(value = {
    "recommendations:mutual",
    "recommendations:taste",
    "recommnedation:city",      // SAI - thieu 's' va 'e'
    "recommendations:popular",
    "recommendations:combined",
    "recommendations:quick",
}, allEntries = true)
public void invalidateRecommnedationCache() {}

// Sau (DUNG):
@CacheEvict(value = {
    "recommendations:mutual",
    "recommendations:taste",
    "recommendations:city",
    "recommendations:popular",
    "recommendations:combined",
    "recommendations:quick",
}, allEntries = true)
public void invalidateRecommendationCache() {}
```

**Dinh nghia hang so de tranh loi chinh ta trong tuong lai:**
```java
public final class CacheNames {
    private CacheNames() {}

    // Content Service
    public static final String POSTS = "posts";
    public static final String POST = "post";
    public static final String USER_POSTS = "userPosts";
    public static final String POST_LIKES = "postLikes";
    public static final String SAVED_POSTS = "savedPosts";
    public static final String COMMENTS = "comments";
    public static final String COMMENT_REPLIES = "commentReplies";
    public static final String NESTED_REPLIES = "nestedReplies";
    public static final String COMMENT_REPLY_BY_ID = "commentReplyById";
    public static final String STORIES = "stories";
    public static final String STORY_BY_ID = "storyById";
    public static final String STORIES_BY_USER = "storiesByUser";
    public static final String MY_STORIES = "myStories";
    public static final String STORY_LIKES = "storyLikes";

    // User Service
    public static final String USERS = "users";
    public static final String USER_BY_ID = "userById";
    public static final String MY_INFO = "myInfo";
    public static final String PROFILES = "profiles";
    public static final String PROFILE_BY_USER_ID = "profileByUserId";
    public static final String MY_PROFILE = "myProfile";
    public static final String SEARCH_RESULTS = "searchResults";
    public static final String FOLLOWERS = "followers";
    public static final String FOLLOWING = "following";
    public static final String REC_MUTUAL = "recommendations:mutual";
    public static final String REC_TASTE = "recommendations:taste";
    public static final String REC_CITY = "recommendations:city";
    public static final String REC_POPULAR = "recommendations:popular";
    public static final String REC_COMBINED = "recommendations:combined";
    public static final String REC_QUICK = "recommendations:quick";
}
```

---

### A5. Them CacheErrorHandler cho content-service va communication-service

**Van de:** Chi user-service co CacheErrorHandler (fallback to DB khi cache loi). Content-service va communication-service se CRASH neu Redis chet.

**File moi:** `content-service/.../configuration/CacheErrorConfig.java`
```java
package com.contentservice.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class CacheErrorConfig implements CachingConfigurer {

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.warn("Cache GET error on '{}' key='{}': {} - falling back to DB",
                    cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                log.warn("Cache PUT error on '{}' key='{}': {}",
                    cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                log.warn("Cache EVICT error on '{}' key='{}': {}",
                    cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                log.warn("Cache CLEAR error on '{}': {}",
                    cache.getName(), e.getMessage());
            }
        };
    }
}
```

Tao file tuong tu cho communication-service: `communication-service/.../configuration/CacheErrorConfig.java`

---

### A6. Them sync=true Chong Cache Stampede

**Van de:** Khi cache het han (TTL expire), nhieu request dong thoi se TAT CA cung goi DB. Day goi la **Cache Stampede** (con goi la Thundering Herd).

**Giai phap don gian:** Them `sync = true` vao `@Cacheable`. Spring se dung lock de chi 1 thread load data, cac thread khac doi.

**Ap dung cho cac endpoint "hot" (nhieu request):**

**Content Service:**
```java
// PostService - getAllPots (feed)
// Chua co @Cacheable, can them:
@Cacheable(value = "posts", key = "'page-' + #page + '-' + #limit", sync = true,
    unless = "#result == null || #result.content.isEmpty()")
public Page<PostResponse> getAllPots(int page, int limit) { ... }

// PostService - getPostById
@Cacheable(value = "post", key = "#postId", sync = true, unless = "#result == null")
public PostResponse getPostById(String postId) { ... }

// PostService - getPostsByUserId
@Cacheable(value = "userPosts", key = "#userId", sync = true,
    unless = "#result == null || #result.isEmpty()")
public List<PostResponse> getPostsByUserId(String userId) { ... }

// CommentService
@Cacheable(value = "comments", key = "#postId", sync = true,
    unless = "#result == null || #result.isEmpty()")
public List<CommentResponse> getAllCommentByPostId(String postId) { ... }

// StoryService
@Cacheable(value = "stories", key = "'all'", sync = true,
    unless = "#result == null || #result.isEmpty()")
public List<Story> getAllStories() { ... }
```

**User Service:**
```java
// UserProfileService
@Cacheable(value = "profileByUserId", key = "#userId", sync = true,
    unless = "#result == null")
public UserProfileResponse getByUserId(String userId) { ... }

@Cacheable(value = "recommendations:combined",
    key = "#root.target.getCurrentProfileId() + '-' + #limit",
    sync = true, unless = "#result.isEmpty()")
public List<RecommendationResponse> getCombinedRecommendations(int limit) { ... }

@Cacheable(value = "recommendations:quick",
    key = "#root.target.getCurrentProfileId() + '-' + #limit",
    sync = true, unless = "#result.isEmpty()")
public List<RecommendationResponse> getQuickRecommendations(int limit) { ... }
```

> **Luu y:** `sync = true` chi hoat dong voi cac @Cacheable co DUY NHAT 1 cache name. Khong dung duoc voi @Caching.

---

### A7. Them Cache cho PostService (Hien tai KHONG co cache)

**Van de:** `PostService` la service duoc goi NHIEU NHAT nhung hoan toan KHONG co `@Cacheable`. Moi request deu query DB.

**File:** `content-service/.../post/service/PostService.java`

```java
// Them @Cacheable cho cac method read:

@Cacheable(value = "post", key = "#postId", sync = true, unless = "#result == null")
public PostResponse getPostById(String postId) {
    return postMapper.toPostResponse(postRepository.findById(postId)
        .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND)));
}

@Cacheable(value = "userPosts", key = "#userId", sync = true,
    unless = "#result == null || #result.isEmpty()")
public List<PostResponse> getPostsByUserId(String userId) {
    return postRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
        .stream().map(postMapper::toPostResponse)
        .collect(Collectors.toList());
}

@Cacheable(value = "userPosts", key = "#root.target.getCurrentUserId()",
    unless = "#result == null || #result.isEmpty()")
public List<PostResponse> getMyPosts() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String userId = authentication.getName();
    return postRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
        .stream().map(postMapper::toPostResponse)
        .collect(Collectors.toList());
}

@Cacheable(value = "postLikes", key = "#postId",
    unless = "#result == null || #result.isEmpty()")
public List<PostLike> getPostLikesByPostId(String postId) {
    return postRepository.findLikesByPostId(postId);
}

// Them eviction cho cac method write:

@Caching(evict = {
    @CacheEvict(value = "userPosts", key = "#root.target.getCurrentUserId()"),
    @CacheEvict(value = "posts", allEntries = true)
})
@Transactional
public PostResponse createPost(PostRequest postRequest) { ... }

@Caching(evict = {
    @CacheEvict(value = "post", key = "#postId"),
    @CacheEvict(value = "userPosts", key = "#root.target.getCurrentUserId()"),
    @CacheEvict(value = "posts", allEntries = true)
})
@Transactional
public PostResponse updatePost(String postId, PostRequest postRequest) { ... }

@Caching(evict = {
    @CacheEvict(value = "post", key = "#postId"),
    @CacheEvict(value = "userPosts", key = "#root.target.getCurrentUserId()"),
    @CacheEvict(value = "posts", allEntries = true),
    @CacheEvict(value = "postLikes", key = "#postId"),
    @CacheEvict(value = "savedPosts", allEntries = true)
})
@Transactional
public String deletePost(String postId) { ... }

@Caching(evict = {
    @CacheEvict(value = "postLikes", key = "#postId"),
    @CacheEvict(value = "post", key = "#postId")
})
@Transactional
public String likePost(String postId) { ... }

@Caching(evict = {
    @CacheEvict(value = "postLikes", key = "#postId"),
    @CacheEvict(value = "post", key = "#postId")
})
@Transactional
public String unlikePost(String postId) { ... }

// Them helper method:
public String getCurrentUserId() {
    return SecurityContextHolder.getContext().getAuthentication().getName();
}
```

---

### A8. Toi uu Eviction Strategy cho Follow/Unfollow

**Van de:** Moi lan follow/unfollow se `allEntries = true` cho 8 cache region. Tuc la XOA HET cache cua TAT CA user, gay cache miss hang loat.

**File:** `user-service/.../profile/service/UserProfileService.java`

**Truoc (XOA HET):**
```java
@Caching(evict = {
    @CacheEvict(value = "followers", allEntries = true),        // Xoa followers cua TAT CA user
    @CacheEvict(value = "following", allEntries = true),        // Xoa following cua TAT CA user
    @CacheEvict(value = "recommendations:mutual", allEntries = true),
    // ... 5 cache region nua
})
public String followUser(String followerId) { ... }
```

**Sau (CHI XOA CUA 2 USER LIEN QUAN):**
```java
@Caching(evict = {
    // Chi xoa cache cua 2 user lien quan
    @CacheEvict(value = "followers", key = "#root.target.getProfileIdByUserId(#followerId)"),
    @CacheEvict(value = "following", key = "#root.target.getCurrentProfileId()"),
    // Recommendations chi xoa cua current user
    @CacheEvict(value = "recommendations:mutual",
        key = "#root.target.getCurrentProfileId() + '-*'", condition = "false"),
    // Dung programmatic eviction thay vi allEntries:
})
public String followUser(String followerId) {
    // ... logic hien tai ...

    // Programmatic eviction cho recommendations (chi cua current user)
    evictCurrentUserRecommendations();

    return "You are following " + followingUser.getFirstName();
}

// Helper method:
private void evictCurrentUserRecommendations() {
    String prefix = getCurrentProfileId();
    String[] recCaches = {
        "recommendations:mutual", "recommendations:taste",
        "recommendations:city", "recommendations:popular",
        "recommendations:combined", "recommendations:quick"
    };
    for (String cacheName : recCaches) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();  // Tam thoi clear, se toi uu them voi key-based eviction
        }
    }
}
```

> **Luu y:** Voi cau truc cache key hien tai (`profileId-page-size`), viec evict theo key chinh xac can biet tat ca combination cua page/size. Giai phap tot hon la dung **cache tag** (se lam o Phan B voi Pub/Sub).

---

### A9. Tang Connection Pool va Expose Metrics

**File:** `content-service/src/main/resources/application.yaml`
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      database: 1
      timeout: ${REDIS_TIME_OUT:60000}
      lettuce:
        pool:
          max-active: ${MAX_ACTIVE:16}     # Tang tu 8 -> 16
          max-idle: ${MAX_IDLE:8}
          min-idle: ${MIN_IDLE:2}           # Tang tu 0 -> 2 (luon giu san connection)
          max-wait: 3000ms                  # Timeout khi pool het connection
        shutdown-timeout: 200ms

# Expose cache metrics qua Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info,caches,metrics,prometheus
  endpoint:
    health:
      show-details: always
    caches:
      enabled: true
  metrics:
    tags:
      application: content-service
```

Lam tuong tu cho `user-service/application.yaml` va `communication-service/application.yaml`.

**Xem cache metrics:**
```bash
# Xem danh sach tat ca caches
GET /actuator/caches

# Xem cache hit/miss ratio
GET /actuator/metrics/cache.gets?tag=cache:posts&tag=result:hit
GET /actuator/metrics/cache.gets?tag=cache:posts&tag=result:miss

# Xem so luong cache puts
GET /actuator/metrics/cache.puts?tag=cache:posts
```

---

### A10. Toi uu Scheduled Story Cleanup

**Van de:** `StoryService.deleteOldStories()` chay moi gio va XOA 5 cache region `allEntries = true` ngay ca khi KHONG co story nao can xoa.

**File:** `content-service/.../story/service/StoryService.java`

**Truoc:**
```java
@Scheduled(fixedRate = 3600000)
@Caching(evict = {
    @CacheEvict(value = "stories", allEntries = true),
    @CacheEvict(value = "storyById", allEntries = true),
    @CacheEvict(value = "storiesByUser", allEntries = true),
    @CacheEvict(value = "myStories", allEntries = true),
    @CacheEvict(value = "storyLikes", allEntries = true)
})
public void deleteOldStories() {
    Instant twentyFourHoursAgo = Instant.now().minus(24, ChronoUnit.HOURS);
    List<Story> oldStories = storyRepository.findAllByCreatedAtBefore(twentyFourHoursAgo);
    if (!oldStories.isEmpty()) {
        storyRepository.deleteAll(oldStories);
    }
}
```

**Sau (chi evict khi thuc su xoa):**
```java
@Scheduled(fixedRate = 3600000)
public void deleteOldStories() {
    Instant twentyFourHoursAgo = Instant.now().minus(24, ChronoUnit.HOURS);
    List<Story> oldStories = storyRepository.findAllByCreatedAtBefore(twentyFourHoursAgo);
    if (!oldStories.isEmpty()) {
        log.info("Deleting {} expired stories", oldStories.size());
        storyRepository.deleteAll(oldStories);
        // Chi evict cache khi thuc su co story bi xoa
        evictAllStoryCaches();
    }
}

private void evictAllStoryCaches() {
    String[] storyCaches = {"stories", "storyById", "storiesByUser", "myStories", "storyLikes"};
    for (String cacheName : storyCaches) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
```

---

### A11. Fix Communication Service dung DB 0 trung voi User Service

**Van de hien tai:**
- User Service: DB 0 (token) + DB 2 (profile)
- Communication Service: DB 0 (default) - **TRUNG voi User Service!**
- Content Service: DB 1

**Giai phap:** Chuyen Communication Service sang DB 3.

**File:** `communication-service/src/main/resources/application.yaml`
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      database: 3                          # CHUYEN TU 0 -> 3
      timeout: ${REDIS_TIME_OUT:60000}
```

---

## PHAN B: TINH NANG NANG CAO

### B1. Them Dependencies

**File:** `pom.xml` (cho moi service can cache nang cao)

```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
    <version>3.1.8</version>
</dependency>
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.25.0</version>
</dependency>
```

---

### B2. Multi-level Cache - Caffeine L1 + Redis L2

**Tai sao can:** Moi lan doc cache Redis = 1 network round-trip (~1-5ms). Voi Caffeine L1 (in-memory), hot data doc trong ~10ns (nhanh hon 100,000 lan).

**File:** `content-service/.../configuration/TwoLevelCacheManager.java`

```java
package com.contentservice.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.data.redis.cache.RedisCacheManager;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class TwoLevelCacheManager implements CacheManager {
    private final CacheManager redisCacheManager;
    private final Map<String, Cache> cacheMap = new ConcurrentHashMap<>();

    // Caffeine L1 config: nho, nhanh, TTL ngan
    private static final int L1_MAX_SIZE = 500;
    private static final Duration L1_TTL = Duration.ofSeconds(30);

    public TwoLevelCacheManager(RedisCacheManager redisCacheManager) {
        this.redisCacheManager = redisCacheManager;
    }

    @Override
    public Cache getCache(String name) {
        return cacheMap.computeIfAbsent(name, n -> {
            Cache redisCache = redisCacheManager.getCache(n);
            if (redisCache == null) return null;

            CaffeineCache caffeineCache = new CaffeineCache(n,
                Caffeine.newBuilder()
                    .maximumSize(L1_MAX_SIZE)
                    .expireAfterWrite(L1_TTL)
                    .recordStats()
                    .build());

            return new TwoLevelCache(n, caffeineCache, redisCache);
        });
    }

    @Override
    public Collection<String> getCacheNames() {
        return redisCacheManager.getCacheNames();
    }
}
```

**File:** `content-service/.../configuration/TwoLevelCache.java`

```java
package com.contentservice.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;

import java.util.concurrent.Callable;

@Slf4j
public class TwoLevelCache implements Cache {
    private final String name;
    private final Cache l1Cache;  // Caffeine (in-memory)
    private final Cache l2Cache;  // Redis (distributed)

    public TwoLevelCache(String name, Cache l1Cache, Cache l2Cache) {
        this.name = name;
        this.l1Cache = l1Cache;
        this.l2Cache = l2Cache;
    }

    @Override
    public String getName() { return name; }

    @Override
    public Object getNativeCache() { return l2Cache.getNativeCache(); }

    @Override
    public ValueWrapper get(Object key) {
        // 1. Check L1 (Caffeine) truoc
        ValueWrapper l1Value = l1Cache.get(key);
        if (l1Value != null) {
            log.trace("L1 cache HIT: {}::{}", name, key);
            return l1Value;
        }

        // 2. Check L2 (Redis)
        ValueWrapper l2Value = l2Cache.get(key);
        if (l2Value != null) {
            log.trace("L2 cache HIT: {}::{}", name, key);
            // Promote to L1
            l1Cache.put(key, l2Value.get());
            return l2Value;
        }

        log.trace("Cache MISS: {}::{}", name, key);
        return null;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        T l1Value = l1Cache.get(key, type);
        if (l1Value != null) return l1Value;

        T l2Value = l2Cache.get(key, type);
        if (l2Value != null) {
            l1Cache.put(key, l2Value);
            return l2Value;
        }
        return null;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper l1Value = l1Cache.get(key);
        if (l1Value != null) {
            @SuppressWarnings("unchecked")
            T result = (T) l1Value.get();
            return result;
        }

        // Delegate to L2 voi valueLoader (Redis se load neu miss)
        T value = l2Cache.get(key, valueLoader);
        if (value != null) {
            l1Cache.put(key, value);
        }
        return value;
    }

    @Override
    public void put(Object key, Object value) {
        l2Cache.put(key, value);  // Ghi Redis truoc
        l1Cache.put(key, value);  // Roi ghi local
    }

    @Override
    public void evict(Object key) {
        l2Cache.evict(key);  // Evict Redis truoc
        l1Cache.evict(key);  // Roi evict local
    }

    @Override
    public void clear() {
        l2Cache.clear();
        l1Cache.clear();
    }
}
```

**Cap nhat RedisConfig de dung TwoLevelCacheManager:**

```java
// Trong RedisConfig.java, doi cacheManager() method:

@Bean
public RedisCacheManager redisCacheManager(
        RedisConnectionFactory connectionFactory,
        @Qualifier("redisObjectMapper") ObjectMapper redisObjectMapper) {
    // ... giu nguyen code cu tao RedisCacheManager ...
    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(defaultConfig)
        .withInitialCacheConfigurations(cacheConfigurations)
        .transactionAware()
        .build();
}

@Bean
@Primary
public CacheManager cacheManager(RedisCacheManager redisCacheManager) {
    return new TwoLevelCacheManager(redisCacheManager);
}
```

---

### B3. Distributed Lock (Ngan Cache Stampede cho manual cache)

**Tai sao can:** `sync = true` chi hoat dong voi `@Cacheable`. Voi manual RedisTemplate operations (CacheService, RedisCacheService), can distributed lock.

**File:** `content-service/.../configuration/DistributedCacheService.java`

```java
package com.contentservice.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedCacheService {
    private final RedissonClient redissonClient;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Get with distributed lock.
     * Neu cache miss -> chi 1 thread acquire lock -> load data -> put cache.
     * Cac thread khac doi lock release -> doc tu cache.
     */
    public <T> T getWithLock(String cacheKey, Duration ttl, Supplier<T> dataLoader) {
        // 1. Check cache
        @SuppressWarnings("unchecked")
        T cached = (T) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) return cached;

        // 2. Cache miss -> acquire lock
        String lockKey = "lock:" + cacheKey;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (acquired) {
                try {
                    // Double-check: thread khac co the da load xong
                    @SuppressWarnings("unchecked")
                    T doubleCheck = (T) redisTemplate.opsForValue().get(cacheKey);
                    if (doubleCheck != null) return doubleCheck;

                    // Load data
                    T data = dataLoader.get();
                    if (data != null) {
                        redisTemplate.opsForValue().set(cacheKey, data, ttl);
                    }
                    return data;
                } finally {
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            } else {
                // Lock timeout -> load truc tiep (fallback)
                log.warn("Lock timeout for key={}, loading directly", cacheKey);
                return dataLoader.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return dataLoader.get();
        }
    }
}
```

**Vi du su dung:**
```java
// Trong PostService, thay vi query DB truc tiep:
public Page<PostResponse> getAllPots(int page, int limit) {
    String cacheKey = "content-service:posts::page-" + page + "-" + limit;
    return distributedCacheService.getWithLock(
        cacheKey,
        Duration.ofMinutes(2),
        () -> {
            // Logic load tu DB
            Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
            Page<Post> postPage = postRepository.findAll(pageable);
            // ... map to response ...
            return new PageImpl<>(responses, pageable, postPage.getTotalElements());
        }
    );
}
```

---

### B4. Pub/Sub Cache Invalidation Bus

**Tai sao can:** Khi chay nhieu instances cua 1 service, L1 cache (Caffeine) cua instance A khong biet khi instance B evict cache. Can Pub/Sub de dong bo.

**File:** `content-service/.../configuration/CacheInvalidationPublisher.java`

```java
package com.contentservice.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheInvalidationPublisher {
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CHANNEL = "cache-invalidation:content-service";

    public void publishInvalidation(String cacheName, String key) {
        try {
            String message = cacheName + "::" + (key != null ? key : "*");
            redisTemplate.convertAndSend(CHANNEL, message);
            log.debug("Published cache invalidation: {}", message);
        } catch (Exception e) {
            log.warn("Failed to publish cache invalidation: {}", e.getMessage());
        }
    }

    public void publishClearAll(String cacheName) {
        publishInvalidation(cacheName, "*");
    }
}
```

**File:** `content-service/.../configuration/CacheInvalidationSubscriber.java`

```java
package com.contentservice.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.Cache;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidationSubscriber implements MessageListener {
    private final CacheManager cacheManager;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String payload = new String(message.getBody());
            String[] parts = payload.split("::", 2);
            if (parts.length == 2) {
                String cacheName = parts[0];
                String key = parts[1];
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    if ("*".equals(key)) {
                        cache.clear();
                        log.debug("L1 cache cleared via pub/sub: {}", cacheName);
                    } else {
                        cache.evict(key);
                        log.debug("L1 cache evicted via pub/sub: {}::{}", cacheName, key);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to process cache invalidation message: {}", e.getMessage());
        }
    }
}
```

**Dang ky listener trong RedisConfig:**
```java
@Bean
public RedisMessageListenerContainer redisMessageListenerContainer(
        RedisConnectionFactory connectionFactory,
        CacheInvalidationSubscriber subscriber) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.addMessageListener(subscriber,
        new ChannelTopic("cache-invalidation:content-service"));
    return container;
}
```

---

### B5. Lua Script Atomic Counter

**Tai sao can:** Like count, view count can atomic increment. `INCR` + `EXPIRE` la 2 lenh rieng, co the race condition.

**File:** `content-service/.../configuration/AtomicCounterService.java`

```java
package com.contentservice.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class AtomicCounterService {
    private final RedisTemplate<String, Object> redisTemplate;

    // Lua script: INCR + EXPIRE atomic
    private static final String INCREMENT_SCRIPT = """
        local current = redis.call('GET', KEYS[1])
        if current == false then
            redis.call('SET', KEYS[1], 1)
            redis.call('EXPIRE', KEYS[1], ARGV[1])
            return 1
        else
            return redis.call('INCR', KEYS[1])
        end
    """;

    // Lua script: DECR nhung khong xuong duoi 0
    private static final String DECREMENT_SCRIPT = """
        local current = redis.call('GET', KEYS[1])
        if current == false or tonumber(current) <= 0 then
            return 0
        else
            return redis.call('DECR', KEYS[1])
        end
    """;

    public Long incrementCounter(String key, long ttlSeconds) {
        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(INCREMENT_SCRIPT, Long.class);
            return redisTemplate.execute(script,
                Collections.singletonList(key),
                String.valueOf(ttlSeconds));
        } catch (Exception e) {
            log.warn("Failed to increment counter key={}: {}", key, e.getMessage());
            return 0L;
        }
    }

    public Long decrementCounter(String key) {
        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(DECREMENT_SCRIPT, Long.class);
            return redisTemplate.execute(script, Collections.singletonList(key));
        } catch (Exception e) {
            log.warn("Failed to decrement counter key={}: {}", key, e.getMessage());
            return 0L;
        }
    }

    public Long getCounter(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? Long.parseLong(value.toString()) : 0L;
        } catch (Exception e) {
            log.warn("Failed to get counter key={}: {}", key, e.getMessage());
            return 0L;
        }
    }
}
```

**Vi du su dung cho post views:**
```java
// Trong PostService:
public PostResponse getPostById(String postId) {
    // Increment view count (atomic, khong can lock)
    atomicCounterService.incrementCounter(
        "content-service:views:" + postId, 86400);  // 24h TTL

    return postMapper.toPostResponse(postRepository.findById(postId)
        .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND)));
}
```

---

### B6. Cache Warming on Startup

**Tai sao can:** Khi service restart, cache rong. Cac request dau tien se cham vi phai query DB. Pre-load hot data de giam cold-start.

**File:** `content-service/.../configuration/CacheWarmupRunner.java`

```java
package com.contentservice.configuration;

import com.contentservice.post.repository.PostRepository;
import com.contentservice.post.service.PostService;
import com.contentservice.story.service.StoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheWarmupRunner implements ApplicationRunner {
    private final PostService postService;
    private final StoryService storyService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting cache warmup...");
        long start = System.currentTimeMillis();

        try {
            // Warm up feed page 1 (most accessed)
            postService.getAllPots(1, 20);
            log.info("Warmed up: posts page 1");
        } catch (Exception e) {
            log.warn("Failed to warm up posts: {}", e.getMessage());
        }

        try {
            // Warm up all stories (story feed)
            storyService.getAllStories();
            log.info("Warmed up: all stories");
        } catch (Exception e) {
            log.warn("Failed to warm up stories: {}", e.getMessage());
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("Cache warmup completed in {}ms", elapsed);
    }
}
```

---

## TONG KET THAY DOI

### Bang tong hop van de va giai phap

| # | Van de | Muc do | Giai phap | Service |
|---|--------|--------|-----------|---------|
| A1 | LaissezFaireSubTypeValidator | **CAO** | Doi sang BasicPolymorphicTypeValidator | content, user |
| A2 | Dung `KEYS *` (block Redis) | **CAO** | Doi sang `SCAN` + `UNLINK` | content, comm |
| A3 | Silent catch blocks | **TRUNG BINH** | Them `log.warn()` | tat ca |
| A4 | Sai chinh ta cache name | **CAO** | Fix typo + tao CacheNames constants | user |
| A5 | Khong co CacheErrorHandler | **TRUNG BINH** | Them CacheErrorConfig | content, comm |
| A6 | Cache Stampede | **TRUNG BINH** | Them `sync = true` | content, user |
| A7 | PostService khong co cache | **CAO** | Them @Cacheable/@CacheEvict | content |
| A8 | Follow evict tat ca cache | **TRUNG BINH** | Targeted eviction | user |
| A9 | Pool min-idle = 0 | **THAP** | Tang min-idle, expose metrics | tat ca |
| A10 | Scheduled evict vo dieu kien | **THAP** | Chi evict khi co data bi xoa | content |
| A11 | Comm service trung DB 0 | **TRUNG BINH** | Chuyen sang DB 3 | comm |
| B1-B6 | Tinh nang nang cao | Enhancement | Multi-level, Lock, Pub/Sub, Lua, Warmup | content |

### Thu tu thuc hien

**Uu tien 1 (Fix truoc):** A1, A2, A3, A4, A7
**Uu tien 2 (Toi uu):** A5, A6, A8, A9, A10, A11
**Uu tien 3 (Nang cao):** B1 -> B2 -> B3 -> B4 -> B5 -> B6

---

## Checklist

### Phan A - Fix van de hien tai
- [ ] A1: Thay LaissezFaireSubTypeValidator -> BasicPolymorphicTypeValidator (content-service, user-service)
- [ ] A2: Thay KEYS -> SCAN trong CacheService, RedisCacheService, RedisConfig startup
- [ ] A3: Them log.warn() cho tat ca catch blocks (content, communication, user service)
- [ ] A4: Fix typo cache names (recommnedations -> recommendations) + tao CacheNames constants
- [ ] A5: Them CacheErrorHandler cho content-service va communication-service
- [ ] A6: Them sync=true cho cac @Cacheable hot (posts, profiles, recommendations)
- [ ] A7: Them @Cacheable/@CacheEvict cho PostService
- [ ] A8: Toi uu follow/unfollow eviction (targeted thay vi allEntries)
- [ ] A9: Tang connection pool min-idle, expose cache metrics qua Actuator
- [ ] A10: Chi evict story caches khi thuc su xoa stories
- [ ] A11: Chuyen communication-service Redis tu DB 0 sang DB 3

### Phan B - Tinh nang nang cao
- [ ] B1: Them Caffeine + Redisson dependencies vao pom.xml
- [ ] B2: Tao TwoLevelCacheManager (Caffeine L1 + Redis L2)
- [ ] B3: Tao DistributedCacheService voi Redisson distributed lock
- [ ] B4: Tao CacheInvalidationPublisher + Subscriber (Pub/Sub)
- [ ] B5: Tao AtomicCounterService voi Lua scripts
- [ ] B6: Tao CacheWarmupRunner cho startup pre-loading
- [ ] B7: Cau hinh Redisson trong application.yaml

### Testing
- [ ] Test: Redis down -> service van hoat dong (graceful degradation)
- [ ] Test: Cache hit ratio > 80% (xem qua /actuator/metrics)
- [ ] Test: Khong con KEYS command trong production (dung redis MONITOR de verify)
- [ ] Test: Multiple instances invalidation dong bo (Pub/Sub)
- [ ] Test: Cache stampede khong xay ra khi concurrent requests
- [ ] Test: Deserialization chi cho phep class cua project (security)
