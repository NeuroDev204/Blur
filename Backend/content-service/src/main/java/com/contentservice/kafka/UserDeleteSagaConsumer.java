package com.contentservice.kafka;

import java.time.Instant;
import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.contentservice.post.repository.CommentReplyRepository;
import com.contentservice.post.repository.CommentRepository;
import com.contentservice.post.repository.PostFeedRepository;
import com.contentservice.post.repository.PostRepository;
import com.contentservice.story.repository.StoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserDeleteSagaConsumer {
  PostRepository postRepository;
  CommentReplyRepository commentReplyRepository;
  CommentRepository commentRepository;
  StoryRepository storyRepository;
  PostFeedRepository postFeedRepository;
  KafkaTemplate<String, String> kafkaTemplate;
  ObjectMapper objectMapper;

  @KafkaListener(topics = "user-delete-saga", groupId = "content-service,saga")
  public void handleSagaEvent(String message) {
    try {
      Map<String, Object> event = objectMapper.readValue(message, Map.class);
      String step = (String) event.get("step");
      String userId = (String) event.get("userId");
      if (!"INITIATED".equals(step) || userId == null)
        return;

      deleteUserContent(userId);

      // publish CONTENT_CLEANED
      event.put("step", "CONTENT_CLEANED");
      event.put("timestamp", Instant.now().toString());
      String json = objectMapper.writeValueAsString(event);
      kafkaTemplate.send("user-delete-saga", userId, json);
    } catch (Exception e) {
      publishFailure(message, e.getMessage());
    }
  }

  @Transactional
  public void deleteUserContent(String userId) {
    postRepository.deleteAllByUserId(userId);
    commentRepository.deleteAllByUserId(userId);
    commentReplyRepository.deleteAllByUserId(userId);
    storyRepository.deleteByAuthorId(userId);
    postFeedRepository.deleteAllByAuthorId(userId);
  }

  private void publishFailure(String originalMessage, String error) {
    try {
      Map<String, Object> event = objectMapper.readValue(originalMessage, Map.class);
      event.put("step", "FAILED");
      event.put("error", "Content cleanup failed: " + error);
      String json = objectMapper.writeValueAsString(event);
      kafkaTemplate.send("user-delete-saga", (String) event.get("userId"), json);
    } catch (Exception e) {
      log.error("Failed to publish failure event", e);
    }
  }
}
