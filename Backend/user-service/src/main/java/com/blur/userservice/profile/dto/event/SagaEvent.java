package com.blur.userservice.profile.dto.event;

import java.time.Instant;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SagaEvent {
  String sagaId;
  String userId;
  String step; // initiated, content_cleaned, communication_cleaned, completed, failed
  Instant timestamp;
  String error;
}
