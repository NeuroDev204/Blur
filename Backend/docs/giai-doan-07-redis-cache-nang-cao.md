# Giai Đoạn 7: Redis Cache Nâng Cao (CHƯA TRIỂN KHAI)

## Mục tiêu

Nâng cấp hệ thống Redis cache hiện tại lên production-grade với 5 kỹ thuật chính:

1. **Multi-level Cache:** Caffeine L1 (in-memory) + Redis L2 (distributed)
2. **Distributed Lock:** Ngăn Cache Stampede khi nhiều request đồng thời cache miss
3. **Pub/Sub Cache Invalidation Bus:** Đồng bộ invalidation giữa nhiều instances
4. **Lua Script Atomic:** Like/view counter atomic, tránh race condition
5. **Cache Warming on Startup:** Pre-load hot data khi service khởi động

## Trạng thái: CHƯA TRIỂN KHAI

## Redis DB Layout hiện tại

| DB | Service | Dữ liệu |
|----|---------|----------|
| 0 | User Service | Token blacklist, online status (RedisMultiDbConfig) |
| 1 | Content Service | Post cache, feed cache, comment cache (RedisConfig) |
| 2 | User Service | Profile cache, recommendation cache (RedisMultiDbConfig) |
| 3 | Communication Service | Session cache, message dedup, unread counts |

## Caching hiện tại (đã có)

### Content Service (DB 1)

**File:** `content-service/src/main/java/com/contentservice/configuration/RedisConfig.java`

Cache TTLs hiện tại:
- `posts`: 2 phút
- `post`: 5 phút
- `comments`, `commentReplies`, `nestedReplies`: 3 phút
- `commentReplyById`: 5 phút
- `savedPosts`, `profiles`: 10 phút
- `feed`: 5 phút (default)

### User Service (DB 0 + DB 2)

**File:** `user-service/src/main/java/com/blur/userservice/profile/configuration/RedisMultiDbConfig.java`

- DB 0: Token storage (30-min TTL) - `invalidateToken()`, `isTokenInvalidated()`
- DB 2: Profile cache (10-min TTL) - `@Cacheable` cho profiles, recommendations

### Communication Service (DB 3)

**File:** `communication-service/src/main/java/com/blur/communicationservice/service/RedisCacheService.java`

- Call state/session management
- User online status
- Missed call counters
- Unread message counts (per conversation, per user)
- Message deduplication (3-second TTL)

---

## Bước 1: Thêm Dependencies

**File:** `pom.xml` (cho mỗi service cần cache nâng cao)

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

## Bước 2: Multi-level Cache - Caffeine L1 + Redis L2

### Tạo TwoLevelCacheManager

**File:** `content-service/src/main/java/com/contentservice/configuration/TwoLevelCacheManager.java`

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

    // Caffeine L1 config: nhỏ, nhanh, TTL ngắn
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

### Tạo TwoLevelCache

**File:** `content-service/src/main/java/com/contentservice/configuration/TwoLevelCache.java`

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
        // 1. Check L1 (Caffeine) trước
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
        T l1Value = l1Cache.get(key, (Class<T>) null);
        if (l1Value != null) return l1Value;
        return l2Cache.get(key, valueLoader);
    }

    @Override
    public void put(Object key, Object value) {
        l1Cache.put(key, value);
        l2Cache.put(key, value);
    }

    @Override
    public void evict(Object key) {
        l1Cache.evict(key);
        l2Cache.evict(key);
    }

    @Override
    public void clear() {
        l1Cache.clear();
        l2Cache.clear();
    }
}
```

## Bước 3: Distributed Lock (Ngăn Cache Stampede)

**File:** `content-service/src/main/java/com/contentservice/configuration/DistributedCacheService.java`

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
     * Nếu cache miss → chỉ 1 thread acquire lock → load data → put cache.
     * Các thread khác đợi lock release → đọc từ cache.
     */
    public <T> T getWithLock(String cacheKey, Duration ttl, Supplier<T> dataLoader) {
        // 1. Check cache
        @SuppressWarnings("unchecked")
        T cached = (T) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) return cached;

        // 2. Cache miss → acquire lock
        String lockKey = "lock:" + cacheKey;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean acquired = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (acquired) {
                try {
                    // Double-check: thread khác có thể đã load xong
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
                    lock.unlock();
                }
            } else {
                // Lock timeout → load trực tiếp (fallback)
                return dataLoader.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return dataLoader.get();
        }
    }
}
```

## Bước 4: Pub/Sub Cache Invalidation Bus

Khi chạy nhiều instances, invalidate cache ở 1 instance cần thông báo cho tất cả instances khác.

**File:** `content-service/src/main/java/com/contentservice/configuration/CacheInvalidationPublisher.java`

```java
package com.contentservice.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CacheInvalidationPublisher {
    private final RedisTemplate<String, String> redisTemplate;
    private static final String CHANNEL = "cache-invalidation";

    public void publishInvalidation(String cacheName, String key) {
        String message = cacheName + "::" + key;
        redisTemplate.convertAndSend(CHANNEL, message);
    }
}
```

**File:** `content-service/src/main/java/com/contentservice/configuration/CacheInvalidationSubscriber.java`

```java
package com.contentservice.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
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
        String payload = new String(message.getBody());
        String[] parts = payload.split("::", 2);
        if (parts.length == 2) {
            String cacheName = parts[0];
            String key = parts[1];
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.evict(key);
                log.debug("L1 cache evicted via pub/sub: {}::{}", cacheName, key);
            }
        }
    }
}
```

## Bước 5: Lua Script Atomic Counter

**File:** `content-service/src/main/java/com/contentservice/configuration/AtomicCounterService.java`

```java
package com.contentservice.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AtomicCounterService {
    private final RedisTemplate<String, Object> redisTemplate;

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

    public Long incrementCounter(String key, long ttlSeconds) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(INCREMENT_SCRIPT, Long.class);
        return redisTemplate.execute(script,
                Collections.singletonList(key),
                String.valueOf(ttlSeconds));
    }

    public Long getCounter(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value.toString()) : 0L;
    }
}
```

## Bước 6: Cache Warming on Startup

**File:** `content-service/src/main/java/com/contentservice/configuration/CacheWarmupRunner.java`

```java
package com.contentservice.configuration;

import com.contentservice.post.service.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheWarmupRunner implements ApplicationRunner {
    private final CacheService cacheService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting cache warmup...");
        try {
            cacheService.warmUp();
            log.info("Cache warmup completed");
        } catch (Exception e) {
            log.warn("Cache warmup failed (non-critical): {}", e.getMessage());
        }
    }
}
```

## Checklist

- [ ] Thêm Caffeine và Redisson dependencies vào pom.xml
- [ ] Tạo TwoLevelCacheManager (Caffeine L1 + Redis L2)
- [ ] Tạo TwoLevelCache wrapper
- [ ] Tạo DistributedCacheService với Redisson distributed lock
- [ ] Tạo CacheInvalidationPublisher + CacheInvalidationSubscriber (Pub/Sub)
- [ ] Tạo AtomicCounterService với Lua scripts
- [ ] Tạo CacheWarmupRunner cho startup pre-loading
- [ ] Cấu hình Redisson trong application.yaml
- [ ] Test: cache hit ratio > 80%
- [ ] Test: multiple instances invalidation đồng bộ
- [ ] Test: cache stampede không xảy ra khi concurrent requests
