# Giai Đoạn 7: Redis Cache Nâng Cao

## Mục tiêu

Triển khai Redis Cache nâng cao với 5 kỹ thuật chính:

1. **Multi-level Cache:** Caffeine L1 (in-memory) + Redis L2 (distributed)
2. **Distributed Lock:** Ngăn Cache Stampede khi nhiều request đồng thời cache miss
3. **Pub/Sub Cache Invalidation Bus:** Đồng bộ invalidation giữa nhiều instances
4. **Lua Script Atomic:** Like/view counter atomic, tránh race condition
5. **Cache Warming on Startup:** Pre-load hot data khi service khởi động

## Redis DB Layout

| DB | Service | Dữ liệu |
|----|---------|----------|
| 0 | Identity | Token blacklist, online status |
| 1 | Content | Post cache, feed cache, like/view counters |
| 2 | Profile | Profile cache, recommendation cache |
| 3 | Communication | Session cache, message dedup |

## Bước 1: Thêm Dependencies

**File:** `pom.xml` (cho mỗi service cần cache)

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
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

## Bước 2: Multi-level Cache - Caffeine L1 + Redis L2

### 2.1 Tạo TwoLevelCacheManager

**File:** `profile-service/src/main/java/com/blur/profileservice/configuration/TwoLevelCacheManager.java`

```java
package com.blur.profileservice.configuration;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TwoLevelCacheManager implements CacheManager {

    private final CacheManager caffeineCacheManager;
    private final RedisCacheManager redisCacheManager;
    private final Map<String, TwoLevelCache> cacheMap = new ConcurrentHashMap<>();

    public TwoLevelCacheManager(CacheManager caffeineCacheManager, RedisCacheManager redisCacheManager) {
        this.caffeineCacheManager = caffeineCacheManager;
        this.redisCacheManager = redisCacheManager;
    }

    @Override
    public Cache getCache(String name) {
        return cacheMap.computeIfAbsent(name, key -> {
            Cache caffeineCache = caffeineCacheManager.getCache(key);
            Cache redisCache = redisCacheManager.getCache(key);
            if (caffeineCache == null || redisCache == null) {
                return null;
            }
            return new TwoLevelCache(caffeineCache, redisCache);
        });
    }

    @Override
    public Collection<String> getCacheNames() {
        return redisCacheManager.getCacheNames();
    }
}
```

### 2.2 Tạo TwoLevelCache

**File:** `profile-service/src/main/java/com/blur/profileservice/configuration/TwoLevelCache.java`

```java
package com.blur.profileservice.configuration;

import org.springframework.cache.Cache;

import java.util.concurrent.Callable;

public class TwoLevelCache implements Cache {

    private final Cache caffeineCache;
    private final Cache redisCache;

    public TwoLevelCache(Cache caffeineCache, Cache redisCache) {
        this.caffeineCache = caffeineCache;
        this.redisCache = redisCache;
    }

    @Override
    public String getName() {
        return redisCache.getName();
    }

    @Override
    public Object getNativeCache() {
        return redisCache.getNativeCache();
    }

    @Override
    public ValueWrapper get(Object key) {
        // L1: Caffeine (in-memory, cực nhanh)
        ValueWrapper value = caffeineCache.get(key);
        if (value != null) {
            return value;
        }

        // L2: Redis (distributed)
        value = redisCache.get(key);
        if (value != null) {
            // Đẩy ngược lên L1 cho lần truy cập sau
            caffeineCache.put(key, value.get());
        }
        return value;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        T value = caffeineCache.get(key, type);
        if (value != null) {
            return value;
        }

        value = redisCache.get(key, type);
        if (value != null) {
            caffeineCache.put(key, value);
        }
        return value;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        T value = caffeineCache.get(key, () -> {
            T redisValue = redisCache.get(key, valueLoader);
            return redisValue;
        });
        return value;
    }

    @Override
    public void put(Object key, Object value) {
        // Ghi vào cả 2 tầng
        caffeineCache.put(key, value);
        redisCache.put(key, value);
    }

    @Override
    public void evict(Object key) {
        // Xóa khỏi cả 2 tầng
        caffeineCache.evict(key);
        redisCache.evict(key);
    }

    @Override
    public void clear() {
        caffeineCache.clear();
        redisCache.clear();
    }
}
```

### 2.3 Cấu hình CacheConfig với Multi-level

**File:** `profile-service/src/main/java/com/blur/profileservice/configuration/CacheConfig.java`

