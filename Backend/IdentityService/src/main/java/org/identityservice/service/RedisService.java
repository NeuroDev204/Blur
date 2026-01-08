package org.identityservice.service;

import java.time.Duration;

import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RedisService {
    RedisTemplate<String, Object> redisTemplate;

    private static final String ONLINE_KEY_PREFIX = "user:online:";
    private static final Duration ONLINE_TTL = Duration.ofMinutes(30);

    public void setOnline(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("Invalid userId for setOnline");
            return;
        }

        String key = ONLINE_KEY_PREFIX + userId;
        try {
            redisTemplate.opsForValue().set(key, System.currentTimeMillis(), ONLINE_TTL);
            log.debug("User {} set online", userId);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failed while setting user {} online", userId, e);
        } catch (Exception e) {
            log.error("Error setting user {} online", userId, e);
        }
    }

    public void setOffline(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("Invalid userId for setOffline");
            return;
        }

        String key = ONLINE_KEY_PREFIX + userId;
        try {
            redisTemplate.delete(key);
            log.debug("User {} set offline", userId);
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failed while setting user {} offline", userId, e);
        } catch (Exception e) {
            log.error("Error setting user {} offline", userId, e);
        }
    }

    public boolean isOnline(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }

        String key = ONLINE_KEY_PREFIX + userId;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (RedisConnectionFailureException e) {
            log.error("Redis connection failed while checking user {} status", userId, e);
            return false;
        } catch (Exception e) {
            log.error("Error checking user {} status", userId, e);
            return false;
        }
    }
}
