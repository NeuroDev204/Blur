package com.blur.userservice.profile.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.blur.userservice.profile.dto.event.SagaEvent;
import com.blur.userservice.profile.repository.UserProfileRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserDeleteSagaService {
  KafkaTemplate<String, String> kafkaTemplate;
  ObjectMapper objectMapper;
  UserProfileRepository userProfileRepository;

  // xoa user
  public void initiateDeleteUser(String userId) {
    try {
      SagaEvent event = SagaEvent.builder()
          .sagaId(UUID.randomUUID().toString())
          .userId(userId)
          .step("INITIATED")
          .timestamp(Instant.now())
          .build();
      String json = objectMapper.writeValueAsString(event);
      kafkaTemplate.send("user-delete-saga", userId, json);
    } catch (Exception e) {
      log.error("Failed to initiate delete saga for user {}", userId, e);
    }
  }

  // lang nghe saga event de hoan thanh buoc cuoi
  @KafkaListener(topics = "user-delete-saga", groupId = "user-service-saga")
  public void handleSagaEvent(String message) {
    try {
      SagaEvent event = objectMapper.readValue(message, SagaEvent.class);
      if ("COMMUNICATION_CLEANED".equals(event.getStep())) {
        // buooc cuoi xoa user profile
        userProfileRepository.deleteByUserId(event.getUserId());
        log.info("User profile deleted for user {}", event.getUserId());

        // publish completed
        event.setStep("COMPLETED");
        event.setTimestamp(Instant.now());
        String json = objectMapper.writeValueAsString(event);
        kafkaTemplate.send("user-delete-saga", event.getUserId(), json);
        log.info("Delete user saga COMPLETED for user {}", event.getUserId());
      }
      if ("FAILED".equals(event.getStep())) {
        log.error("Delete user saga FAILED for user {}: {}", event.getUserId(), event.getError());
      }
    } catch (Exception e) {
      log.error("Failed to handle saga event", e);
    }
  }
}
