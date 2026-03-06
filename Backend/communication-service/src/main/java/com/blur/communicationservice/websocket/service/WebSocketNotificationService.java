package com.blur.communicationservice.websocket.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.blur.communicationservice.notification.entity.Notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Push notification realtime den user qua STOMP
     * Client subscribe: /user/{userId}/notification
     */
    public void sendNotification(Notification notification) {
        messagingTemplate.convertAndSendToUser(notification.getReceiverId(), "/notification", notification);
        log.debug("Realtime notification pushed to user {}", notification.getReceiverId());
    }

    /**
     * Push chat message den user qua STOMP
     * Client subscribe: /user/{userId}/chat/message
     */
    public void sendChatMessage(String userId, Object messagePayload) {
        messagingTemplate.convertAndSendToUser(userId, "/chat/message", messagePayload);
    }

    /**
     * Push typing indicator
     * Client subscribe: /user/{userId}/chat/typing
     */
    public void sendTypingIndicator(String userId, Object typingData) {
        messagingTemplate.convertAndSendToUser(userId, "/chat/typing", typingData);
    }

    /**
     * Push call signaling event
     * Client subscribe: /user/{userId}/call
     */
    public void sendCallEvent(String userId, Object callData) {
        messagingTemplate.convertAndSendToUser(userId, "/call", callData);
    }

    /**
     * Push WebRTC signaling
     * Client subscribe: /user/{userId}/webrtc
     */
    public void sendWebRTCSignal(String userId, Object signalData) {
        messagingTemplate.convertAndSendToUser(userId, "/webrtc", signalData);
    }
}
