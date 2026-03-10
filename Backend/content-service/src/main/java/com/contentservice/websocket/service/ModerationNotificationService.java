package com.contentservice.websocket.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ModerationNotificationService {

  private final SimpMessagingTemplate messagingTemplate;
  private final ObjectMapper objectMapper;

  /**
   * Send moderation update to user via WebSocket
   * Frontend subscribes to: /user/queue/moderation
   */
  public void sendModerationUpdate(String userId, String commentId, String postId, String status) {
    try {
      var payload = new java.util.HashMap<String, Object>();
      payload.put("commentId", commentId);
      payload.put("postId", postId);
      payload.put("status", status);
      payload.put("timestamp", System.currentTimeMillis());

      messagingTemplate.convertAndSendToUser(userId, "/queue/moderation", payload);
    } catch (Exception e) {
      // Log error but don't fail the main process
      System.err.println("Failed to send moderation update: " + e.getMessage());
    }
  }
}
