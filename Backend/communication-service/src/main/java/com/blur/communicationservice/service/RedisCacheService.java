package com.blur.communicationservice.service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisCallback;
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
public class RedisCacheService {

    // Cache key prefixes
    private static final String CALL_STATE_PREFIX = "chat-service:call:state:";
    private static final String USER_CALL_PREFIX = "chat-service:user:call:";
    private static final String CALL_HISTORY_PREFIX = "chat-service:call:history:";
    private static final String MISSED_CALLS_PREFIX = "chat-service:call:missed:";
    private static final String USER_STATUS_PREFIX = "chat-service:user:status:";
    private static final String MESSAGE_PREFIX = "chat-service:message:";
    private static final String CONVERSATION_PREFIX = "chat-service:conversation:";
    private static final String UNREAD_COUNT_PREFIX = "chat-service:unread:";
    private static final String SESSION_PREFIX = "chat-service:session:";
    private static final String USER_SESSIONS_PREFIX = "chat-service:user:sessions:";
    private static final long SESSION_TTL = 7200; // 2 hours in seconds
    private static final String USER_SOCKET_PREFIX = "chat-service:socket:user:";
    private static final String PROCESSED_MESSAGE_PREFIX = "chat-service:processed:msg:";
    private static final String LAST_MESSAGE_PREFIX = "chat-service:lastmsg:";
    RedisTemplate<String, Object> redisTemplate;

    // ==================== USER CALL STATUS ====================

