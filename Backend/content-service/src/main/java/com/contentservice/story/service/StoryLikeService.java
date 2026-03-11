package com.contentservice.story.service;

import com.contentservice.post.dto.event.Event;
import com.contentservice.post.exception.AppException;
import com.contentservice.post.exception.ErrorCode;
import com.contentservice.post.repository.httpclient.ProfileClient;
import com.contentservice.kafka.NotificationEventPublisher;
import com.contentservice.story.repository.StoryRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StoryLikeService {
    StoryRepository storyRepository;
    ProfileClient profileClient;
    NotificationEventPublisher notificationEventPublisher;

    @CacheEvict(value = "storyLikes", key = "#storyId")
    public String likeStory(String storyId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var userId = authentication.getName();
        var story = storyRepository.findById(storyId)
                .orElseThrow(() -> new AppException(ErrorCode.STORY_NOT_FOUND));

        // Graph: (user_profile)-[:LIKED_STORY {createdAt}]->(story)
        storyRepository.likeStory(userId, storyId, Instant.now());

        var receiverProfile = profileClient.getProfile(story.getAuthorId()).getResult();
        Event event = Event.builder()
                .senderName(story.getFirstName() + " " + story.getLastName())
                .senderId(userId)
                .receiverEmail(receiverProfile != null ? receiverProfile.getEmail() : null)
                .receiverId(story.getAuthorId())
                .receiverName(receiverProfile != null ? receiverProfile.getUsername() : "Unknown")
                .timestamp(LocalDateTime.now())
                .build();
        notificationEventPublisher.publishLikeStoryEvent(event);
        return "Like story successfully";
    }

    @CacheEvict(value = "storyLikes", key = "#storyId")
    public String unlikeStory(String storyId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var userId = authentication.getName();
        // Remove the (user_profile)-[:LIKED_STORY]->(story) edge
        storyRepository.unlikeStory(userId, storyId);
        return "Unlike story successfully";
    }
}
