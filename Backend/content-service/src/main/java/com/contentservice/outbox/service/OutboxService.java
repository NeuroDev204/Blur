package com.contentservice.outbox.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.contentservice.outbox.entity.OutboxEvent;
import com.contentservice.outbox.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OutboxService {
  OutboxRepository outboxRepository;
  ObjectMapper objectMapper;

  // luu event vao outbox table goi cung @Transactional voi business logic
  public void saveEvent(String aggregateType, String aggregateId, String eventType, String topic, Object payload) {
    try{
      OutboxEvent event = OutboxEvent.builder()
      .aggregateId(aggregateId)
      .aggregateType(aggregateType)
      .eventType(eventType)
      .topic(topic)
      .payload(objectMapper.writeValueAsString(payload))
      .published(false)
      .retryCount(0)
      .createdAt(Instant.now())
      .build();
      outboxRepository.save(event);
      log.debug("Outbox event saved: type={}, aggregateId={}", eventType,aggregateId);
    } catch (Exception e) {
      throw new RuntimeException("Failed to save outbox event", e);
    }
  }
}