    public void cacheCallSession(String callId, Object session, long ttlSeconds) {
        try {
            String key = CALL_STATE_PREFIX + callId;
            redisTemplate.opsForValue().set(key, session, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
        }
    }

    public <T> T getCallSession(String callId, Class<T> type) {
        try {
            String key = CALL_STATE_PREFIX + callId;
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? type.cast(value) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public void deleteCallSession(String callId) {
        try {
            String key = CALL_STATE_PREFIX + callId;
            redisTemplate.delete(key);
        } catch (Exception e) {
            // Silent fail
        }
    }

    public void markUserInCall(String userId, String callId, long ttlSeconds) {
        try {
            String key = USER_CALL_PREFIX + userId;
            redisTemplate.opsForValue().set(key, callId, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            // Silent fail
        }
    }

    // ==================== CALL HISTORY CACHE ====================

    public boolean isUserInCall(String userId) {
        try {
            String key = USER_CALL_PREFIX + userId;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            return false;
        }
    }

    public String getUserCurrentCallId(String userId) {
        try {
            String key = USER_CALL_PREFIX + userId;
            Object callId = redisTemplate.opsForValue().get(key);
            return callId != null ? callId.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public void removeUserFromCall(String userId) {
        try {
            String key = USER_CALL_PREFIX + userId;
            redisTemplate.delete(key);
        } catch (Exception e) {
            // Silent fail
        }
    }

    // ==================== MISSED CALLS ====================

    public void cacheCallHistory(String userId, Object history, int page) {
        try {
            String key = CALL_HISTORY_PREFIX + userId + ":page:" + page;
            redisTemplate.opsForValue().set(key, history, 10, TimeUnit.MINUTES);
        } catch (Exception e) {
            // Silent fail
        }
    }

    public <T> T getCallHistory(String userId, int page, Class<T> type) {
        try {
            String key = CALL_HISTORY_PREFIX + userId + ":page:" + page;
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? type.cast(value) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public void invalidateCallHistory(String userId) {
        try {
            String pattern = CALL_HISTORY_PREFIX + userId + ":page:*";
            deleteByPattern(pattern);
        } catch (Exception e) {
            // Silent fail
        }
    }

    // ==================== USER STATUS ====================

    public void incrementMissedCalls(String userId) {
        try {
            String key = MISSED_CALLS_PREFIX + userId;
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, 24, TimeUnit.HOURS);
        } catch (Exception e) {
            // Silent fail
        }
    }

    public long getMissedCallCount(String userId) {
        try {
            String key = MISSED_CALLS_PREFIX + userId;
            Object count = redisTemplate.opsForValue().get(key);
            return count != null ? Long.parseLong(count.toString()) : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // ==================== MESSAGE CACHE ====================

    public void resetMissedCalls(String userId) {
        try {
            String key = MISSED_CALLS_PREFIX + userId;
            redisTemplate.delete(key);
        } catch (Exception e) {
            // Silent fail
        }
    }

    public void setUserOnlineStatus(String userId, boolean isOnline) {
        try {
            String key = USER_STATUS_PREFIX + userId;
            redisTemplate.opsForValue().set(key, isOnline ? "online" : "offline", 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            // Silent fail
        }
    }

    public boolean isUserOnline(String userId) {
        try {
            String key = USER_STATUS_PREFIX + userId;
            Object status = redisTemplate.opsForValue().get(key);
            return "online".equals(status);
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== CONVERSATION CACHE ====================

    public void cacheMessage(String messageId, Object message, long ttlMinutes) {
        try {
            String key = MESSAGE_PREFIX + messageId;
            redisTemplate.opsForValue().set(key, message, ttlMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            // Silent fail
        }
    }

    public <T> T getMessage(String messageId, Class<T> type) {
        try {
            String key = MESSAGE_PREFIX + messageId;
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? type.cast(value) : null;
        } catch (Exception e) {
            log.warn("Failed to get message from cache: key={}, error={}", MESSAGE_PREFIX + messageId, e.getMessage());
            return null;
        }
    }

    public void invalidateConversationMessages(String conversationId) {
        try {
            String pattern = MESSAGE_PREFIX + conversationId + ":*";
            deleteByPattern(pattern);
        } catch (Exception e) {
            // Silent fail
        }
    }

    public void cacheConversation(String conversationId, Object conversation, long ttlMinutes) {
        try {
            String key = CONVERSATION_PREFIX + conversationId;
            redisTemplate.opsForValue().set(key, conversation, ttlMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            // Silent fail
        }
    }

    // ==================== UNREAD COUNT CACHE ====================

    public <T> T getConversation(String conversationId, Class<T> type) {
        try {
            String key = CONVERSATION_PREFIX + conversationId;
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? type.cast(value) : null;
        } catch (Exception e) {
            return null;
        }
    }

    public void evictConversation(String conversationId) {
        try {
            String key = CONVERSATION_PREFIX + conversationId;
            redisTemplate.delete(key);
        } catch (Exception e) {
            // Silent fail
        }
    }

    public void evictUserConversations(String userId) {
        try {
            String pattern = CONVERSATION_PREFIX + "*:" + userId + ":*";
            deleteByPattern(pattern);
        } catch (Exception e) {
            // Silent fail
        }
    }

    // ==================== WEBSOCKET SESSION CACHE ====================

    public void cacheUnreadCount(String conversationId, String userId, int count) {
        try {
            String key = UNREAD_COUNT_PREFIX + conversationId + ":" + userId;
            redisTemplate.opsForValue().set(key, count, 30, TimeUnit.MINUTES);
        } catch (Exception e) {
            // Silent fail
        }
    }

    public Integer getUnreadCount(String conversationId, String userId) {
        try {
            String key = UNREAD_COUNT_PREFIX + conversationId + ":" + userId;
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? Integer.parseInt(value.toString()) : null;
        } catch (Exception e) {
            log.warn(
                    "Failed to get unread count from cache: conversationId={}, userId={}, error={}",
                    conversationId,
                    userId,
                    e.getMessage());
            return null;
        }
    }

    public void evictUnreadCount(String conversationId, String userId) {
        try {
            String key = UNREAD_COUNT_PREFIX + conversationId + ":" + userId;
            redisTemplate.delete(key);
        } catch (Exception e) {
            // Silent fail
        }
    }

    public void cacheSession(String sessionId, String userId, long ttlMinutes) {
        try {
            String key = SESSION_PREFIX + sessionId;
            redisTemplate.opsForValue().set(key, userId, ttlMinutes, TimeUnit.MINUTES);

            // Also add to user's session set
            String userSessionsKey = USER_SESSIONS_PREFIX + userId;
            redisTemplate.opsForSet().add(userSessionsKey, sessionId);
            redisTemplate.expire(userSessionsKey, ttlMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            // Silent fail
        }
    }

    public String getSessionUserId(String sessionId) {
        try {
            String key = SESSION_PREFIX + sessionId;
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public Set<Object> getUserSessions(String userId) {
        try {
            String key = USER_SESSIONS_PREFIX + userId;
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            return Set.of();
        }
    }

    public void removeSession(String sessionId, String userId) {
        try {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                String sessionKey = SESSION_PREFIX + sessionId;
                redisTemplate.delete(sessionKey);

                if (userId != null) {
                    String userSessionsKey = USER_SESSIONS_PREFIX + userId;
                    redisTemplate.opsForSet().remove(userSessionsKey, sessionId);
                }

                return null;
            });
        } catch (Exception e) {
            // Silent fail
        }
    }

    public long getUserActiveSessionCount(String userId) {
        try {
            String key = USER_SESSIONS_PREFIX + userId;
            Long size = redisTemplate.opsForSet().size(key);
            return size != null ? size : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    public void cleanupCallCaches(String callId, String callerId, String receiverId) {
        try {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                deleteCallSession(callId);
                removeUserFromCall(callerId);
                removeUserFromCall(receiverId);
                invalidateCallHistory(callerId);
                invalidateCallHistory(receiverId);
                return null;
            });
        } catch (Exception e) {
        }
    }

    private void deleteByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
        }
    }

    public Set<String> getAllActiveCalls() {
        try {
            String pattern = CALL_STATE_PREFIX + "*";
            return redisTemplate.keys(pattern);
        } catch (Exception e) {
            return Set.of();
        }
    }

    public Set<String> getAllUsersInCalls() {
        try {
            String pattern = USER_CALL_PREFIX + "*";
            return redisTemplate.keys(pattern);
        } catch (Exception e) {
            return Set.of();
        }
    }

    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Cache user socket mapping
     * Used for routing messages to specific user
     */
    public void cacheUserSocket(String userId, String socketId) {
        try {
            String key = USER_SOCKET_PREFIX + userId;
            redisTemplate.opsForValue().set(key, socketId, SESSION_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            // Silent fail
        }
    }

    /**
     * Get user's socket ID
     */
    public String getUserSocket(String userId) {
        try {
            String key = USER_SOCKET_PREFIX + userId;
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Remove user socket mapping
     */
    public void removeUserSocket(String userId) {
        try {
            String key = USER_SOCKET_PREFIX + userId;
            redisTemplate.delete(key);
        } catch (Exception e) {
            // Silent fail
        }
    }

    /**
     * Add session to user's session set
     */
    public void addUserSession(String userId, String sessionId) {
        try {
            String key = USER_SESSIONS_PREFIX + userId;
            redisTemplate.opsForSet().add(key, sessionId);
            redisTemplate.expire(key, SESSION_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            // Silent fail
        }
    }

    /**
     * Remove session from user's session set
     */
    public void removeUserSession(String userId, String sessionId) {
        try {
            String key = USER_SESSIONS_PREFIX + userId;
            redisTemplate.opsForSet().remove(key, sessionId);
        } catch (Exception e) {
            // Silent fail
        }
    }

    /**
     * Check if message was already processed (deduplication)
     */
    public boolean isMessageProcessed(String messageKey) {
        try {
            String key = PROCESSED_MESSAGE_PREFIX + messageKey;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Mark message as processed with TTL
     */
    public void markMessageAsProcessed(String messageKey, long ttlMillis) {
        try {
            String key = PROCESSED_MESSAGE_PREFIX + messageKey;
            redisTemplate.opsForValue().set(key, "1", ttlMillis, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            // Silent fail
        }
    }

    public void cacheLastMessage(String conversationId, Object lastMessage, long ttlMinutes) {
        try {
            String key = LAST_MESSAGE_PREFIX + conversationId;
            redisTemplate.opsForValue().set(key, lastMessage, ttlMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            // Silent fail
        }
    }

    public <T> T getLastMessage(String conversationId, Class<T> type) {
        try {
            String key = LAST_MESSAGE_PREFIX + conversationId;
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? type.cast(value) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Evict last message cache
     */
    public void evictLastMessage(String conversationId) {
        try {
            String key = LAST_MESSAGE_PREFIX + conversationId;
            redisTemplate.delete(key);
        } catch (Exception e) {
            // Silent fail
        }
    }
}
