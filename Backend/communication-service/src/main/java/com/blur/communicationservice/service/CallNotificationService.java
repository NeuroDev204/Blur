package com.blur.communicationservice.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.blur.communicationservice.enums.CallType;
import com.blur.communicationservice.notification.entity.Notification;
import com.blur.communicationservice.notification.enums.NotificationType;
import com.blur.communicationservice.websocket.service.WebSocketNotificationService;
import com.blur.communicationservice.websocket.service.WebSocketSessionManager;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service("callNotificationService")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CallNotificationService {

    WebSocketNotificationService webSocketNotificationService;
    WebSocketSessionManager sessionManager;

    /**
     * Push notification khi co cuoc goi den qua STOMP WebSocket
     */
    public void sendIncomingCallNotification(String userId, String callerName, CallType callType) {
        log.info("Sending incoming call notification to user: {}, caller: {}, type: {}", userId, callerName, callType);

        Notification notification = Notification.builder()
                .receiverId(userId)
                .senderName(callerName)
                .type(NotificationType.Message)
                .content(callerName + " is calling you (" + callType.name() + ")")
                .timestamp(LocalDateTime.now())
                .read(false)
                .build();

        if (sessionManager.isUserOnline(userId)) {
            webSocketNotificationService.sendNotification(notification);
        }
    }

    /**
     * Push notification khi nho cuoc goi
     */
    public void sendMissedCallNotification(String userId, String callerName, CallType callType) {
        log.info("Sending missed call notification to user: {}, caller: {}, type: {}", userId, callerName, callType);

        Notification notification = Notification.builder()
                .receiverId(userId)
                .senderName(callerName)
                .type(NotificationType.Message)
                .content("Missed " + callType.name().toLowerCase() + " call from " + callerName)
                .timestamp(LocalDateTime.now())
                .read(false)
                .build();

        if (sessionManager.isUserOnline(userId)) {
            webSocketNotificationService.sendNotification(notification);
        }
    }
}