```java
package com.blur.profileservice.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CaffeineCacheManager caffeineCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats());
        return manager;
    }

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
                "profiles", defaultConfig.entryTtl(Duration.ofMinutes(30)),
                "recommendations", defaultConfig.entryTtl(Duration.ofMinutes(5)),
                "followers", defaultConfig.entryTtl(Duration.ofMinutes(15)),
                "following", defaultConfig.entryTtl(Duration.ofMinutes(15)),
                "feed", defaultConfig.entryTtl(Duration.ofSeconds(30))
        );

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

    @Primary
    @Bean
    public CacheManager cacheManager(CaffeineCacheManager caffeineCacheManager,
                                     RedisCacheManager redisCacheManager) {
        return new TwoLevelCacheManager(caffeineCacheManager, redisCacheManager);
    }
}
```

## Bước 3: Distributed Lock - Ngăn Cache Stampede

Khi cache expire, nhiều request đồng thời đều miss cache → tất cả đều query DB → overload.
Giải pháp: chỉ 1 request được phép rebuild cache, các request khác đợi.

### 3.1 Cấu hình Redisson

**File:** `profile-service/src/main/java/com/blur/profileservice/configuration/RedissonConfig.java`

```java
package com.blur.profileservice.configuration;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + redisHost + ":" + redisPort)
                .setDatabase(2)
                .setConnectionMinimumIdleSize(5)
                .setConnectionPoolSize(10);
        return Redisson.create(config);
    }
}
```

### 3.2 Tạo CacheStampedeProtector

**File:** `profile-service/src/main/java/com/blur/profileservice/service/CacheStampedeProtector.java`

```java
package com.blur.profileservice.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CacheStampedeProtector {

    RedissonClient redissonClient;
    RedisTemplate<String, Object> redisTemplate;

    /**
     * Lấy data từ cache, nếu miss thì dùng distributed lock để chỉ 1 thread rebuild.
     * Các thread khác đợi lock rồi đọc lại từ cache.
     *
     * @param cacheKey   Redis key
     * @param ttl        Thời gian sống của cache
     * @param dataLoader Function để load data từ DB khi cache miss
     * @return Cached hoặc freshly loaded data
     */
    @SuppressWarnings("unchecked")
    public <T> T getWithLock(String cacheKey, Duration ttl, Supplier<T> dataLoader) {
        // Thử đọc cache trước
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return (T) cached;
        }

        // Cache miss → acquire distributed lock
        String lockKey = "lock:" + cacheKey;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Đợi tối đa 5 giây để lấy lock, lock tự expire sau 10 giây
            boolean acquired = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (acquired) {
                try {
                    // Double-check: có thể thread khác đã rebuild cache rồi
                    cached = redisTemplate.opsForValue().get(cacheKey);
                    if (cached != null) {
                        return (T) cached;
                    }

                    // Chỉ thread này load data từ DB
                    T data = dataLoader.get();
                    if (data != null) {
                        redisTemplate.opsForValue().set(cacheKey, data, ttl);
                    }
                    return data;
                } finally {
                    lock.unlock();
                }
            } else {
                // Không lấy được lock → thread khác đang rebuild → đọc lại cache
                cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    return (T) cached;
                }
                // Worst case: vẫn miss → fallback load từ DB
                return dataLoader.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Lock interrupted for key: {}", cacheKey, e);
            return dataLoader.get();
        }
    }
}
```

### 3.3 Sử dụng CacheStampedeProtector trong Service

**File:** `profile-service/src/main/java/com/blur/profileservice/service/UserProfileService.java`

```java
package com.blur.profileservice.service;

import com.blur.profileservice.dto.response.UserProfileResponse;
import com.blur.profileservice.entity.UserProfile;
import com.blur.profileservice.repository.UserProfileRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserProfileService {

    UserProfileRepository userProfileRepository;
    CacheStampedeProtector cacheStampedeProtector;
    CacheInvalidationService cacheInvalidationService;

    @Cacheable(value = "profiles", key = "#userId", unless = "#result == null")
    public UserProfileResponse getProfile(String userId) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        return mapToResponse(profile);
    }

    /**
     * Dùng cho hot profiles (celebrities, influencers) có lượng request rất cao.
     * Khi cache expire, chỉ 1 thread rebuild, các thread khác đợi.
     */
    public UserProfileResponse getHotProfile(String userId) {
        String cacheKey = "profiles:hot:" + userId;
        return cacheStampedeProtector.getWithLock(
                cacheKey,
                Duration.ofMinutes(30),
                () -> {
                    log.info("Cache miss for hot profile: {} → loading from DB", userId);
                    UserProfile profile = userProfileRepository.findByUserId(userId)
                            .orElseThrow(() -> new RuntimeException("Profile not found"));
                    return mapToResponse(profile);
                }
        );
    }

    @CacheEvict(value = "profiles", key = "#userId")
    public UserProfileResponse updateProfile(String userId, UserProfileRequest request) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setBio(request.getBio());
        profile.setAvatar(request.getAvatar());
        profile = userProfileRepository.save(profile);

        cacheInvalidationService.invalidateOnProfileUpdate(userId);

        return mapToResponse(profile);
    }

    private UserProfileResponse mapToResponse(UserProfile profile) {
        return UserProfileResponse.builder()
                .userId(profile.getUserId())
                .username(profile.getUsername())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .avatar(profile.getAvatar())
                .bio(profile.getBio())
                .build();
    }
}
```

