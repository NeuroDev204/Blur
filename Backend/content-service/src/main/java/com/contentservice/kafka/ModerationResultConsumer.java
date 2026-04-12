package com.contentservice.kafka;

import com.contentservice.client.CommunicationServiceClient;
import com.contentservice.post.exception.AppException;
import com.contentservice.post.exception.ErrorCode;
import com.contentservice.post.repository.CommentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ModerationResultConsumer {

  private final ObjectMapper objectMapper;
  private final CommentRepository commentRepository;
  private final CacheManager cacheManager;
  private final CommunicationServiceClient communicationServiceClient;

  @KafkaListener(topics = "comment-moderation-results", groupId = "content-service")
  public void consume(String json) {
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> result = objectMapper.readValue(json, Map.class);
      String commentId = (String) result.get("commentId");
      String status = (String) result.get("status");
      double confidence = ((Number) result.get("confidence")).doubleValue();
      double toxicScore = ((Number) result.get("toxicScore")).doubleValue();
      String modelVersion = (String) result.get("modelVersion");

      commentRepository.findById(commentId).ifPresent(comment -> {
        comment.setModerationStatus(status);
        comment.setModerationConfidence(toxicScore);
        comment.setModerationConfidence(confidence);
        comment.setModelVersion(modelVersion);
        comment.setModeratedAt(LocalDateTime.now());
        if ("REJECTED".equals(status) || "FLAGGED".equals(status)) {
          comment.setContent("[Bình luận đã được ẩn bởi hệ thống kiểm duyệt!]");
        }
        commentRepository.save(comment);

        // Traverse (comment)-[:COMMENTS_ON]->(Post) to get the postId for cache eviction
        String postId = commentRepository.findPostIdByCommentId(commentId);

        // Invalidate comments cache for this post
        org.springframework.cache.Cache cache = cacheManager.getCache("comments");
        if (cache != null) {
          cache.evict(postId);
        }

        // Send real-time moderation update to user via WebSocket (through communication-service)
        try {
          Map<String, String> notificationRequest = new HashMap<>();
          notificationRequest.put("userId", comment.getUserId());
          notificationRequest.put("commentId", commentId);
          notificationRequest.put("postId", postId);
          notificationRequest.put("status", status);
          communicationServiceClient.sendModerationUpdate(notificationRequest);
        } catch (Exception e) {
          System.err.println("Failed to send moderation update via WebSocket: " + e.getMessage());
        }
      });
    } catch (AppException | JsonProcessingException e) {
      throw new AppException(ErrorCode.COMMENT_PROCESS_MODERATION_FAILED);
    }
  }
}