package com.blur.notificationservice.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IdempotencyService {
  RedisTemplate<String, String> redisTemplate;
  private static final String KEY_PREFIX = "precessed_event:";
  private static final Duration TTL = Duration.ofHours(24);

  // check event da duoc xu ly chua
  public boolean isProcessed(String eventId) {
    String key = KEY_PREFIX + eventId;
    boolean equals = Boolean.TRUE.equals(redisTemplate.hasKey(key));
    return equals;
  }

  // danh dau event da xu ly
  public void markProcessed(String eventId) {
    String key = KEY_PREFIX + eventId;
    redisTemplate.opsForValue().set(key, "1", TTL);
  }
}