## Bước 4: Pub/Sub Cache Invalidation Bus

Khi chạy nhiều instances của cùng 1 service, invalidate cache L1 (Caffeine) trên instance A không ảnh hưởng instance B.
Giải pháp: dùng Redis Pub/Sub để broadcast invalidation message tới tất cả instances.

### 4.1 Tạo CacheInvalidationPublisher

**File:** `profile-service/src/main/java/com/blur/profileservice/service/CacheInvalidationPublisher.java`

```java
package com.blur.profileservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CacheInvalidationPublisher {

    RedisTemplate<String, String> redisTemplate;
    ObjectMapper objectMapper;

    private static final String CHANNEL = "cache:invalidation";

    /**
     * Publish invalidation message tới tất cả instances qua Redis Pub/Sub.
     */
    public void publishInvalidation(String cacheName, String cacheKey) {
        try {
            Map<String, String> message = Map.of(
                    "cacheName", cacheName,
                    "cacheKey", cacheKey
            );
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(CHANNEL, json);
            log.debug("Published cache invalidation: cacheName={}, cacheKey={}", cacheName, cacheKey);
        } catch (Exception e) {
            log.error("Failed to publish cache invalidation", e);
        }
    }

    public void publishInvalidateAll(String cacheName) {
        try {
            Map<String, String> message = Map.of(
                    "cacheName", cacheName,
                    "cacheKey", "*"
            );
            String json = objectMapper.writeValueAsString(message);
            redisTemplate.convertAndSend(CHANNEL, json);
            log.debug("Published cache invalidate all: cacheName={}", cacheName);
        } catch (Exception e) {
            log.error("Failed to publish cache invalidation", e);
        }
    }
}
```

### 4.2 Tạo CacheInvalidationSubscriber

**File:** `profile-service/src/main/java/com/blur/profileservice/service/CacheInvalidationSubscriber.java`

```java
package com.blur.profileservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CacheInvalidationSubscriber implements MessageListener {

    CaffeineCacheManager caffeineCacheManager;
    ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> event = objectMapper.readValue(message.getBody(), Map.class);
            String cacheName = event.get("cacheName");
            String cacheKey = event.get("cacheKey");

            var cache = caffeineCacheManager.getCache(cacheName);
            if (cache == null) {
                return;
            }

            if ("*".equals(cacheKey)) {
                cache.clear();
                log.debug("L1 cache cleared: cacheName={}", cacheName);
            } else {
                cache.evict(cacheKey);
                log.debug("L1 cache evicted: cacheName={}, cacheKey={}", cacheName, cacheKey);
            }
        } catch (Exception e) {
            log.error("Failed to process cache invalidation message", e);
        }
    }
}
```

### 4.3 Đăng ký Redis Listener

**File:** `profile-service/src/main/java/com/blur/profileservice/configuration/RedisListenerConfig.java`

```java
package com.blur.profileservice.configuration;

import com.blur.profileservice.service.CacheInvalidationSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisListenerConfig {

    @Bean
    public ChannelTopic cacheInvalidationTopic() {
        return new ChannelTopic("cache:invalidation");
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            CacheInvalidationSubscriber cacheInvalidationSubscriber,
            ChannelTopic cacheInvalidationTopic) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(cacheInvalidationSubscriber, cacheInvalidationTopic);
        return container;
    }
}
```

### 4.4 Cập nhật CacheInvalidationService dùng Pub/Sub

**File:** `profile-service/src/main/java/com/blur/profileservice/service/CacheInvalidationService.java`

```java
package com.blur.profileservice.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CacheInvalidationService {

    CacheInvalidationPublisher cacheInvalidationPublisher;

    @Caching(evict = {
            @CacheEvict(value = "recommendations", allEntries = true),
            @CacheEvict(value = "followers", key = "#userId"),
            @CacheEvict(value = "following", key = "#userId")
    })
    public void invalidateOnFollow(String userId) {
        // Broadcast tới tất cả instances → xóa L1 Caffeine cache
        cacheInvalidationPublisher.publishInvalidateAll("recommendations");
        cacheInvalidationPublisher.publishInvalidation("followers", userId);
        cacheInvalidationPublisher.publishInvalidation("following", userId);
        log.debug("Cache invalidated for follow event: userId={}", userId);
    }

    @Caching(evict = {
            @CacheEvict(value = "profiles", key = "#userId"),
            @CacheEvict(value = "recommendations", allEntries = true)
    })
    public void invalidateOnProfileUpdate(String userId) {
        // Broadcast tới tất cả instances → xóa L1 Caffeine cache
        cacheInvalidationPublisher.publishInvalidation("profiles", userId);
        cacheInvalidationPublisher.publishInvalidateAll("recommendations");
        log.debug("Cache invalidated for profile update: userId={}", userId);
    }
}
```

