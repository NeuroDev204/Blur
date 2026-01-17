package com.blur.common.outbox;

import com.blur.common.enums.OutboxStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "outbox_event")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OutboxEvent {
  @MongoId
  String id;
  String aggregateType;
  String aggregateId;
  String eventType;
  String topic;
  String payload;

  @Indexed
  Instant createdAt;
  @Indexed
  OutboxStatus status;

  int retryCount;
  String errorMessage;
  Instant publishedAt;

  public static OutboxEvent create(String topic, String aggregateType,
      String aggregateId, String eventType, String payload) {
    return OutboxEvent.builder()
        .id(UUID.randomUUID().toString())
        .topic(topic)
        .aggregateType(aggregateType)
        .aggregateId(aggregateId)
        .eventType(eventType)
        .payload(payload)
        .createdAt(Instant.now())
        .status(OutboxStatus.PENDING)
        .retryCount(0)
        .build();
  }

}
