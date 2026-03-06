package com.contentservice.story.service;

import com.contentservice.post.dto.event.Event;
import com.contentservice.post.exception.AppException;
import com.contentservice.post.exception.ErrorCode;
import com.contentservice.post.repository.httpclient.IdentityClient;
import com.contentservice.kafka.NotificationEventPublisher;
import com.contentservice.story.entity.StoryLike;
import com.contentservice.story.repository.StoryLikeRepository;
import com.contentservice.story.repository.StoryRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StoryLikeService {
    StoryLikeRepository storyLikeRepository;
    StoryRepository storyRepository;
    IdentityClient identityClient;
    NotificationEventPublisher notificationEventPublisher;

    @CacheEvict(value = "storyLikes", key = "#storyId")
    public String likeStory(String storyId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var userId = authentication.getName();
        var story = storyRepository.findById(storyId)
                .orElseThrow(() -> new AppException(ErrorCode.STORY_NOT_FOUND));
        StoryLike storyLike = StoryLike.builder()
                .storyId(storyId)
                .userId(userId)
                .createdAt(story.getCreatedAt())
                .updatedAt(story.getUpdatedAt())
                .build();
        storyLikeRepository.save(storyLike);
        var user = identityClient.getUser(story.getAuthorId());
        Event event = Event.builder()
                .senderName(story.getFirstName() + " " + story.getLastName())
                .senderId(userId)
                .receiverEmail(user.getResult().getEmail())
                .receiverId(user.getResult().getId())
                .receiverName(user.getResult().getUsername())
                .timestamp(LocalDateTime.now())
                .build();
        notificationEventPublisher.publishLikeStoryEvent(event);
        return "Like story successfully";
    }

    @CacheEvict(value = "storyLikes", key = "#storyId")
    public String unlikeStory(String storyId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var userId = authentication.getName();
        storyLikeRepository.deleteByStoryIdAndUserId(storyId, userId);
        return "Unlike story successfully";
    }
}
