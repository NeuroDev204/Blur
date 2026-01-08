package com.blur.notificationservice.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RedisService{
   /* RedisTemplate<String, String> redisTemplate;
    public void setUserOnline(String userId) {
        redisTemplate.opsForValue().set("online:" + userId, "true", 10, TimeUnit.MINUTES);
    }

    public void setUserOffline(String userId) {
        redisTemplate.delete("online:" + userId);
    }
    public boolean isOnline(String userId){
        return Boolean.TRUE.equals(redisTemplate.hasKey("online:" + userId));
    }
*/
   // RedisService - dùng userId
   RedisTemplate<String, String> redisTemplate;
   public void setUserOnline(String userId) {
       redisTemplate.opsForValue().set("online:" + userId, "true", 10, TimeUnit.MINUTES);
       log.info("✅ User {} marked ONLINE in Redis", userId);
   }

    public boolean isOnline(String userId) {
        boolean online = Boolean.TRUE.equals(redisTemplate.hasKey("online:" + userId));
        log.info("🔍 Checking online status for userId {}: {}", userId, online);
        return online;
    }
    public void setUserOffline(String userId) {
        redisTemplate.delete("online:" + userId);
    }
}
