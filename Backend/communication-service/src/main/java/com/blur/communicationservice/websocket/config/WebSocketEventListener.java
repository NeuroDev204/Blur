package com.blur.communicationservice.websocket.config;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.blur.communicationservice.service.RedisCacheService;
import com.blur.communicationservice.websocket.service.WebSocketSessionManager;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final WebSocketSessionManager sessionManager;
    private final RedisCacheService redisCacheService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String userId = accessor.getUser() != null ? accessor.getUser().getName() : null;

        if (userId != null) {
            sessionManager.registerSession(userId, sessionId);
            redisCacheService.setUserOnlineStatus(userId, true);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String userId = accessor.getUser() != null ? accessor.getUser().getName() : null;

        if (userId != null) {
            sessionManager.removeSession(userId, sessionId);
            // Offline chi khi khong con session nao
            if (!sessionManager.hasActiveSessions(userId)) {
                redisCacheService.setUserOnlineStatus(userId, false);
            }
        }
    }
}
