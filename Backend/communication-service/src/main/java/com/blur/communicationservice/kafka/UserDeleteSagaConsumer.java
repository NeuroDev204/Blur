package com.blur.communicationservice.kafka;

import java.time.Instant;
import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.blur.communicationservice.ai.repository.AiConversationRepository;
import com.blur.communicationservice.chat.repository.ChatMessageRepository;
import com.blur.communicationservice.chat.repository.ConversationRepository;
import com.blur.communicationservice.notification.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PACKAGE, makeFinal = true)
public class UserDeleteSagaConsumer {
    ChatMessageRepository chatMessageRepository;
    ConversationRepository conversationRepository;
    NotificationRepository notificationRepository;
    AiConversationRepository aiConversationRepository;
    KafkaTemplate<String, String> kafkaTemplate;
    ObjectMapper objectMapper;

    @KafkaListener(topics = "user-delete-saga", groupId = "communication-service-saga")
    public void handleSagaEvent(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, Map.class);
            String step = (String) event.get("step");
            String userId = (String) event.get("userId");
            if (!"CONTENT_CLEANED".equals(step)) return;
            // xoa messages, notifications, ai conversation
            chatMessageRepository.deleteBySenderUserId(userId);
            notificationRepository.deleteByReceiverId(userId);
            aiConversationRepository.deleteByUserId(userId);

            event.put("step", "COMMUNICATION_CLEANED");
            event.put("timestamp", Instant.now().toString());
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("user-delete-saga", userId, json);
        } catch (Exception e) {
            publishFailure(message, e.getMessage());
        }
    }

    private void publishFailure(String originalMessage, String error) {
        try {
            Map<String, Object> event = objectMapper.readValue(originalMessage, Map.class);
            event.put("step", "FAILED");
            event.put("error", "Communication cleanup failed: " + error);
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("user-delete-saga", (String) event.get("userId"), json);
        } catch (Exception e) {
            log.error("Failed to publish failure event", e);
        }
    }
}
