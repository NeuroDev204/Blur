package com.postservice.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CacheService {

    RedisTemplate<String, Object> redisTemplate;

    public void evictPostCaches(String postId, String userId) {
        try {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                // Get keys matching patterns
                Set<String> postKeys = redisTemplate.keys("post-service:post::" + postId);
                Set<String> userPostKeys = redisTemplate.keys("post-service:userPosts::" + userId + "*");
                Set<String> allPostsKeys = redisTemplate.keys("post-service:posts::*");

                // Delete all keys in batch
                if (postKeys != null && !postKeys.isEmpty()) {
                    redisTemplate.delete(postKeys);
                }
                if (userPostKeys != null && !userPostKeys.isEmpty()) {
                    redisTemplate.delete(userPostKeys);
                }
                if (allPostsKeys != null && !allPostsKeys.isEmpty()) {
                    redisTemplate.delete(allPostsKeys);
                }

                return null;
            });
        } catch (Exception e) {

        }
    }

    public void evictPostLikeCache(String postId) {
        try {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                Set<String> likeKeys = redisTemplate.keys("post-service:postLikes::" + postId);
                Set<String> postKeys = redisTemplate.keys("post-service:post::" + postId);

                if (likeKeys != null && !likeKeys.isEmpty()) {
                    redisTemplate.delete(likeKeys);
                }
                if (postKeys != null && !postKeys.isEmpty()) {
                    redisTemplate.delete(postKeys);
                }

                return null;
            });
        } catch (Exception e) {

        }
    }

    public void evictSavedPostsCache(String userId) {
        try {
            deleteByPattern("post-service:savedPosts::" + userId);
        } catch (Exception e) {
        }
    }

    public void evictCommentCaches(String postId, String commentId) {
        try {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                Set<String> commentKeys = redisTemplate.keys("post-service:comments::" + postId);
                Set<String> replyKeys = redisTemplate.keys("post-service:commentReplies::" + commentId);

                if (commentKeys != null && !commentKeys.isEmpty()) {
                    redisTemplate.delete(commentKeys);
                }
                if (replyKeys != null && !replyKeys.isEmpty()) {
                    redisTemplate.delete(replyKeys);
                }

                return null;
            });
        } catch (Exception e) {
        }
    }

    public void evictCommentReplyCaches(String commentId, String replyId, String parentReplyId) {
        try {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                Set<String> replyListKeys = redisTemplate.keys("post-service:commentReplies::" + commentId);

                if (replyListKeys != null && !replyListKeys.isEmpty()) {
                    redisTemplate.delete(replyListKeys);
                }

                if (replyId != null) {
                    Set<String> singleReplyKeys = redisTemplate.keys("post-service:commentReplyById::" + replyId);
                    if (singleReplyKeys != null && !singleReplyKeys.isEmpty()) {
                        redisTemplate.delete(singleReplyKeys);
                    }
                }

                if (parentReplyId != null) {
                    Set<String> nestedKeys = redisTemplate.keys("post-service:nestedReplies::" + parentReplyId);
                    if (nestedKeys != null && !nestedKeys.isEmpty()) {
                        redisTemplate.delete(nestedKeys);
                    }
                }

                return null;
            });
        } catch (Exception e) {

        }
    }

    private void deleteByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        } catch (Exception e) {
            // Silent fail
        }
    }

    public Object get(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            return false;
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
        }
    }

    public long getCacheSize() {
        try {
            Set<String> keys = redisTemplate.keys("post-service:*");
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            return -1;
        }
    }

    public long getCacheSizeByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys("post-service:" + pattern);
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            return -1;
        }
    }

    public void clearAllCaches() {
        try {
            Set<String> keys = redisTemplate.keys("post-service:*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
        }
    }

    public void warmUp(String cacheName, String key, Object value) {
        try {
            String fullKey = "post-service:" + cacheName + "::" + key;
            redisTemplate.opsForValue().set(fullKey, value, 5, TimeUnit.MINUTES);
        } catch (Exception e) {
        }
    }
}