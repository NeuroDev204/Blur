package com.blur.common.outbox;


import com.blur.common.enums.OutboxStatus;
import com.blur.common.exception.BlurException;
import com.blur.common.exception.ErrorCode;
import com.blur.common.repository.OutboxRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OutboxPublisher {
  OutboxRepository outboxRepository;
  KafkaTemplate<String, String> kafkaTemplate;

  static int MAX_RETRIES = 3;

  // lap lai moi 100ms
  @Scheduled(fixedDelay = 100)
  public void publishPendingEvents() throws ExecutionException, InterruptedException {
    // gioi han 100 events de tranh oom
    List<OutboxEvent> pendingEvents = outboxRepository.findTop100ByStatusOrderByCreatedAt(OutboxStatus.PENDING);

    if (pendingEvents.isEmpty()) {
      return; // khong co gi de publish
    }
    for (OutboxEvent event : pendingEvents) {
      publishEvent(event);
    }
  }

  void publishEvent(OutboxEvent outboxEvent) throws ExecutionException, InterruptedException {
    try {
      kafkaTemplate.send(
          outboxEvent.getTopic(),
          outboxEvent.getAggregateId(),
          outboxEvent.getPayload()
      ).get();

      outboxEvent.setStatus(OutboxStatus.PUBLISHED);
      outboxEvent.setPublishedAt(Instant.now());
      outboxRepository.save(outboxEvent);
    } catch (Exception ex) {
      handlePublishFailure(outboxEvent, ex);
    }
  }

  // retry toi da 3 lan -> sau 3 lan danh failed
  void handlePublishFailure(OutboxEvent event, Exception ex) {
    event.setRetryCount(event.getRetryCount() + 1);
    event.setErrorMessage(ex.getMessage());
    if (event.getRetryCount() >= MAX_RETRIES) {
      event.setStatus(OutboxStatus.FAILED);
      throw new BlurException(ErrorCode.OUTBOX_PUBLISHER_FAILED);
    }
    outboxRepository.save(event);
  }

  // admin api: retry tai ca failed events
  public int retryFailedEvents() {
    List<OutboxEvent> failedEvents = outboxRepository.findByStatusOrderByCreatedAtDesc(OutboxStatus.FAILED);
    for (OutboxEvent event : failedEvents) {
      event.setStatus(OutboxStatus.PENDING);
      event.setRetryCount(0);
      event.setErrorMessage(null);
      outboxRepository.save(event);
    }
    log.info("OutboxPublisher: reset {} failed events to PENDING", failedEvents.size());
    return failedEvents.size();
  }

}