## Bước 5: Lua Script Atomic cho Like/View Counter

Redis Lua scripts chạy atomic trên server, tránh race condition khi nhiều users đồng thời like/view.

### 5.1 Tạo file Lua scripts

**File:** `content-service/src/main/resources/scripts/increment-like.lua`

```lua
-- KEYS[1] = post like count key (e.g., "post:likes:abc123")
-- KEYS[2] = user like set key (e.g., "post:liked_by:abc123")
-- ARGV[1] = userId
-- ARGV[2] = TTL in seconds cho counter key

-- Kiểm tra user đã like chưa
local alreadyLiked = redis.call('SISMEMBER', KEYS[2], ARGV[1])
if alreadyLiked == 1 then
    return -1  -- Đã like rồi
end

-- Thêm user vào set đã like
redis.call('SADD', KEYS[2], ARGV[1])

-- Tăng like count
local newCount = redis.call('INCR', KEYS[1])

-- Set TTL nếu key mới được tạo
if newCount == 1 then
    redis.call('EXPIRE', KEYS[1], ARGV[2])
    redis.call('EXPIRE', KEYS[2], ARGV[2])
end

return newCount
```

**File:** `content-service/src/main/resources/scripts/decrement-like.lua`

```lua
-- KEYS[1] = post like count key
-- KEYS[2] = user like set key
-- ARGV[1] = userId

-- Kiểm tra user có trong set không
local wasLiked = redis.call('SISMEMBER', KEYS[2], ARGV[1])
if wasLiked == 0 then
    return -1  -- Chưa like, không thể unlike
end

-- Xóa user khỏi set
redis.call('SREM', KEYS[2], ARGV[1])

-- Giảm like count, không cho xuống dưới 0
local newCount = redis.call('DECR', KEYS[1])
if newCount < 0 then
    redis.call('SET', KEYS[1], 0)
    newCount = 0
end

return newCount
```

**File:** `content-service/src/main/resources/scripts/increment-view.lua`

```lua
-- KEYS[1] = post view count key (e.g., "post:views:abc123")
-- KEYS[2] = user view set key (e.g., "post:viewed_by:abc123")
-- ARGV[1] = userId
-- ARGV[2] = TTL in seconds cho view set (tránh đếm lặp trong 1 giờ)
-- ARGV[3] = TTL in seconds cho counter key

-- Kiểm tra user đã view trong khoảng thời gian chưa
local alreadyViewed = redis.call('SISMEMBER', KEYS[2], ARGV[1])
if alreadyViewed == 1 then
    return redis.call('GET', KEYS[1]) or 0  -- Trả về count hiện tại, không tăng
end

-- Thêm user vào set đã view (tự expire để cho phép đếm lại sau 1 giờ)
redis.call('SADD', KEYS[2], ARGV[1])
redis.call('EXPIRE', KEYS[2], ARGV[2])

-- Tăng view count
local newCount = redis.call('INCR', KEYS[1])

-- Set TTL cho counter nếu mới tạo
if newCount == 1 then
    redis.call('EXPIRE', KEYS[1], ARGV[3])
end

return newCount
```

### 5.2 Tạo RedisCounterService

**File:** `content-service/src/main/java/com/contentservice/post/service/RedisCounterService.java`

