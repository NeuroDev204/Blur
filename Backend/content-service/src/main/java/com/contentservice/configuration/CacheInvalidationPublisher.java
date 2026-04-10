package com.contentservice.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheInvalidationPublisher {
    private static final String CHANNEL = "cache-invalidation:content-service";
    private final RedisTemplate<String, Object> redisTemplate;

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
