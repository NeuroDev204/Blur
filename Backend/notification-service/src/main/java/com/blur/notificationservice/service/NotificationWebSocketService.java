package com.blur.notificationservice.service;

import com.blur.notificationservice.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationWebSocketService {

  private final SimpMessagingTemplate messagingTemplate;

  public void sendToUser(Notification notification) {
    messagingTemplate.convertAndSendToUser(
        notification.getReceiverId(),
        "/notification",
        notification
    );
    log.info("✅ Realtime notification pushed to user {}", notification.getReceiverId());
  }

  public void pushToUser(String receiverId, Notification notification) {
    messagingTemplate.convertAndSendToUser(
        receiverId,
        "/notification",
        notification
    );
  }
}