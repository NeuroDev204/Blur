package com.contentservice.outbox.entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Node("outbox_event")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OutboxEvent {
  @Id
  @GeneratedValue(generatorClass = UUIDStringGenerator.class)
  String id;
  String aggregateType; // post comment  story
  String aggregateId; // id cua entity
  String eventType; // "POST_CREATED", "COMMENT_CREATED", "POST_LIKED"
  String topic; // kafka topi "posts-event", "comment-moderation-request"
  String payload; // json payload
  boolean published; // da publish len kafka chua
  int retryCount; // so lan retry
  Instant createdAt;
  Instant publishedAt;
}
