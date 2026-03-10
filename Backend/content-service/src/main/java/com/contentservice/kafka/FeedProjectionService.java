package com.contentservice.kafka;

import com.contentservice.post.entity.PostFeedItem;
import com.contentservice.post.repository.PostFeedRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FeedProjectionService {
  PostFeedRepository postFeedRepository;
  ObjectMapper objectMapper;

  @KafkaListener(topics = "post-events", groupId = "content-feed-projection")
  public void consume(String json) {
    try {
      Map<String, Object> event = objectMapper.readValue(json, Map.class);
      String eventType = (String) event.get("eventType");
      switch (eventType) {
        case "POST_CREATED" -> handlePostCreated(event);
        case "POST_DELETED" -> handlePostDeleted(event);
        case "POST_LIKED" -> handleEngagementUpdate(event, "likeCount");
        case "POST_COMMENTED" -> handleEngagementUpdate(event, "commentCount");
        default -> log.debug("Ignoring event: {}", eventType);
      }
    } catch (Exception e) {
      log.error("Failed to process post event for feed projection", e);
    }
  }

  @SuppressWarnings("unchecked")
  private void handlePostCreated(Map<String, Object> event) {
    String postId = (String) event.get("postId");
    String authorId = (String) event.get("authorId");
    String content = (String) event.get("content");
    List<String> followerIds = (List<String>) event.get("followerIds");
    if (followerIds == null || followerIds.isEmpty()) {
      return;
    }
    // tao feed item cho moi follower
    for (String followerId : followerIds) {
      PostFeedItem item = PostFeedItem.builder()
          .postId(postId)
          .content(content)
          .authorId(authorId)
          .authorUsername((String) event.get("authorUsername"))
          .authorFirstName((String) event.get("authorFirstName"))
          .authorLastName((String) event.get("authorLastName"))
          .authorAvatar((String) event.get("authorAvatar"))
          .targetUserId(followerId)
          .likeCount(0)
          .commentCount(0)
          .shareCount(0)
          .build();
      postFeedRepository.save(item);
    }
  }

  private void handlePostDeleted(Map<String, Object> event) {
    String postId = (String) event.get("postId");
    postFeedRepository.deleteAllByPostId(postId);
  }

  private void handleEngagementUpdate(Map<String, Object> event, String field) {

  }
}