```java
package com.contentservice.post.service;

import jakarta.annotation.PostConstruct;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RedisCounterService {

    RedisTemplate<String, String> redisTemplate;

    private DefaultRedisScript<Long> incrementLikeScript;
    private DefaultRedisScript<Long> decrementLikeScript;
    private DefaultRedisScript<Long> incrementViewScript;

    private static final long COUNTER_TTL_SECONDS = 86400 * 7;  // 7 ngày
    private static final long VIEW_DEDUP_TTL_SECONDS = 3600;     // 1 giờ

    @PostConstruct
    public void init() {
        incrementLikeScript = new DefaultRedisScript<>();
        incrementLikeScript.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("scripts/increment-like.lua")));
        incrementLikeScript.setResultType(Long.class);

        decrementLikeScript = new DefaultRedisScript<>();
        decrementLikeScript.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("scripts/decrement-like.lua")));
        decrementLikeScript.setResultType(Long.class);

        incrementViewScript = new DefaultRedisScript<>();
        incrementViewScript.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("scripts/increment-view.lua")));
        incrementViewScript.setResultType(Long.class);
    }

    /**
     * Like một post. Atomic: kiểm tra trùng + tăng counter trong 1 operation.
     * @return Số like mới, hoặc -1 nếu đã like rồi
     */
    public long likePost(String postId, String userId) {
        List<String> keys = Arrays.asList(
                "post:likes:" + postId,
                "post:liked_by:" + postId
        );
        Long result = redisTemplate.execute(
                incrementLikeScript, keys, userId, String.valueOf(COUNTER_TTL_SECONDS));
        if (result != null && result == -1) {
            log.debug("User {} already liked post {}", userId, postId);
        } else {
            log.debug("Post {} liked by user {}, new count: {}", postId, userId, result);
        }
        return result != null ? result : 0;
    }

    /**
     * Unlike một post. Atomic: kiểm tra + giảm counter.
     * @return Số like mới, hoặc -1 nếu chưa like
     */
    public long unlikePost(String postId, String userId) {
        List<String> keys = Arrays.asList(
                "post:likes:" + postId,
                "post:liked_by:" + postId
        );
        Long result = redisTemplate.execute(decrementLikeScript, keys, userId);
        log.debug("Post {} unliked by user {}, new count: {}", postId, userId, result);
        return result != null ? result : 0;
    }

    /**
     * Ghi nhận view một post. Dedup: mỗi user chỉ tính 1 view trong 1 giờ.
     * @return Tổng số view
     */
    public long viewPost(String postId, String userId) {
        List<String> keys = Arrays.asList(
                "post:views:" + postId,
                "post:viewed_by:" + postId
        );
        Long result = redisTemplate.execute(
                incrementViewScript, keys, userId,
                String.valueOf(VIEW_DEDUP_TTL_SECONDS),
                String.valueOf(COUNTER_TTL_SECONDS));
        return result != null ? result : 0;
    }

    /**
     * Lấy like count hiện tại của post.
     */
    public long getLikeCount(String postId) {
        String value = redisTemplate.opsForValue().get("post:likes:" + postId);
        return value != null ? Long.parseLong(value) : 0;
    }

    /**
     * Lấy view count hiện tại của post.
     */
    public long getViewCount(String postId) {
        String value = redisTemplate.opsForValue().get("post:views:" + postId);
        return value != null ? Long.parseLong(value) : 0;
    }

    /**
     * Kiểm tra user đã like post chưa.
     */
    public boolean hasUserLiked(String postId, String userId) {
        Boolean result = redisTemplate.opsForSet().isMember("post:liked_by:" + postId, userId);
        return result != null && result;
    }
}
```

### 5.3 Tạo LikeController

**File:** `content-service/src/main/java/com/contentservice/post/controller/LikeController.java`

```java
package com.contentservice.post.controller;

import com.contentservice.post.service.RedisCounterService;
import com.contentservice.story.dto.response.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LikeController {

    RedisCounterService redisCounterService;

    @PostMapping("/{postId}/like")
    public ApiResponse<Map<String, Object>> likePost(@PathVariable String postId) {
        String userId = getCurrentUserId();
        long newCount = redisCounterService.likePost(postId, userId);
        boolean alreadyLiked = (newCount == -1);

        Map<String, Object> result = Map.of(
                "postId", postId,
                "likeCount", alreadyLiked ? redisCounterService.getLikeCount(postId) : newCount,
                "liked", !alreadyLiked,
                "message", alreadyLiked ? "Already liked" : "Liked successfully"
        );

        return ApiResponse.<Map<String, Object>>builder()
                .result(result)
                .build();
    }

    @DeleteMapping("/{postId}/like")
    public ApiResponse<Map<String, Object>> unlikePost(@PathVariable String postId) {
        String userId = getCurrentUserId();
        long newCount = redisCounterService.unlikePost(postId, userId);
        boolean wasNotLiked = (newCount == -1);

        Map<String, Object> result = Map.of(
                "postId", postId,
                "likeCount", wasNotLiked ? redisCounterService.getLikeCount(postId) : newCount,
                "liked", false,
                "message", wasNotLiked ? "Not liked yet" : "Unliked successfully"
        );

        return ApiResponse.<Map<String, Object>>builder()
                .result(result)
                .build();
    }

    @PostMapping("/{postId}/view")
    public ApiResponse<Map<String, Long>> viewPost(@PathVariable String postId) {
        String userId = getCurrentUserId();
        long viewCount = redisCounterService.viewPost(postId, userId);

        Map<String, Long> result = Map.of("viewCount", viewCount);
        return ApiResponse.<Map<String, Long>>builder()
                .result(result)
                .build();
    }

    @GetMapping("/{postId}/engagement")
    public ApiResponse<Map<String, Object>> getEngagement(@PathVariable String postId) {
        String userId = getCurrentUserId();

        Map<String, Object> result = Map.of(
                "postId", postId,
                "likeCount", redisCounterService.getLikeCount(postId),
                "viewCount", redisCounterService.getViewCount(postId),
                "liked", redisCounterService.hasUserLiked(postId, userId)
        );

        return ApiResponse.<Map<String, Object>>builder()
                .result(result)
                .build();
    }

    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
```

