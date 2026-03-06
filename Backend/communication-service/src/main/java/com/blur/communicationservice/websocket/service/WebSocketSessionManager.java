package com.blur.communicationservice.websocket.service;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.blur.communicationservice.service.RedisCacheService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WebSocketSessionManager {

    private final RedisCacheService redisCacheService;

    public void registerSession(String userId, String sessionId) {
        redisCacheService.addUserSession(userId, sessionId);
        redisCacheService.cacheUserSocket(userId, sessionId);
    }

    public void removeSession(String userId, String sessionId) {
        redisCacheService.removeUserSession(userId, sessionId);
        // Cap nhat socket ID noi bat nhat
        Set<Object> remaining = redisCacheService.getUserSessions(userId);
        if (remaining != null && !remaining.isEmpty()) {
            redisCacheService.cacheUserSocket(
                    userId, remaining.iterator().next().toString());
        } else {
            redisCacheService.removeUserSocket(userId);
        }
    }

    public boolean hasActiveSessions(String userId) {
        return redisCacheService.getUserActiveSessionCount(userId) > 0;
    }

    public boolean isUserOnline(String userId) {
        return redisCacheService.isUserOnline(userId);
    }
}
