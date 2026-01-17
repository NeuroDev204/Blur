package com.blur.common.outbox;

import com.blur.common.event.BaseEvent;
import com.blur.common.repository.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OutboxService {
  OutboxRepository outboxRepository;
  ObjectMapper objectMapper;

  public <T extends BaseEvent> OutboxEvent save(
      String topic,
      String aggregateType,
      String aggregateId,
      T event) {
    try {
      event.initDefaults();
      String payload = objectMapper.writeValueAsString(event);
      OutboxEvent outboxEvent = OutboxEvent.create(
          topic,
          aggregateType,
          aggregateId,
          event.getEventType(),
          payload
      );
      outboxEvent = outboxRepository.save(outboxEvent);
      return outboxEvent;
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Failed to serialize event", e);
    }
  }
}