## Bước 6: Cache Warming on Startup

Pre-load hot data (top profiles, trending posts) vào cache khi service khởi động, tránh cold start.

### 6.1 Tạo CacheWarmer

**File:** `profile-service/src/main/java/com/blur/profileservice/service/CacheWarmer.java`

```java
package com.blur.profileservice.service;

import com.blur.profileservice.dto.response.UserProfileResponse;
import com.blur.profileservice.entity.UserProfile;
import com.blur.profileservice.repository.UserProfileRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CacheWarmer {

    UserProfileRepository userProfileRepository;
    CacheManager cacheManager;
    UserProfileService userProfileService;

    /**
     * Khi application khởi động xong → pre-load top 100 profiles vào cache.
     * Chạy async để không block startup.
     */
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void warmUpCaches() {
        log.info("Cache warming started...");
        long startTime = System.currentTimeMillis();

        warmUpTopProfiles();
        warmUpRecentlyActiveProfiles();

        long duration = System.currentTimeMillis() - startTime;
        log.info("Cache warming completed in {} ms", duration);
    }

    private void warmUpTopProfiles() {
        try {
            // Load top 100 profiles có nhiều followers nhất
            List<UserProfile> topProfiles = userProfileRepository
                    .findTopByOrderByFollowerCountDesc(PageRequest.of(0, 100));

            var cache = cacheManager.getCache("profiles");
            if (cache == null) {
                log.warn("Cache 'profiles' not found, skipping warm-up");
                return;
            }

            int count = 0;
            for (UserProfile profile : topProfiles) {
                UserProfileResponse response = UserProfileResponse.builder()
                        .userId(profile.getUserId())
                        .username(profile.getUsername())
                        .firstName(profile.getFirstName())
                        .lastName(profile.getLastName())
                        .avatar(profile.getAvatar())
                        .bio(profile.getBio())
                        .build();
                cache.put(profile.getUserId(), response);
                count++;
            }
            log.info("Warmed up {} top profiles", count);
        } catch (Exception e) {
            log.error("Failed to warm up top profiles", e);
        }
    }

    private void warmUpRecentlyActiveProfiles() {
        try {
            // Load profiles đã active trong 24 giờ qua
            List<UserProfile> recentProfiles = userProfileRepository
                    .findRecentlyActive(PageRequest.of(0, 200));

            var cache = cacheManager.getCache("profiles");
            if (cache == null) {
                return;
            }

            int count = 0;
            for (UserProfile profile : recentProfiles) {
                UserProfileResponse response = UserProfileResponse.builder()
                        .userId(profile.getUserId())
                        .username(profile.getUsername())
                        .firstName(profile.getFirstName())
                        .lastName(profile.getLastName())
                        .avatar(profile.getAvatar())
                        .bio(profile.getBio())
                        .build();
                cache.put(profile.getUserId(), response);
                count++;
            }
            log.info("Warmed up {} recently active profiles", count);
        } catch (Exception e) {
            log.error("Failed to warm up recently active profiles", e);
        }
    }
}
```

### 6.2 Thêm Repository methods cho Cache Warming

**File:** `profile-service/src/main/java/com/blur/profileservice/repository/UserProfileRepository.java`

```java
package com.blur.profileservice.repository;

import com.blur.profileservice.entity.UserProfile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, String> {

    Optional<UserProfile> findByUserId(String userId);

    List<UserProfile> findTopByOrderByFollowerCountDesc(Pageable pageable);

    @Query("SELECT p FROM UserProfile p WHERE p.lastActiveAt >= CURRENT_TIMESTAMP - 1 ORDER BY p.lastActiveAt DESC")
    List<UserProfile> findRecentlyActive(Pageable pageable);
}
```

### 6.3 Bật Async Support

**File:** `profile-service/src/main/java/com/blur/profileservice/configuration/AsyncConfig.java`

```java
package com.blur.profileservice.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
}
```

## Bước 7: Cấu hình Redis trong application.yaml

**File:** `profile-service/src/main/resources/application.yaml`

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      database: 2
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
          max-wait: -1ms
```

**File:** `content-service/src/main/resources/application.yaml`

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      database: 1
      timeout: 3000ms
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
          max-wait: -1ms
```

## Hướng dẫn Test

### Test 1: Multi-level Cache (L1 Caffeine + L2 Redis)

**Bước 1:** Đảm bảo Redis và services đang chạy.

```bash
docker-compose up -d redis
```

**Bước 2:** Gọi API getProfile lần 1 (cache miss → query DB → ghi vào L1 + L2).

```bash
curl -X GET http://localhost:8888/api/profile/users/my-profile \
  -H "Authorization: Bearer <TOKEN>"
```

