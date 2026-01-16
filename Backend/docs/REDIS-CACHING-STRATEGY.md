# 🚀 Chiến Lược Redis Cache Cho Blur Platform

> **Phiên bản:** 1.0 | **Ngày tạo:** 2026-01-16
> **Tác giả:** Senior Software Architect

---

## 📋 Mục Lục

1. [Phân Tích Hệ Thống](#1-phân-tích-hệ-thống)
2. [Kiến Trúc Redis Cache](#2-kiến-trúc-redis-cache)
3. [Dữ Liệu Cần Cache](#3-dữ-liệu-cần-cache)
4. [Chiến Lược Invalidation](#4-chiến-lược-invalidation)
5. [Hướng Dẫn Triển Khai Spring Boot](#5-hướng-dẫn-triển-khai-spring-boot)
6. [Tối Ưu Frontend React](#6-tối-ưu-frontend-react)
7. [Microservice & Distributed Cache](#7-microservice--distributed-cache)
8. [Bảo Mật & Hiệu Năng](#8-bảo-mật--hiệu-năng)
9. [Checklist Triển Khai](#9-checklist-triển-khai)

---

## 1. Phân Tích Hệ Thống

### 1.1 Kiến Trúc Hiện Tại

```
┌─────────────────────────────────────────────────────────────────┐
│                         API Gateway (8888)                       │
└─────────────────────────────────────────────────────────────────┘
                                   │
        ┌──────────────────────────┼──────────────────────────┐
        │                          │                          │
        ▼                          ▼                          ▼
┌───────────────┐      ┌───────────────────┐      ┌──────────────────┐
│ Identity (8080)│      │ Profile (8081)    │      │ Chat (8083/8099) │
│ MySQL + Redis │      │ Neo4j + Redis     │      │ MongoDB + Redis  │
└───────────────┘      └───────────────────┘      └──────────────────┘
        │                          │                          │
        ▼                          ▼                          ▼
┌───────────────┐      ┌───────────────────┐      ┌──────────────────┐
│ Post (8084)   │      │ Story (8086)      │      │ Notification     │
│ MongoDB+Redis │      │ MongoDB + Redis   │      │ (8082) MongoDB   │
└───────────────┘      └───────────────────┘      └──────────────────┘
```

### 1.2 Đánh Giá Hiệu Năng Hiện Tại

| Vấn Đề | Mức Độ | Service | Mô Tả |
|--------|--------|---------|-------|
| **N+1 Query** | 🔴 Cao | post-service | `getAllPosts()` gọi `profileClient.getProfile()` cho mỗi post |
| **N+1 Query** | 🔴 Cao | chat-service | `myConversations()` gọi `getLastMessageCached()` cho mỗi conversation |
| **Over-fetching** | 🟡 Trung bình | Frontend | Gọi lại API khi navigate giữa các trang |
| **Không có cache** | 🔴 Cao | post-service | `PostService` không có annotation `@Cacheable` |
| **Cache disabled** | 🟡 Trung bình | chat-service | Comment ghi "Redis caching disabled" do serialization errors |
| **Expensive queries** | 🟡 Trung bình | profile-service | Neo4j graph queries cho followers/following |

### 1.3 API Traffic Analysis

| Endpoint | QPS Ước Tính | Read/Write | Nên Cache |
|----------|--------------|------------|-----------|
| `GET /post/all` | 🔴 Rất cao | Read | ✅ **Bắt buộc** |
| `GET /profile/users/{userId}` | 🔴 Rất cao | Read | ✅ **Bắt buộc** |
| `GET /chat/conversations/my-conversations` | 🔴 Cao | Read | ✅ **Bắt buộc** |
| `GET /story/all` | 🟡 Trung bình | Read | ✅ Nên có |
| `GET /profile/my-profile` | 🟡 Trung bình | Read | ✅ **Đã có** |
| `GET /post/comment/{postId}/comments` | 🟡 Trung bình | Read | ✅ **Đã có** |
| `POST /chat/messages/create` | 🟡 Cao | Write | ❌ Không |

---

## 2. Kiến Trúc Redis Cache

### 2.1 Cache Patterns Đề Xuất

```
┌────────────────────────────────────────────────────────────────┐
│                      CACHE PATTERNS                             │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────┐     ┌─────────────────┐                   │
│  │   CACHE-ASIDE   │     │  READ-THROUGH   │                   │
│  │                 │     │                 │                   │
│  │  • User Profile │     │  • Config       │                   │
│  │  • Posts Feed   │     │  • Permissions  │                   │
│  │  • Comments     │     │  • Roles        │                   │
│  └─────────────────┘     └─────────────────┘                   │
│                                                                 │
│  ┌─────────────────┐     ┌─────────────────┐                   │
│  │  WRITE-THROUGH  │     │  WRITE-BEHIND   │                   │
│  │                 │     │                 │                   │
│  │  • User Status  │     │  • View Count   │                   │
│  │  • Call State   │     │  • Like Count   │                   │
│  │  • Session      │     │  • Analytics    │                   │
│  └─────────────────┘     └─────────────────┘                   │
│                                                                 │
└────────────────────────────────────────────────────────────────┘
```

### 2.2 Redis Data Structures

| Loại Dữ Liệu | Redis Structure | Lý Do |
|--------------|-----------------|-------|
| User Profile | **Hash** | Field-level access, partial update |
| User Session | **String** | Simple key-value |
| Posts Feed | **Sorted Set** | Pagination by timestamp |
| Followers/Following | **Set** | Unique members, set operations |
| Unread Count | **String (Counter)** | Atomic increment/decrement |
| Online Users | **Set** | Membership check |
| Rate Limiting | **String + EXPIRE** | Time-based expiry |
| Conversation Messages | **List** | Recent messages, LPUSH/RPOP |

### 2.3 Key Naming Convention

```
{service}:{entity}:{identifier}:{sub-field}
```

| Service | Key Pattern | Ví Dụ |
|---------|-------------|-------|
| identity | `identity:user:{userId}` | `identity:user:abc123` |
| identity | `identity:roles:{userId}` | `identity:roles:abc123` |
| profile | `profile:user:{userId}` | `profile:user:abc123` |
| profile | `profile:followers:{profileId}` | `profile:followers:xyz789` |
| profile | `profile:search:{query}` | `profile:search:john` |
| chat | `chat:conversation:{convId}` | `chat:conversation:conv123` |
| chat | `chat:unread:{convId}:{userId}` | `chat:unread:conv123:user456` |
| chat | `chat:session:{sessionId}` | `chat:session:sess789` |
| post | `post:feed:page:{page}` | `post:feed:page:1` |
| post | `post:detail:{postId}` | `post:detail:post123` |
| post | `post:comments:{postId}` | `post:comments:post123` |
| post | `post:likes:{postId}` | `post:likes:post123` |
| story | `story:all` | `story:all` |
| story | `story:user:{userId}` | `story:user:abc123` |

### 2.4 TTL Strategy

| Loại Dữ Liệu | TTL | Lý Do |
|--------------|-----|-------|
| **User Session** | 2 giờ | Security + memory |
| **User Profile** | 15 phút | Ít thay đổi |
| **My Profile** | 10 phút | User thấy thay đổi nhanh |
| **Posts Feed** | 5 phút | Fresh content |
| **Post Detail** | 30 phút | Ít thay đổi |
| **Comments** | 10 phút | Moderate changes |
| **Stories** | 5 phút | Expiry every 24h |
| **Search Results** | 5 phút | Dynamic data |
| **Followers/Following** | 30 phút | Social graph stable |
| **Unread Count** | 30 phút | Real-time fallback |
| **Online Status** | 5 phút | Heartbeat refresh |
| **Roles/Permissions** | 1 giờ | Admin changes rare |

---

## 3. Dữ Liệu Cần Cache

### 3.1 Cache Matrix

| Loại Dữ Liệu | Cache Level | Redis Key Pattern | TTL | Invalidation Strategy |
|--------------|-------------|-------------------|-----|----------------------|
| User Profile | L1 (Service) | `profile:user:{userId}` | 15m | `@CacheEvict` on update |
| My Profile | L1 | `profile:my:{userId}` | 10m | `@CachePut` on update |
| User Roles/Permissions | L1 | `identity:roles:{userId}` | 60m | Event-driven (Kafka) |
| Posts Feed (Page) | L1 | `post:feed:page:{n}` | 5m | `@CacheEvict(allEntries=true)` on create/delete |
| Post Detail | L1 | `post:detail:{postId}` | 30m | `@CacheEvict` on update/delete |
| Comments | L1 | `post:comments:{postId}` | 10m | `@CacheEvict` on CRUD |
| Stories | L1 | `story:all`, `story:user:{id}` | 5m | `@CacheEvict` + Scheduled cleanup |
| Followers | L1 | `profile:followers:{profileId}` | 30m | `@CacheEvict` on follow/unfollow |
| Following | L1 | `profile:following:{profileId}` | 30m | `@CacheEvict` on follow/unfollow |
| Conversations | L1 | `chat:conversations:{userId}` | 15m | Evict on new message |
| Unread Count | L1 | `chat:unread:{convId}:{userId}` | 30m | Atomic decrement on read |
| Search Results | L2 | `profile:search:{query}` | 5m | Time-based expiry |
| User Online Status | L1 | `chat:status:{userId}` | 5m | Socket heartbeat |
| Call State | L1 | `chat:call:state:{callId}` | 10m | Event-driven |

### 3.2 Ưu Tiên Triển Khai

**Phase 1 - Critical (Tuần 1-2):**
- ✅ Đã có: Profile caching, Comments caching, Stories caching
- 🔴 Cần làm: **Posts Feed caching** (cao nhất)
- 🔴 Cần làm: **Fix N+1 trong getAllPosts()**

**Phase 2 - High Priority (Tuần 3-4):**
- 🟡 Conversations list caching
- 🟡 User online status
- 🟡 Fix conversation lastMessage serialization

**Phase 3 - Optimization (Tuần 5-6):**
- 🟢 Rate limiting với Redis
- 🟢 Distributed session management
- 🟢 Cache warming strategies

---

## 4. Chiến Lược Invalidation

### 4.1 Annotation-Based Invalidation

```java
// ✅ ĐÃ TRIỂN KHAI - UserProfileService
@Caching(evict = {
    @CacheEvict(value = "profiles", key = "#userProfileId"),
    @CacheEvict(value = "profileByUserId", key = "#root.target.getUserIdByProfileId(#userProfileId)"),
    @CacheEvict(value = "myProfile", key = "#root.target.getUserIdByProfileId(#userProfileId)"),
    @CacheEvict(value = "searchResults", allEntries = true)
})
public UserProfile updateUserProfile(String userProfileId, UserProfileUpdateRequest request) {
    // ...
}
```

### 4.2 Event-Driven Invalidation (Kafka)

```java
// notification-service đã có Kafka consumer
// Mở rộng để xử lý cache invalidation

@Component
@RequiredArgsConstructor
public class CacheInvalidationHandler {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    @KafkaListener(topics = "profile-updated")
    public void handleProfileUpdated(ProfileUpdatedEvent event) {
        String userId = event.getUserId();
        
        // Invalidate across all services
        redisTemplate.delete(List.of(
            "profile:user:" + userId,
            "profile:my:" + userId
        ));
        
        log.info("Cache invalidated for user: {}", userId);
    }
    
    @KafkaListener(topics = "post-created")
    public void handlePostCreated(PostCreatedEvent event) {
        // Invalidate feed cache
        Set<String> keys = redisTemplate.keys("post:feed:page:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
```

### 4.3 Redis Pub/Sub (Multi-Instance)

```java
@Component
@RequiredArgsConstructor
public class RedisPubSubConfig {
    
    @Bean
    public RedisMessageListenerContainer container(RedisConnectionFactory factory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(
            cacheInvalidationListener(), 
            new ChannelTopic("cache:invalidation")
        );
        return container;
    }
    
    @Bean
    public MessageListener cacheInvalidationListener() {
        return (message, pattern) -> {
            String key = new String(message.getBody());
            localCache.invalidate(key); // L1 local cache
            log.info("Local cache invalidated: {}", key);
        };
    }
}
```

### 4.4 Tránh Stale Data

| Vấn Đề | Giải Pháp |
|--------|-----------|
| Race condition | Sử dụng Redis WATCH/MULTI/EXEC |
| Partial update fail | Transaction với `@Transactional` + `@CacheEvict` |
| Cross-service stale | Kafka events với idempotent consumers |
| Read-your-writes | `@CachePut` thay vì `@CacheEvict` cho update |

---

## 5. Hướng Dẫn Triển Khai Spring Boot

### 5.1 Dependencies (đã có trong pom.xml)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

### 5.2 Redis Configuration (Cải Tiến)

```java
@Slf4j
@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        CustomRedisSerializer serializer = new CustomRedisSerializer(createObjectMapper());

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(15))
            .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(SerializationPair.fromSerializer(serializer))
            .disableCachingNullValues();

        return RedisCacheManager.builder(factory)
            .cacheDefaults(defaultConfig)
            // Identity Service
            .withCacheConfiguration("users", defaultConfig.entryTtl(Duration.ofMinutes(30)))
            .withCacheConfiguration("userById", defaultConfig.entryTtl(Duration.ofMinutes(15)))
            .withCacheConfiguration("myInfo", defaultConfig.entryTtl(Duration.ofMinutes(10)))
            // Profile Service
            .withCacheConfiguration("profiles", defaultConfig.entryTtl(Duration.ofMinutes(15)))
            .withCacheConfiguration("profileByUserId", defaultConfig.entryTtl(Duration.ofMinutes(15)))
            .withCacheConfiguration("followers", defaultConfig.entryTtl(Duration.ofMinutes(30)))
            .withCacheConfiguration("following", defaultConfig.entryTtl(Duration.ofMinutes(30)))
            .withCacheConfiguration("searchResults", defaultConfig.entryTtl(Duration.ofMinutes(5)))
            // Post Service - CẦN THÊM
            .withCacheConfiguration("posts", defaultConfig.entryTtl(Duration.ofMinutes(5)))
            .withCacheConfiguration("postById", defaultConfig.entryTtl(Duration.ofMinutes(30)))
            .withCacheConfiguration("comments", defaultConfig.entryTtl(Duration.ofMinutes(10)))
            // Story Service
            .withCacheConfiguration("stories", defaultConfig.entryTtl(Duration.ofMinutes(5)))
            .withCacheConfiguration("storyById", defaultConfig.entryTtl(Duration.ofMinutes(30)))
            // Chat Service
            .withCacheConfiguration("conversations", defaultConfig.entryTtl(Duration.ofMinutes(15)))
            .transactionAware()
            .build();
    }
}
```

### 5.3 PostService Cải Tiến (CẦN TRIỂN KHAI)

```java
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PostService {
    
    PostRepository postRepository;
    PostMapper postMapper;
    ProfileClient profileClient;
    // Thêm cache cho profile lookup
    private final ConcurrentHashMap<String, UserProfileResponse> profileCache = new ConcurrentHashMap<>();

    /**
     * ✅ CẢI TIẾN: Cache posts feed + batch profile lookup
     */
    @Cacheable(
        value = "posts",
        key = "'page:' + #page + ':limit:' + #limit",
        unless = "#result == null || #result.isEmpty()"
    )
    public Page<PostResponse> getAllPosts(int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by("createdAt").descending());
        Page<Post> postPage = postRepository.findAllByOrderByCreatedAtDesc(pageable);

        // ✅ FIX N+1: Batch load all profiles first
        Set<String> userIds = postPage.getContent().stream()
            .map(Post::getUserId)
            .collect(Collectors.toSet());
        
        Map<String, UserProfileResponse> profiles = batchLoadProfiles(userIds);

        List<PostResponse> responses = postPage.getContent().stream()
            .map(post -> buildPostResponse(post, profiles.get(post.getUserId())))
            .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, postPage.getTotalElements());
    }
    
    private Map<String, UserProfileResponse> batchLoadProfiles(Set<String> userIds) {
        Map<String, UserProfileResponse> result = new HashMap<>();
        
        for (String userId : userIds) {
            try {
                // Check local cache first
                UserProfileResponse cached = profileCache.get(userId);
                if (cached != null) {
                    result.put(userId, cached);
                    continue;
                }
                
                // Call service and cache
                var response = profileClient.getProfile(userId);
                if (response != null && response.getResult() != null) {
                    result.put(userId, response.getResult());
                    profileCache.put(userId, response.getResult());
                }
            } catch (Exception e) {
                log.warn("Failed to load profile for user: {}", userId);
            }
        }
        
        return result;
    }

    @CacheEvict(value = "posts", allEntries = true)
    @Transactional
    public PostResponse createPost(PostRequest request) {
        // existing code...
    }

    @Caching(evict = {
        @CacheEvict(value = "posts", allEntries = true),
        @CacheEvict(value = "postById", key = "#postId")
    })
    @Transactional
    public String deletePost(String postId) {
        // existing code...
    }
}
```

### 5.4 Conditional Caching

```java
// Chỉ cache khi có kết quả và user đã verify
@Cacheable(
    value = "myProfile",
    key = "#root.target.getCurrentUserId()",
    condition = "#root.target.isUserVerified()",
    unless = "#result == null"
)
public UserProfileResponse myProfile() {
    // ...
}

// Cache với TTL động
@Cacheable(
    value = "posts",
    key = "'trending'",
    cacheManager = "shortTtlCacheManager" // 1 phút cho trending
)
public List<PostResponse> getTrendingPosts() {
    // ...
}
```

### 5.5 Serialization Strategy (Đã Triển Khai)

```java
// ✅ Đã có trong RedisConfig - sử dụng Jackson với type info
private ObjectMapper createRedisObjectMapper() {
    ObjectMapper mapper = JsonMapper.builder()
        .addModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .build();

    mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
    mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    
    // ✅ IMPORTANT: Type info for polymorphic deserialization
    mapper.activateDefaultTyping(
        LaissezFaireSubTypeValidator.instance,
        ObjectMapper.DefaultTyping.NON_FINAL,
        JsonTypeInfo.As.PROPERTY
    );

    return mapper;
}
```

---

## 6. Tối Ưu Frontend React

### 6.1 React Query Integration

```typescript
// src/hooks/usePosts.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { fetchAllPost, createPost, likePost } from '../api/postApi';

const STALE_TIME = 5 * 60 * 1000; // 5 phút - match backend cache TTL
const CACHE_TIME = 30 * 60 * 1000; // 30 phút

export const usePosts = (page: number = 1, limit: number = 10) => {
    return useQuery({
        queryKey: ['posts', page, limit],
        queryFn: () => fetchAllPost(page, limit),
        staleTime: STALE_TIME,
        gcTime: CACHE_TIME,
        placeholderData: (previousData) => previousData, // Keep previous while fetching
    });
};

export const useCreatePost = () => {
    const queryClient = useQueryClient();
    
    return useMutation({
        mutationFn: createPost,
        onSuccess: () => {
            // Invalidate all posts pages
            queryClient.invalidateQueries({ queryKey: ['posts'] });
        },
        // Optimistic update
        onMutate: async (newPost) => {
            await queryClient.cancelQueries({ queryKey: ['posts'] });
            const previousPosts = queryClient.getQueryData(['posts', 1, 10]);
            
            queryClient.setQueryData(['posts', 1, 10], (old: any) => ({
                ...old,
                posts: [{ ...newPost, id: 'temp-id' }, ...(old?.posts || [])],
            }));
            
            return { previousPosts };
        },
        onError: (err, newPost, context) => {
            queryClient.setQueryData(['posts', 1, 10], context?.previousPosts);
        },
    });
};

export const useLikePost = () => {
    const queryClient = useQueryClient();
    
    return useMutation({
        mutationFn: ({ postId, isLiked }: { postId: string; isLiked: boolean }) => 
            likePost(postId),
        onMutate: async ({ postId, isLiked }) => {
            // Optimistic update
            queryClient.setQueriesData(
                { queryKey: ['posts'] },
                (old: any) => ({
                    ...old,
                    posts: old?.posts?.map((post: any) =>
                        post.id === postId
                            ? { ...post, isLiked: !isLiked, likeCount: post.likeCount + (isLiked ? -1 : 1) }
                            : post
                    ),
                })
            );
        },
    });
};
```

### 6.2 Giảm Redundant API Calls

```typescript
// src/contexts/UserContext.tsx
import { createContext, useContext } from 'react';
import { useQuery } from '@tanstack/react-query';

const UserContext = createContext<UserContextType | null>(null);

export const UserProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    // Cache user profile globally - không refetch khi navigate
    const { data: user, isLoading } = useQuery({
        queryKey: ['myProfile'],
        queryFn: getMyProfile,
        staleTime: 10 * 60 * 1000, // 10 phút
        gcTime: 60 * 60 * 1000, // 1 giờ
        retry: 1,
    });

    return (
        <UserContext.Provider value={{ user, isLoading }}>
            {children}
        </UserContext.Provider>
    );
};

// Usage - không gọi API nhiều lần
export const useUser = () => {
    const context = useContext(UserContext);
    if (!context) throw new Error('useUser must be used within UserProvider');
    return context;
};
```

### 6.3 Cache Busting Strategies

```typescript
// src/api/axiosClient.ts - cải tiến
import axios from 'axios';

const axiosClient = axios.create({
    baseURL: import.meta.env.VITE_API_URL,
});

// Add cache busting for critical updates
axiosClient.interceptors.response.use(
    (response) => {
        // Check for cache invalidation header from backend
        const cacheInvalidate = response.headers['x-cache-invalidate'];
        if (cacheInvalidate) {
            const keys = cacheInvalidate.split(',');
            keys.forEach(key => {
                queryClient.invalidateQueries({ queryKey: [key.trim()] });
            });
        }
        return response;
    }
);

// Prefetch on hover for better UX
export const prefetchPost = async (postId: string) => {
    await queryClient.prefetchQuery({
        queryKey: ['post', postId],
        queryFn: () => fetchPostById(postId),
        staleTime: 5 * 60 * 1000,
    });
};
```

---

## 7. Microservice & Distributed Cache

### 7.1 Cache Consistency Model

```
┌─────────────────────────────────────────────────────────────────┐
│                    CACHE CONSISTENCY STRATEGY                    │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────┐    ┌────────────┐    ┌────────────┐             │
│  │ Service A  │    │ Service B  │    │ Service C  │             │
│  │            │    │            │    │            │             │
│  │ ┌────────┐ │    │ ┌────────┐ │    │ ┌────────┐ │             │
│  │ │L1 Cache│ │    │ │L1 Cache│ │    │ │L1 Cache│ │             │
│  │ └────┬───┘ │    │ └────┬───┘ │    │ └────┬───┘ │             │
│  └──────┼─────┘    └──────┼─────┘    └──────┼─────┘             │
│         │                 │                 │                    │
│         ▼                 ▼                 ▼                    │
│  ┌──────────────────────────────────────────────────┐           │
│  │              REDIS CLUSTER (L2 Cache)            │           │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐          │           │
│  │  │ Master  │  │ Replica │  │ Replica │          │           │
│  │  └─────────┘  └─────────┘  └─────────┘          │           │
│  └──────────────────────────────────────────────────┘           │
│         │                                                        │
│         ▼                                                        │
│  ┌──────────────────────────────────────────────────┐           │
│  │              DATABASE (Source of Truth)          │           │
│  │  MongoDB │ MySQL │ Neo4j                         │           │
│  └──────────────────────────────────────────────────┘           │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 7.2 Shared vs Per-Service Cache

| Approach | Use Case | Pros | Cons |
|----------|----------|------|------|
| **Shared Redis** | User profiles, Sessions | Single source of truth | Coupling between services |
| **Per-Service Redis DB** | Service-specific data | Isolation | Data duplication |
| **Hybrid** ✅ Recommended | Mix of both | Best of both worlds | Complexity |

**Blur Implementation:**
```yaml
# Mỗi service dùng database riêng
identity-service:  database: 0
profile-service:   database: 2
post-service:      database: 1
chat-service:      database: 0 (shared với identity cho session)
```

### 7.3 Cache Stampede Prevention

```java
@Service
public class StampedeProtectedCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;

    /**
     * Cache với lock để tránh stampede
     */
    public <T> T getWithLock(String key, Supplier<T> loader, Duration ttl) {
        // 1. Try cache first
        T cached = (T) redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached;
        }

        // 2. Acquire distributed lock
        RLock lock = redissonClient.getLock("lock:" + key);
        try {
            if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
                // Double-check after acquiring lock
                cached = (T) redisTemplate.opsForValue().get(key);
                if (cached != null) {
                    return cached;
                }

                // 3. Load from source
                T value = loader.get();
                if (value != null) {
                    redisTemplate.opsForValue().set(key, value, ttl);
                }
                return value;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        // Fallback: load from source directly
        return loader.get();
    }
}
```

### 7.4 Circuit Breaker + Cache Fallback

```java
@Service
@RequiredArgsConstructor
public class ResilientProfileService {

    private final ProfileClient profileClient;
    private final RedisTemplate<String, Object> redisTemplate;

    @CircuitBreaker(name = "profileService", fallbackMethod = "getProfileFallback")
    @Cacheable(value = "profiles", key = "#userId")
    public UserProfileResponse getProfile(String userId) {
        return profileClient.getProfile(userId).getResult();
    }

    /**
     * Fallback: return cached data even if stale
     */
    public UserProfileResponse getProfileFallback(String userId, Exception ex) {
        log.warn("Profile service down, using cache fallback for user: {}", userId);
        
        // Try to get from Redis directly (even expired)
        String key = "profiles::" + userId;
        Object cached = redisTemplate.opsForValue().get(key);
        
        if (cached instanceof UserProfileResponse) {
            return (UserProfileResponse) cached;
        }
        
        // Return minimal response
        return UserProfileResponse.builder()
            .userId(userId)
            .firstName("Unknown")
            .lastName("User")
            .build();
    }
}
```

---

## 8. Bảo Mật & Hiệu Năng

### 8.1 Sensitive Data Rules

| Loại Dữ Liệu | Có Cache? | Lý Do |
|--------------|-----------|-------|
| Password hash | ❌ **KHÔNG BAO GIỜ** | Security risk |
| JWT Token | ❌ KHÔNG | Already in memory |
| Email (full) | ⚠️ Cẩn thận | Hash hoặc mask |
| Phone number | ❌ KHÔNG | PII data |
| User sessions | ✅ Có | Cần cho auth, có encryption |
| Roles/Permissions | ✅ Có | Public data |
| Profile (public) | ✅ Có | Public data |

### 8.2 Cache Security Configuration

```java
@Configuration
public class RedisSecurityConfig {

    @Bean
    public RedisConnectionFactory secureRedisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        config.setPassword(RedisPassword.of(redisPassword)); // ✅ Bắt buộc
        
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .useSsl() // ✅ Production: Enable TLS
            .commandTimeout(Duration.ofSeconds(5))
            .build();
            
        return new LettuceConnectionFactory(config, clientConfig);
    }
}
```

### 8.3 Cache Penetration Prevention

```java
/**
 * Bloom Filter để ngăn cache penetration
 * Khi query ID không tồn tại
 */
@Service
public class CachePenetrationGuard {

    private final BloomFilter<String> existingIds;
    
    @PostConstruct
    public void init() {
        // Load all existing IDs vào Bloom Filter
        existingIds = BloomFilter.create(
            Funnels.stringFunnel(StandardCharsets.UTF_8),
            10_000_000, // Expected insertions
            0.01 // False positive rate
        );
        
        // Warm up bloom filter
        postRepository.findAllIds().forEach(existingIds::put);
    }
    
    public PostResponse getPostSafe(String postId) {
        // Check bloom filter first
        if (!existingIds.mightContain(postId)) {
            return null; // Definitely not exists
        }
        
        // Proceed with cache/db lookup
        return postService.getPostById(postId);
    }
}
```

### 8.4 Rate Limiting với Redis

```java
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Sliding window rate limiting
     */
    public boolean isAllowed(String userId, String endpoint, int maxRequests, int windowSeconds) {
        String key = String.format("ratelimit:%s:%s", endpoint, userId);
        long now = System.currentTimeMillis();
        long windowStart = now - (windowSeconds * 1000L);

        // Lua script for atomic operation
        String script = """
            redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[1])
            local count = redis.call('ZCARD', KEYS[1])
            if count < tonumber(ARGV[2]) then
                redis.call('ZADD', KEYS[1], ARGV[3], ARGV[3])
                redis.call('EXPIRE', KEYS[1], ARGV[4])
                return 1
            end
            return 0
            """;

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        Long result = redisTemplate.execute(
            redisScript,
            List.of(key),
            String.valueOf(windowStart),
            String.valueOf(maxRequests),
            String.valueOf(now),
            String.valueOf(windowSeconds)
        );

        return result != null && result == 1L;
    }
}
```

### 8.5 Monitoring & Metrics

```java
@Configuration
public class RedisMetricsConfig {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> redisMetrics(RedisConnectionFactory factory) {
        return registry -> {
            // Cache hit/miss ratio
            CacheMetricsCollector cacheMetrics = new CacheMetricsCollector();
            cacheMetrics.register(registry);
            
            // Custom Redis metrics
            Gauge.builder("redis.connections.active", factory, cf -> {
                if (cf instanceof LettuceConnectionFactory lcf) {
                    return lcf.getConnection().getNativeConnection() != null ? 1 : 0;
                }
                return 0;
            }).register(registry);
        };
    }
}

// Prometheus metrics exposed at /actuator/prometheus
// redis_cache_hits_total
// redis_cache_misses_total
// redis_cache_evictions_total
// redis_command_latency_seconds
```

---

## 9. Checklist Triển Khai

### 9.1 Phase 1: Critical Fixes (Tuần 1-2)

- [ ] **[MANDATORY]** Fix N+1 trong `PostService.getAllPosts()`
  - [ ] Implement batch profile loading
  - [ ] Add `@Cacheable` annotation
  
- [ ] **[MANDATORY]** Enable cache trong `chat-service`
  - [ ] Fix serialization issues
  - [ ] Implement `ConversationService` caching
  
- [ ] **[MANDATORY]** Add monitoring
  - [ ] Enable Redis statistics
  - [ ] Add Prometheus metrics

### 9.2 Phase 2: Optimization (Tuần 3-4)

- [ ] Implement rate limiting
- [ ] Add cache warming strategy
- [ ] Implement Bloom filter for cache penetration
- [ ] Add circuit breaker with cache fallback

### 9.3 Phase 3: Advanced (Tuần 5-6)

- [ ] Implement Redis Pub/Sub for multi-instance sync
- [ ] Add L1 (Caffeine) + L2 (Redis) caching
- [ ] Implement cache compression
- [ ] Setup Redis Cluster for HA

### 9.4 Best Practices

**✅ NÊN LÀM:**
- Sử dụng TTL phù hợp với tần suất thay đổi dữ liệu
- Implement cache eviction khi write operations
- Log cache hit/miss để monitoring
- Sử dụng connection pooling
- Serialize DTOs thay vì Entities

**❌ KHÔNG NÊN:**
- Cache sensitive data (passwords, tokens)
- Cache quá lớn (>1MB per entry)
- Forget cache invalidation on updates
- Use cache without TTL
- Ignore serialization errors

### 9.5 Common Mistakes

| Lỗi | Hậu quả | Giải pháp |
|-----|---------|-----------|
| Cache entity thay vì DTO | Lazy loading fails, serialization errors | Sử dụng DTO |
| Không set TTL | Memory leak | Luôn set TTL |
| Cache NULL values | Logic errors | `unless = "#result == null"` |
| Không invalidate | Stale data | `@CacheEvict` on mutations |
| Serialize JDK classes | Version conflicts | Jackson with type info |

---

## 📊 Kiến Trúc Tổng Quan

```
┌──────────────────────────────────────────────────────────────────────┐
│                         BLUR REDIS CACHING ARCHITECTURE              │
├──────────────────────────────────────────────────────────────────────┤
│                                                                       │
│    ┌──────────────┐                                                  │
│    │   Frontend   │  React Query / SWR (L0 Cache)                   │
│    │    React     │  staleTime: 5min, localStorage                  │
│    └──────┬───────┘                                                  │
│           │                                                          │
│           ▼                                                          │
│    ┌──────────────┐                                                  │
│    │ API Gateway  │  Rate Limiting, Auth Cache                      │
│    │    8888      │                                                  │
│    └──────┬───────┘                                                  │
│           │                                                          │
│    ┌──────┴───────────────────────────────────────────┐             │
│    │                                                   │             │
│    ▼                   ▼                   ▼           ▼             │
│  ┌────────┐      ┌──────────┐      ┌──────────┐   ┌────────┐        │
│  │Identity│      │ Profile  │      │   Chat   │   │  Post  │        │
│  │ MySQL  │      │  Neo4j   │      │ MongoDB  │   │MongoDB │        │
│  └───┬────┘      └────┬─────┘      └────┬─────┘   └───┬────┘        │
│      │                │                 │              │             │
│      ▼                ▼                 ▼              ▼             │
│  ┌─────────────────────────────────────────────────────────┐        │
│  │                    REDIS CLUSTER                         │        │
│  │  ┌─────────────────────────────────────────────────┐    │        │
│  │  │  DB0: Identity+Chat    │  DB1: Post             │    │        │
│  │  │  - users, sessions     │  - posts, comments     │    │        │
│  │  │  - call state          │  - likes               │    │        │
│  │  ├────────────────────────┼────────────────────────┤    │        │
│  │  │  DB2: Profile          │  DB3: Story+Notification│   │        │
│  │  │  - profiles            │  - stories             │    │        │
│  │  │  - followers           │  - notifications       │    │        │
│  │  └─────────────────────────────────────────────────┘    │        │
│  └─────────────────────────────────────────────────────────┘        │
│                                                                       │
│  ┌─────────────────────────────────────────────────────────┐        │
│  │                    KAFKA (Event Bus)                     │        │
│  │  Topics: profile-updated, post-created, cache-invalidate │        │
│  └─────────────────────────────────────────────────────────┘        │
│                                                                       │
└──────────────────────────────────────────────────────────────────────┘
```

---

**Document Version:** 1.0.0  
**Last Updated:** 2026-01-16  
**Author:** AI Software Architect
