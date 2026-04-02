package com.blur.communicationservice.service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
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

    private Set<String> scanKeys(String pattern) {
        Set<String> scanKeys = new HashSet<>();
        ScanOptions options =
                ScanOptions.scanOptions().match(pattern).count(100).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                scanKeys.add(cursor.next());
            }
        }
        return scanKeys;
    }

    private void deleteByPattern(String pattern) {
        try {
            Set<String> keys = scanKeys(pattern);
            if (!keys.isEmpty()) {
                redisTemplate.unlink(keys);
            }
        } catch (Exception e) {
            log.warn("Failed to delete by patter{}: {}", pattern, e.getMessage());
        }
    }

    public void cacheCallSession(String callId, Object session, long ttlSession) {
        try {
            String key = CALL_STATE_PREFIX + callId;
            redisTemplate.opsForValue().set(key, session, ttlSession, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to cache call session callId={}: {}", callId, e.getMessage());
        }
    }

    public <T> T getCallSession(String callId, Class<T> type) {
        try {
            String key = CALL_STATE_PREFIX + callId;
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? type.cast(value) : null;
        } catch (Exception e) {
            log.warn("Failed to get call session callId={}: {}", callId, e.getMessage());
            return null;
        }
    }

    public void deleteCallSession(String callId) {
        try {
            String key = CALL_STATE_PREFIX + callId;
            redisTemplate.unlink(key);
        } catch (Exception e) {
            log.warn("Failed to delete call session callId={}: {}", callId, e.getMessage());
        }
    }

    public void markUserInCall(String userId, String callId, long ttlSeconds) {
        try {
            String key = USER_CALL_PREFIX + userId;
            redisTemplate.opsForValue().set(key, callId, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to mark user in call userId={}: {}", userId, e.getMessage());
        }
    }

    public boolean isUserInCall(String userId) {
        try {
            String key = USER_CALL_PREFIX + userId;
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.warn("Failed to check if user in call userId={}: {}", userId, e.getMessage());
            return false;
        }
    }

    public Set<Object> getUserSessions(String userId) {
        try {
            String key = USER_SESSIONS_PREFIX + userId;
            Set<Object> members = redisTemplate.opsForSet().members(key);
            return members != null ? members : Set.of();
        } catch (Exception e) {
            log.warn("Failed to get user sessions userId={}: {}", userId, e.getMessage());
            return Set.of();
        }
    }

    public boolean isUserOnline(String userId) {
        try {
            String key = USER_STATUS_PREFIX + userId;
            Object value = redisTemplate.opsForValue().get(key);
            return value != null && "online".equals(value.toString());
        } catch (Exception e) {
            log.warn("Failed to check user online status userId={}: {}", userId, e.getMessage());
            return false;
        }
    }

    public String getUserCurrentCallId(String userId) {
        try {
            String key = USER_CALL_PREFIX + userId;
            Object callId = redisTemplate.opsForValue().get(key);
            return callId != null ? callId.toString() : null;
        } catch (Exception e) {
            log.warn("Failed to get user current call userId={}: {}", userId, e.getMessage());
            return null;
        }
    }

    public void removeUserFromCall(String userId) {
        try {
            String key = USER_CALL_PREFIX + userId;
            redisTemplate.unlink(key);
        } catch (Exception e) {
            log.warn("Failed to remove user from call userId={}: {}", userId, e.getMessage());
        }
    }

    public void cacheCallHistory(String userId, Object history, int page) {
        try {
            String key = CALL_STATE_PREFIX + userId + ":page:" + page;
            redisTemplate.opsForValue().set(key, history, 10, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Failed to cache call history userId={}, page={}: {}", userId, page, e.getMessage());
        }
    }

    public <T> T getCallHistory(String userId, int page, Class<T> type) {
        try {
            String key = CALL_HISTORY_PREFIX + userId + ":page:" + page;
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? type.cast(value) : null;
        } catch (Exception e) {
            log.warn("Failed to get call history userId={}, page={}: {}", userId, page, e.getMessage());
            return null;
        }
    }

    public void invalidateCallHistory(String userId) {
        try {
            String pattern = CALL_HISTORY_PREFIX + userId + ":page:*";
            deleteByPattern(pattern);
        } catch (Exception e) {
            log.warn("Failed to invalidate call history userId={}: {}", userId, e.getMessage());
        }
    }

    public void incrementMissedCalls(String userId) {
        try {
            String key = MISSED_CALLS_PREFIX + userId;
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to increment missed call userId={}: {}", userId, e.getMessage());
        }
    }

    public long getMissedCallCount(String userId) {
        try {
            String key = MISSED_CALLS_PREFIX + userId;
            Object count = redisTemplate.opsForValue().get(key);
            return count != null ? Long.parseLong(count.toString()) : 0;
        } catch (Exception e) {
            log.warn("Failed to get missed call count userId={}: {}", userId, e.getMessage());
            return 0;
        }
    }

    public void resetMissedCalls(String userId) {
        try {
            String key = MISSED_CALLS_PREFIX + userId;
            redisTemplate.unlink(key);
        } catch (Exception e) {
            log.warn("Failed to reset missed calls userId={}: {}", userId, e.getMessage());
        }
    }

    public void setUserOnlineStatus(String userId, boolean isOnline) {
        try {
            String key = USER_STATUS_PREFIX + userId;
            redisTemplate.opsForValue().set(key, isOnline ? "online" : "offline", 5, TimeUnit.MINUTES);

        } catch (Exception e) {
            log.warn("Failed to set user online status userId={}: {}", userId, e.getMessage());
        }
    }

    public void cacheMessage(String messageId, Object message, long ttlMinutes) {
        try {
            String key = MISSED_CALLS_PREFIX + messageId;
            redisTemplate.opsForValue().set(key, message, ttlMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Failed to cache message messageId={}: {}", messageId, e.getMessage());
        }
    }

    public <T> T getMessage(String messageId, Class<T> type) {
        try {
            String key = MESSAGE_PREFIX + messageId;
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? type.cast(value) : null;
        } catch (Exception e) {
            log.warn("Failed to get message messageId={}: {}", messageId, e.getMessage());
            return null;
        }
    }

    public void invalidateConversationMessages(String conversationId) {
        try {
            String pattern = MESSAGE_PREFIX + conversationId + ":*";
            deleteByPattern(pattern);
        } catch (Exception e) {
            log.warn(
                    "Failed to invalidate conversation messages conversationIds={}: {}",
                    conversationId,
                    e.getMessage());
        }
    }

    public void cacheConversation(String conversationId, Object conversation, long ttlMinutes) {
        try {
            String key = CONVERSATION_PREFIX + conversationId;
            redisTemplate.opsForValue().set(key, conversation, ttlMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Failed to cache conversation conversationId={}: {}", conversationId, e.getMessage());
        }
    }

    public void evictConversation(String conversationId) {
        try {
            String key = CONVERSATION_PREFIX + conversationId;
            redisTemplate.unlink(key);
        } catch (Exception e) {
            log.warn("Failed to evict conversation conversationId={}: {}", conversationId, e.getMessage());
        }
    }

    public void evictUserConversations(String userId) {
        try {
            String pattern = CONVERSATION_PREFIX + "*:" + userId + ":*";
            deleteByPattern(pattern);
        } catch (Exception e) {
            log.warn("Failed to evict user conversations userId={}: {}", userId, e.getMessage());
        }
    }

    public void cacheUnreadCount(String conversationId, String userId, int count) {
        try {
            String key = UNREAD_COUNT_PREFIX + conversationId + ":" + userId;
            redisTemplate.opsForValue().set(key, count, 30, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn(
                    "Failed to cache unread count conversationId={}, userId={}: {}",
                    conversationId,
                    userId,
                    e.getMessage());
        }
    }

    public Integer getUnreadCount(String conversationId, String userId) {
        try {
            String key = UNREAD_COUNT_PREFIX + conversationId + ":" + userId;
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? Integer.parseInt(value.toString()) : null;
        } catch (Exception e) {
            log.warn(
                    "Failed to get unread count conversationId={}, userId={}: {}",
                    conversationId,
                    userId,
                    e.getMessage());
            return null;
        }
    }

    public void evictUnreadCount(String conversationId, String userId) {
        try {
            String key = UNREAD_COUNT_PREFIX + conversationId + ":" + userId;
            redisTemplate.unlink(key);
        } catch (Exception e) {
            log.warn(
                    "Failed to evict unread count conversationId={}, userId={}: {}",
                    conversationId,
                    userId,
                    e.getMessage());
        }
    }

    public void cacheSession(String sessionId, String userId, long ttlMinutes) {
        try {
            String key = SESSION_PREFIX + sessionId;
            redisTemplate.opsForValue().set(key, userId, ttlMinutes, TimeUnit.MINUTES);

            String userSessionKey = USER_SESSIONS_PREFIX + userId;
            redisTemplate.opsForSet().add(userSessionKey, sessionId);
            redisTemplate.expire(userSessionKey, ttlMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Failed to cache session sessionId={}, userId={}: {}", sessionId, userId, e.getMessage());
        }
    }

    public String getSessionUserId(String sessionId) {
        try {
            String key = SESSION_PREFIX + sessionId;
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.warn("Failed to get session userId sessionId={}: {}", sessionId, e.getMessage());
            return null;
        }
    }

    public Set<Object> getUserSession(String userId) {
        try {
            String key = USER_SESSIONS_PREFIX + userId;
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            log.warn("Failed to get user sessions userId={}: {}", userId, e.getMessage());
            return Set.of();
        }
    }

    public void removeSession(String sessionId, String userId) {
        try {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                String sessionKey = SESSION_PREFIX + sessionId;
                redisTemplate.unlink(sessionKey);

                if (userId != null) {
                    String userSessionKey = USER_SESSIONS_PREFIX + userId;
                    redisTemplate.opsForSet().remove(userSessionKey, sessionId);
                }
                return null;
            });
        } catch (Exception e) {
            log.warn("Failed to remove session sessionid={}, userId={}: {}", sessionId, userId, e.getMessage());
        }
    }

    public long getUserActiveSessionCount(String userId) {
        try {
            String key = USER_SESSIONS_PREFIX + userId;
            Long size = redisTemplate.opsForSet().size(key);
            return size != null ? size : 0;
        } catch (Exception e) {
            log.warn("Failed to get user active session count userId={}: {}", userId, e.getMessage());
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
            log.warn("Failed to clean up call caches callId={}: {}", callId, e.getMessage());
        }
    }

    public Set<String> getAllActiveCalls() {
        try {
            return scanKeys(CALL_STATE_PREFIX + "*");
        } catch (Exception e) {
            log.warn("Failed to get all active calls: {}", e.getMessage());
            return Set.of();
        }
    }

    public Set<String> getAllUsersInCalls() {
        try {
            return scanKeys(USER_CALL_PREFIX + "*");
        } catch (Exception e) {
            log.warn("Failed to get all users in calls: {}", e.getMessage());
            return Set.of();
        }
    }

    public boolean exists(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            return false;
        }
    }

    public void cacheUserSocket(String userId, String socketId) {
        try {
            String key = USER_SOCKET_PREFIX + userId;
            redisTemplate.opsForValue().set(key, socketId, SESSION_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to cache user socket userId={}: {}", userId, e.getMessage());
        }
    }

    public String getUserSocket(String userId) {
        try {
            String key = USER_SOCKET_PREFIX + userId;
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.warn("Failed to get user socket userId={}: {}", userId, e.getMessage());
            return null;
        }
    }

    public void removeUserSocket(String userId) {
        try {
            String key = USER_SOCKET_PREFIX + userId;
            redisTemplate.unlink(key);
        } catch (Exception e) {
            log.warn("Failed to remove user socket userId={}: {}", userId, e.getMessage());
        }
    }

    public void addUserSession(String userId, String sessionId) {
        try {
            String key = USER_SOCKET_PREFIX + userId;
            redisTemplate.opsForSet().add(key, sessionId);
            redisTemplate.expire(key, SESSION_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to add user session userId={}, sessionId={}: {}", userId, sessionId, e.getMessage());
        }
    }

    public void removeUserSession(String userId, String sessionId) {
        try {
            String key = USER_SESSIONS_PREFIX + userId;
            redisTemplate.opsForSet().remove(key, sessionId);
        } catch (Exception e) {
            log.warn("Failed to remove user session userId={}, sessionId={}: {}", userId, sessionId, e.getMessage());
        }
    }

    public boolean isMessageProcessed(String messageKey) {
        try {
            String key = PROCESSED_MESSAGE_PREFIX + messageKey;
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            log.warn("Failed to check message processed messageKey={}: {}", messageKey, e.getMessage());
            return false;
        }
    }

    public void markMessageAsProcessed(String messageKey, long ttlMinutes) {
        try {
            String key = PROCESSED_MESSAGE_PREFIX + messageKey;
            redisTemplate.opsForValue().set(key, "1", ttlMinutes, TimeUnit.MICROSECONDS);
        } catch (Exception e) {
            log.warn("Failed to mark message as processed messageKey={}: {}", messageKey, e.getMessage());
        }
    }

    public void cacheLastMessage(String conversationId, Object lastMessage, long ttlMinutes) {
        try {
            String key = LAST_MESSAGE_PREFIX + conversationId;
            redisTemplate.opsForValue().set(key, lastMessage, ttlMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Failed to cache last message conversationId={}: {}", conversationId, e.getMessage());
        }
    }

    public <T> T getLastMessage(String conversationId, Class<T> type) {
        try {
            String key = LAST_MESSAGE_PREFIX + conversationId;
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? type.cast(value) : null;
        } catch (Exception e) {
            log.warn("Failed to get last message conversationId={}: {}", conversationId, e.getMessage());
            return null;
        }
    }

    public void evictLastMessage(String conversationId) {
        try {
            String key = LAST_MESSAGE_PREFIX + conversationId;
            redisTemplate.unlink(key);
        } catch (Exception e) {
            log.warn("Failed to evict last message conversationId={}: {}", conversationId, e.getMessage());
        }
    }
}
