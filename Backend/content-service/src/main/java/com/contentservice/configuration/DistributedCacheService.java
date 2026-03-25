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

    public <T> T getWithLock(String cacheKey, Duration ttl, Supplier<T> dataLoader) {
        @SuppressWarnings("unchecked")
        T cached = (T) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        String lockKey = "lock:" + cacheKey;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (acquired) {
                try {
                    @SuppressWarnings("unchecked")
                    T doubleCheck = (T) redisTemplate.opsForValue().get(cacheKey);
                    if (doubleCheck != null) {
                        return doubleCheck;
                    }
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
                log.warn("Lock timeout for key={}, loading directly", cacheKey);
                return dataLoader.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Lock interrupted for key ={}, loading directly", cacheKey);
            return dataLoader.get();
        }
    }

    public void putWithTtl(String cacheKey, Object value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(cacheKey, value, ttl);
        } catch (Exception e) {
            log.warn("Failed to put cache key={}: {}", cacheKey, e.getMessage());
        }
    }

    public void evict(String cacheKey) {
        try {
            redisTemplate.unlink(cacheKey);
        } catch (Exception e) {
            log.warn("Failed to evict cache key={}: {}", cacheKey, e.getMessage());
        }
    }
}
