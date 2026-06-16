package com.blur.userservice.profile.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.blur.userservice.profile.dto.event.SagaEvent;
import com.blur.userservice.profile.entity.UserProfile;
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
  KeycloakUserService keycloakUserService;

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
        // Xoa identity trong Keycloak TRUOC khi xoa profile node. Neu khong, user van con trong Keycloak
        // -> van login/cap JWT duoc, trong khi moi endpoint resolve profile qua findByUserId se 404 (USER_NOT_EXISTED).
        // Tra username khi profile con ton tai (UserProfile khong luu keycloakId). Neu Keycloak loi -> nem ra ngoai,
        // profile khong bi xoa, saga giu trang thai nhat quan.
        UserProfile profile = userProfileRepository.findByUserId(event.getUserId()).orElse(null);
        if (profile != null && profile.getUsername() != null) {
          keycloakUserService.deleteByUsername(profile.getUsername());
        } else {
          log.warn("Khong tim thay profile/username de xoa Keycloak user cho {}", event.getUserId());
        }

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
