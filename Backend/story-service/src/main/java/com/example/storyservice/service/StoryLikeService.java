package com.example.storyservice.service;

import com.example.storyservice.dto.event.Event;
import com.example.storyservice.entity.StoryLike;
import com.example.storyservice.exception.AppException;
import com.example.storyservice.exception.ErrorCode;
import com.example.storyservice.repository.StoryLikeRepository;
import com.example.storyservice.repository.StoryRepository;
import com.example.storyservice.repository.httpclient.IdentityClient;
import com.example.storyservice.repository.httpclient.NotificationClient;
import com.example.storyservice.repository.httpclient.ProfileClient;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.prepost.PostAuthorize;
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
    NotificationClient notificationClient;
    ProfileClient profileClient;


    @CacheEvict(value = "storyLikes", key = "#storyId")
    public String likeStory(String storyId, String reactionType) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String senderUserId = authentication.getName();
        var userId = authentication.getName();
        var story = storyRepository.findById(storyId)
                .orElseThrow(() -> new AppException(ErrorCode.STORY_NOT_FOUND));
        String receiverUserId = story.getAuthorId();
        if (senderUserId.equals(receiverUserId)) {
            return "Like story successfully";
        }
        StoryLike storyLike = StoryLike.builder()
                .storyId(storyId)
                .userId(userId)
                .createdAt(story.getCreatedAt())
                .updatedAt(story.getUpdatedAt())
                .build();
        storyLikeRepository.save(storyLike);
        var user = identityClient.getUser(story.getAuthorId());
        var senderProfile = profileClient.getProfile(senderUserId).getResult();
        var receiverProfile = profileClient.getProfile(receiverUserId).getResult();
        var receiverIdentity = identityClient.getUser(receiverUserId).getResult();

        Event event = Event.builder()
                .action("REACT")
                .storyId(storyId)
                .reactionType(reactionType) // LIKE/LOVE/...
                .timestamp(LocalDateTime.now())

                .senderUserId(senderUserId)
                .senderId(senderProfile.getId()) // profileId
                .senderFirstName(senderProfile.getFirstName())
                .senderLastName(senderProfile.getLastName())
                .senderName(story.getFirstName() + " " + story.getLastName())
                .senderId(userId)
                .senderImageUrl(senderProfile.getImageUrl())

                .receiverUserId(receiverUserId)
                .receiverEmail(user.getResult().getEmail())
                .receiverId(user.getResult().getId())
                .receiverName(user.getResult().getUsername())
                .timestamp(LocalDateTime.now())
                .build();
        notificationClient.sendLikeStoryNotification(event);
        return "Like story successfully";
    }

    @CacheEvict(value = "storyLikes", key = "#storyId")
    public String unlikeStory(String storyId){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var userId = authentication.getName();
        storyLikeRepository.deleteByStoryIdAndUserId(storyId, userId);
        return "Unlike story successfully";
    }
}
