package com.blur.communicationservice.websocket.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.blur.communicationservice.notification.entity.Notification;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Push notification realtime den user qua STOMP
     * Client subscribe: /user/queue/notification
     */
    public void sendNotification(Notification notification) {
        messagingTemplate.convertAndSendToUser(notification.getReceiverId(), "/queue/notification", notification);
    }

    /**
     * Push chat message den user qua STOMP
     * Client subscribe: /user/queue/chat/message
     */
    public void sendChatMessage(String userId, Object messagePayload) {
        messagingTemplate.convertAndSendToUser(userId, "/queue/chat/message", messagePayload);
    }

    /**
     * Push chat read receipt
     * Client subscribe: /user/queue/chat/read
     */
    public void sendChatReadReceipt(String userId, Object receiptPayload) {
        messagingTemplate.convertAndSendToUser(userId, "/queue/chat/read", receiptPayload);
    }

    /**
     * Push typing indicator
     * Client subscribe: /user/queue/chat/typing
     */
    public void sendTypingIndicator(String userId, Object typingData) {
        messagingTemplate.convertAndSendToUser(userId, "/queue/chat/typing", typingData);
    }

    /**
     * Push call signaling event
     * Client subscribe: /user/queue/call
     */
    public void sendCallEvent(String userId, Object callData) {
        messagingTemplate.convertAndSendToUser(userId, "/queue/call", callData);
    }

    /**
     * Push WebRTC signaling
     * Client subscribe: /user/queue/webrtc
     */
    public void sendWebRTCSignal(String userId, Object signalData) {
        messagingTemplate.convertAndSendToUser(userId, "/queue/webrtc", signalData);
    }

    /**
     * Push moderation update realtime den user qua STOMP
     * Client subscribe: /user/queue/moderation
     */
    public void sendModerationUpdate(String userId, Object moderationData) {
        messagingTemplate.convertAndSendToUser(userId, "/queue/moderation", moderationData);
    }
}
