package com.blur.userservice.profile.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RedisService {
    private static final String ONLINE_KEY_PREFIX = "user:online:";
    private static final Duration ONLINE_TTL = Duration.ofMinutes(30);
    RedisTemplate<String, Object> redisTemplate;

    public void setOnline(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }
        String key = ONLINE_KEY_PREFIX + userId;
        try {
            redisTemplate.opsForValue().set(key, System.currentTimeMillis(), ONLINE_TTL);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis connection failed for setOnline userId={}: {}", userId, e.getMessage());
        } catch (Exception e) {
            log.warn("Failed to set online status userId={}: {}", userId, e.getMessage());
        }
    }

    public void setOffline(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }

        String key = ONLINE_KEY_PREFIX + userId;
        try {
            redisTemplate.delete(key);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis connection failed for setOffline userId={}: {}", userId, e.getMessage());
        } catch (Exception e) {
            log.warn("Failed to set offline status userId={}: {}", userId, e.getMessage());
        }
    }

    public boolean isOnline(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }

        String key = ONLINE_KEY_PREFIX + userId;
        try {
            return redisTemplate.hasKey(key);
        } catch (RedisConnectionFailureException e) {
            log.warn("Redis connection failed for isOnline userId={}: {}", userId, e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("Failed to check online status userId={}: {}", userId, e.getMessage());
            return false;
        }
    }

}
