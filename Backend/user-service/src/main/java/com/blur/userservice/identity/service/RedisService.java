package com.blur.userservice.identity.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
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
        } catch (Exception e) {
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
        } catch (Exception e) {
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
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
