package com.blur.common.event;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BaseEvent {
  // cac truong co ban
  String eventId;
  String eventType;
  String aggregateId;
  String aggregateType;
  Instant timestamp;
  String correlationId;
  int version = 1;


  public void initDefaults() {
    if (eventId == null) eventId = UUID.randomUUID().toString();
    if (timestamp == null) timestamp = Instant.now();
    if (eventType == null) eventType = this.getClass().getSimpleName();
  }


}