**Bước 3:** Kiểm tra Redis L2 đã có data.

```bash
docker exec -it redis redis-cli -n 2
KEYS profiles*
GET "profiles::user-id-xxx"
```

**Bước 4:** Gọi API lần 2 → hit L1 Caffeine (không gọi Redis).

```bash
curl -X GET http://localhost:8888/api/profile/users/my-profile \
  -H "Authorization: Bearer <TOKEN>"
```

Kiểm tra log: không có Redis GET command (data từ Caffeine in-memory).

**Bước 5:** Đợi 5 phút (Caffeine expire) → gọi lại → hit L2 Redis → đẩy ngược lên L1.

### Test 2: Distributed Lock - Cache Stampede Protection

**Bước 1:** Dùng Apache Benchmark gửi 100 concurrent requests đến cùng profile.

```bash
ab -n 100 -c 100 -H "Authorization: Bearer <TOKEN>" \
  http://localhost:8888/api/profile/users/hot/celebrity-user-id
```

**Bước 2:** Kiểm tra log → chỉ thấy 1 lần "loading from DB" (1 thread rebuild, 99 thread đợi).

```bash
docker logs profile-service 2>&1 | grep "Cache miss for hot profile"
```

Mong đợi: chỉ xuất hiện 1 dòng log (không phải 100 dòng).

**Bước 3:** Kiểm tra Redis lock key đã tự expire.

```bash
docker exec -it redis redis-cli -n 2
KEYS lock:*
```

Mong đợi: không còn lock key (đã tự expire hoặc đã unlock).

### Test 3: Pub/Sub Cache Invalidation Bus

**Bước 1:** Chạy 2 instances của profile-service (port 8081 và 8082).

```bash
# Terminal 1
java -jar profile-service.jar --server.port=8081

# Terminal 2
java -jar profile-service.jar --server.port=8082
```

**Bước 2:** Gọi getProfile qua instance 8081 → cache L1 trên instance 8081 có data.

```bash
curl -X GET http://localhost:8081/profile/users/user-123
```

**Bước 3:** Gọi getProfile qua instance 8082 → cache L1 trên instance 8082 có data.

```bash
curl -X GET http://localhost:8082/profile/users/user-123
```

**Bước 4:** Update profile qua instance 8081.

```bash
curl -X PUT http://localhost:8081/profile/users/user-123 \
  -H "Content-Type: application/json" \
  -d '{"firstName": "Updated"}'
```

**Bước 5:** Kiểm tra log instance 8082 → L1 cache đã bị evict qua Pub/Sub.

```bash
# Log instance 8082 sẽ hiển thị
L1 cache evicted: cacheName=profiles, cacheKey=user-123
```

**Bước 6:** Gọi getProfile qua instance 8082 → cache miss L1, hit L2 Redis (data mới).

### Test 4: Lua Script Atomic - Like Counter

**Bước 1:** Like một post.

```bash
curl -X POST http://localhost:8888/api/content/posts/post-123/like \
  -H "Authorization: Bearer <TOKEN>"
```

Response mong đợi:
```json
{
  "code": 1000,
  "result": {
    "postId": "post-123",
    "likeCount": 1,
    "liked": true,
    "message": "Liked successfully"
  }
}
```

**Bước 2:** Like lại cùng post (duplicate) → bị từ chối.

```bash
curl -X POST http://localhost:8888/api/content/posts/post-123/like \
  -H "Authorization: Bearer <TOKEN>"
```

Response mong đợi:
```json
{
  "code": 1000,
  "result": {
    "postId": "post-123",
    "likeCount": 1,
    "liked": false,
    "message": "Already liked"
  }
}
```

**Bước 3:** Unlike post.

```bash
curl -X DELETE http://localhost:8888/api/content/posts/post-123/like \
  -H "Authorization: Bearer <TOKEN>"
```

Response mong đợi:
```json
{
  "code": 1000,
  "result": {
    "postId": "post-123",
    "likeCount": 0,
    "liked": false,
    "message": "Unliked successfully"
  }
}
```

**Bước 4:** Kiểm tra Redis trực tiếp.

```bash
docker exec -it redis redis-cli -n 1

# Xem like count
GET "post:likes:post-123"

# Xem set users đã like
SMEMBERS "post:liked_by:post-123"
```

**Bước 5:** Test concurrent likes (10 users cùng like).

```bash
for i in {1..10}; do
  curl -s -X POST http://localhost:8888/api/content/posts/post-123/like \
    -H "Authorization: Bearer <TOKEN_USER_$i>" &
done
wait

# Kiểm tra count
curl -X GET http://localhost:8888/api/content/posts/post-123/engagement \
  -H "Authorization: Bearer <TOKEN>"
```

Mong đợi: likeCount = 10 (chính xác, không bị race condition).

