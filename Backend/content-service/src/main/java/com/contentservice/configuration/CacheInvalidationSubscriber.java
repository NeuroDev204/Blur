package com.contentservice.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.cache.Cache;
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
    public void onMessage(Message message, @Nullable byte[] pattern) {
        try {
            String payload = new String(message.getBody());
            String[] parts = payload.split("::", 2);
            if (parts.length == 2) {
                String cacheName = parts[0];
                String key = parts[1];
                Cache cache = cacheManager.getCache(cacheName);
                // Must call evictLocalOnly/clearLocalOnly — NOT cache.evict()/cache.clear().
                // cache.evict() on TwoLevelCache re-publishes to Redis, which triggers this
                // listener again, creating an infinite pub/sub loop that exhausts heap.
                if (cache instanceof TwoLevelCache twoLevel) {
                    if ("*".equals(key)) {
                        twoLevel.clearLocalOnly();
                        log.debug("L1 cache cleared via pub/sub: {}", cacheName);
                    } else {
                        twoLevel.evictLocalOnly(key);
                        log.debug("L1 cache evicted via pub/sub: {}::{}", cacheName, key);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to process cache invalidation message: {}", e.getMessage());
        }
    }
}
