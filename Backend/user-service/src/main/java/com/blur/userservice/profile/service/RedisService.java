package com.blur.userservice.profile.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RedisService {
    private static final String ONLINE_KEY_PREFIX = "user:online:";
    private static final String INVALIDATED_TOKEN_PREFIX = "token:invalidated:";
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
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (RedisConnectionFailureException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public void invalidateToken(String tokenId, Date expiryTime) {
        if (tokenId == null || tokenId.isBlank() || expiryTime == null) {
            return;
        }

        String key = INVALIDATED_TOKEN_PREFIX + tokenId;
        try {
            Duration ttl = Duration.between(Instant.now(), expiryTime.toInstant());
            if (ttl.isNegative()) {
                ttl = Duration.ZERO;
            }
            redisTemplate.opsForValue().set(key, "1", ttl);
        } catch (RedisConnectionFailureException e) {
        } catch (Exception e) {
        }
    }

    public boolean isTokenInvalidated(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            return false;
        }

        String key = INVALIDATED_TOKEN_PREFIX + tokenId;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (RedisConnectionFailureException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