### Test 5: Lua Script Atomic - View Counter

**Bước 1:** View một post.

```bash
curl -X POST http://localhost:8888/api/content/posts/post-123/view \
  -H "Authorization: Bearer <TOKEN>"
```

Response: `{"code": 1000, "result": {"viewCount": 1}}`

**Bước 2:** View lại trong vòng 1 giờ → không tăng count (dedup).

```bash
curl -X POST http://localhost:8888/api/content/posts/post-123/view \
  -H "Authorization: Bearer <TOKEN>"
```

Response: `{"code": 1000, "result": {"viewCount": 1}}` (không tăng).

**Bước 3:** Kiểm tra Redis.

```bash
docker exec -it redis redis-cli -n 1

GET "post:views:post-123"
SMEMBERS "post:viewed_by:post-123"
TTL "post:viewed_by:post-123"
```

Mong đợi: TTL khoảng 3600 giây (1 giờ dedup window).

### Test 6: Cache Warming on Startup

**Bước 1:** Xóa tất cả cache trong Redis.

```bash
docker exec -it redis redis-cli -n 2
FLUSHDB
```

**Bước 2:** Restart profile-service.

```bash
docker-compose restart profile-service
```

**Bước 3:** Kiểm tra log → thấy cache warming messages.

```bash
docker logs profile-service 2>&1 | grep -i "cache warm"
```

Mong đợi:
```
Cache warming started...
Warmed up 100 top profiles
Warmed up 200 recently active profiles
Cache warming completed in 1500 ms
```

**Bước 4:** Kiểm tra Redis đã có data ngay sau startup.

```bash
docker exec -it redis redis-cli -n 2
KEYS profiles*
DBSIZE
```

Mong đợi: khoảng 300 keys (100 top + 200 recent, có thể trùng).

**Bước 5:** Gọi API getProfile cho 1 top profile → cache hit ngay lập tức (không query DB).

```bash
curl -X GET http://localhost:8888/api/profile/users/top-user-id \
  -H "Authorization: Bearer <TOKEN>"
```

Kiểm tra log: không có DB query log.

### Test 7: Engagement Endpoint tổng hợp

```bash
curl -X GET http://localhost:8888/api/content/posts/post-123/engagement \
  -H "Authorization: Bearer <TOKEN>"
```

Response mong đợi:
```json
{
  "code": 1000,
  "result": {
    "postId": "post-123",
    "likeCount": 10,
    "viewCount": 50,
    "liked": true
  }
}
```

### Test 8: Đo cache hit ratio

```bash
docker exec -it redis redis-cli -n 2
INFO stats
```

Tìm `keyspace_hits` và `keyspace_misses`:
```
hit_ratio = keyspace_hits / (keyspace_hits + keyspace_misses)
```

Mong đợi: > 80% sau khi cache warming + hệ thống chạy ổn định.

## Checklist

- [ ] Thêm dependencies: caffeine, redisson, spring-data-redis
- [ ] Tạo TwoLevelCache và TwoLevelCacheManager (L1 Caffeine + L2 Redis)
- [ ] Tạo CacheConfig với @Primary TwoLevelCacheManager
- [ ] Tạo RedissonConfig cho Distributed Lock
- [ ] Tạo CacheStampedeProtector với getWithLock()
- [ ] Tích hợp CacheStampedeProtector vào UserProfileService.getHotProfile()
- [ ] Tạo CacheInvalidationPublisher (Redis Pub/Sub)
- [ ] Tạo CacheInvalidationSubscriber (Redis MessageListener)
- [ ] Tạo RedisListenerConfig đăng ký subscriber
- [ ] Cập nhật CacheInvalidationService dùng Pub/Sub broadcast
- [ ] Tạo Lua scripts: increment-like.lua, decrement-like.lua, increment-view.lua
- [ ] Tạo RedisCounterService (execute Lua scripts)
- [ ] Tạo LikeController: POST/DELETE /{postId}/like, POST /{postId}/view, GET /{postId}/engagement
- [ ] Tạo CacheWarmer: pre-load top profiles + recently active trên startup
- [ ] Thêm Repository methods: findTopByOrderByFollowerCountDesc, findRecentlyActive
- [ ] Bật @EnableAsync cho cache warming chạy background
- [ ] Cấu hình Redis database riêng cho từng service
- [ ] Test: Multi-level cache L1 hit → L2 hit → DB query
- [ ] Test: Distributed lock chỉ 1 thread rebuild cache khi stampede
- [ ] Test: Pub/Sub invalidation đồng bộ giữa nhiều instances
- [ ] Test: Like/unlike atomic không race condition
- [ ] Test: View dedup trong 1 giờ
- [ ] Test: Cache warming hoàn tất khi startup
- [ ] Test: Engagement endpoint trả về đúng data
- [ ] Test: Cache hit ratio > 80%
