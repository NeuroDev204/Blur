package com.contentservice.outbox.scheduler;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.contentservice.outbox.entity.OutboxEvent;
import com.contentservice.outbox.repository.OutboxRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PACKAGE, makeFinal = true)
public class OutboxScheduler {
  OutboxRepository outboxRepository;
  KafkaTemplate<String, String> kafkaTemplate;

  // moi 1s doc event chua publish -> gui len kafka -> danh dau published
  @Scheduled(fixedDelay = 1000)
  public void publishedOutboxEvents() {
    List<OutboxEvent> events = outboxRepository.findUnpublishedEvents();
    if (events.isEmpty())
      return;
    for (OutboxEvent event : events) {
      try {
        kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload()).get();
        event.setPublished(true);
        event.setPublishedAt(Instant.now());
        outboxRepository.save(event);
      } catch (Exception e) {
        event.setRetryCount(event.getRetryCount() + 1);
        outboxRepository.save(event);
      }
    }
  }
  // moi 1 gio xoa event da publish cu hon 7 ngay
  @Scheduled(fixedDelay = 3600000)
  public void cleanupPublishedEvents() {
    Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
    outboxRepository.deletePublishedEventsBefore(sevenDaysAgo);
  }

}
