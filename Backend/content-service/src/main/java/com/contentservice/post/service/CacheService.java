package com.contentservice.post.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CacheService {
  RedisTemplate<String, Object> redisTemplate;

  private Set<String> scanKeys(String pattern) {
    Set<String> keys = new HashSet<>();
    ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();
    try (Cursor<String> cursor = redisTemplate.scan(options)) {
      while (cursor.hasNext()) {
        keys.add(cursor.next());
      }
    }
    return keys;
  }

  private void deleteByPattern(String pattern) {
    try {
      Set<String> keys = scanKeys(pattern);
      if (!keys.isEmpty()) {
        redisTemplate.unlink(keys);
      }
    } catch (Exception e) {
      log.warn("Failed to delete cache by pattern {}: {}", pattern, e.getMessage());
    }
  }

  public void evictPostCache(String postId, String userId) {
    try {
      deleteByPattern("content-service:post::" + postId);
      deleteByPattern("content-service:userPosts::" + userId + "*");
      deleteByPattern("content-service:posts::*");
    } catch (Exception e) {
      log.warn("Failed to evict post caches postId={}, userId={}: {}", postId, userId, e.getMessage());
    }
  }

  public void evictPostLikeCache(String postId) {
    try {
      deleteByPattern("content-service:postLikes::" + postId);
      deleteByPattern("content-service:post::" + postId);
    } catch (Exception e) {
      log.warn("Failed to evict post like cache postId={}: {}", postId, e.getMessage());
    }
  }

  public void evictSavedPostsCache(String userId) {
    try {
      deleteByPattern("content-service:savedPosts::" + userId);
    } catch (Exception e) {
      log.warn("Failed to evict posts caches userId={}: {}", userId, e.getMessage());
    }
  }

  public void evictCommentCaches(String postId, String commentId) {
    try {
      deleteByPattern("content-service:comments::" + postId);
      deleteByPattern("content-service:commentReplies::" + commentId);
    } catch (Exception e) {
      log.warn("Failed to evict comment caches postId={}, commentId={}: {}", postId, commentId, e.getMessage());
    }
  }

  public void evictCommentReplyCaches(String commentId, String replyId, String parentReplyId) {
    try {
      deleteByPattern("content-service:commentReplies::" + commentId);
      if (replyId != null) {
        deleteByPattern("content-service:commentReplyById::" + commentId);
      }
      if (parentReplyId != null) {
        deleteByPattern("content-service:nestedReplies::" + parentReplyId);
      }
    } catch (Exception e) {
      log.warn("Failed to evict comment reply caches commentId={}: {}", commentId, e.getMessage());
    }
  }

  public void set(String key, Object value, long timeout, TimeUnit unit) {
    try {
      redisTemplate.opsForValue().set(key, value, timeout, unit);
    } catch (Exception e) {
      log.warn("Failed to set cache key={}: {}", key, e.getMessage());
    }
  }

  public Object get(String key) {
    try {
      return redisTemplate.opsForValue().get(key);
    } catch (Exception e) {
      log.warn("Failed to get cache key={}: {}", key, e.getMessage());
      return null;
    }
  }

  public void delete(String key) {
    try {
      redisTemplate.unlink(key);
    } catch (Exception e) {
      log.warn("Failed to delete cache key={}: {}", key, e.getMessage());
    }
  }

  public long getCacheSize() {
    try {
      return scanKeys("content:service:*").size();
    } catch (Exception e) {
      log.warn("Failed to get cache size: {}", e.getMessage());
      return -1;
    }
  }

  public long getCacheSizeByPattern(String pattern) {
    try {
      return scanKeys("content-service:" + pattern).size();
    } catch (Exception e) {
      log.warn("Failed to get cache size by pattern={}: {}", pattern, e.getMessage());
      return -1;
    }
  }

  public void warmUp(String cacheName, String key, Object value) {
    try {
      String fullKey = "content-service:" + cacheName + "::" + key;
      redisTemplate.opsForValue().set(fullKey, value, 5, TimeUnit.MINUTES);
      log.debug("Cache warmed up: {}::{}", cacheName, key);
    } catch (Exception e) {
      log.warn("Failed to warm up cache {}::{}: {}", cacheName, key, e.getMessage());
    }
  }
}