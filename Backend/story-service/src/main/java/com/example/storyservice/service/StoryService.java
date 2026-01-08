package com.example.storyservice.service;

import com.example.storyservice.dto.request.CreateStoryRequest;
import com.example.storyservice.entity.Story;
import com.example.storyservice.exception.AppException;
import com.example.storyservice.exception.ErrorCode;
import com.example.storyservice.repository.StoryRepository;
import com.example.storyservice.repository.httpclient.ProfileClient;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class StoryService {
    StoryRepository storyRepository;
    ProfileClient profileClient;

    @Caching(evict = {
            @CacheEvict(value = "stories", allEntries = true),
            @CacheEvict(value = "storiesByUser", key = "#root.target.getCurrentUserId()"),
            @CacheEvict(value = "myStories", key = "#root.target.getCurrentUserId()")
    })
    public Story createStory(CreateStoryRequest createStoryRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var userId = authentication.getName();
        var profile = profileClient.getProfile(userId);
        Story story = Story.builder()
                .content(createStoryRequest.getContent())
                .mediaUrl(createStoryRequest.getMediaUrl())
                .timestamp(createStoryRequest.getTimestamp())
                .authorId(userId)
                .firstName(profile.getResult().getFirstName())
                .lastName(profile.getResult().getLastName())
                .thumbnailUrl(createStoryRequest.getThumbnailUrl())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        storyRepository.save(story);
        return story;
    }

    @Cacheable(value = "storyById", key = "#id", unless = "#result == null")
    public Story getStoryById(String id) {
        return storyRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.STORY_NOT_FOUND));
    }

    @PostAuthorize("returnObject.get(0).authorId == authentication.name")
    @Cacheable(value = "myStories", key = "#root.target.getCurrentUserId()", unless = "#result == null || #result.isEmpty()")
    public List<Story> getAllMyStories() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var userId = authentication.getName();
        return storyRepository.findAllByAuthorId(userId);
    }

    @Cacheable(value = "stories", key = "'all'", unless = "#result == null || #result.isEmpty()")
    public List<Story> getAllStories() {
        return storyRepository.findAll();
    }

    @Cacheable(value = "storiesByUser", key = "#userId", unless = "#result == null || #result.isEmpty()")
    public List<Story> getAllStoriesByUserId(String userId) {
        return storyRepository.findAllByAuthorId(userId);
    }

    @Caching(evict = {
            @CacheEvict(value = "stories", allEntries = true),
            @CacheEvict(value = "storyById", key = "#id"),
            @CacheEvict(value = "storiesByUser", key = "#root.target.getAuthorIdByStoryId(#id)"),
            @CacheEvict(value = "myStories", key = "#root.target.getCurrentUserId()"),
            @CacheEvict(value = "storyLikes", key = "#id")
    })
    public String deleteStoryById(String id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var userId = authentication.getName();
        var story = storyRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.STORY_NOT_FOUND));
        if (!userId.equals(story.getAuthorId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        storyRepository.deleteById(id);
        return "Delete story successfully";
    }

    @Caching(evict = {
            @CacheEvict(value = "stories", allEntries = true),
            @CacheEvict(value = "storyById", key = "#id"),
            @CacheEvict(value = "storiesByUser", key = "#root.target.getCurrentUserId()"),
            @CacheEvict(value = "myStories", key = "#root.target.getCurrentUserId()")
    })
    public Story updateStory(String id, CreateStoryRequest createStoryRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var userId = authentication.getName();
        var story = storyRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.STORY_NOT_FOUND));
        if (!userId.equals(story.getAuthorId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        story.setContent(createStoryRequest.getContent());
        story.setMediaUrl(createStoryRequest.getMediaUrl());
        story.setTimestamp(createStoryRequest.getTimestamp());
        story.setUpdatedAt(Instant.now());
        return storyRepository.save(story);
    }

    @Scheduled(fixedRate = 3600000) // Run every hour (3600000 ms)
    @Caching(evict = {
            @CacheEvict(value = "stories", allEntries = true),
            @CacheEvict(value = "storyById", allEntries = true),
            @CacheEvict(value = "storiesByUser", allEntries = true),
            @CacheEvict(value = "myStories", allEntries = true),
            @CacheEvict(value = "storyLikes", allEntries = true)
    })
    public void deleteOldStories() {
        Instant twentyFourHoursAgo = Instant.now().minus(24, ChronoUnit.HOURS);

        List<Story> oldStories = storyRepository.findAllByCreatedAtBefore(twentyFourHoursAgo);
        if (!oldStories.isEmpty()) {
            storyRepository.deleteAll(oldStories);
        }
    }

    public String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public String getAuthorIdByStoryId(String storyId) {
        return storyRepository.findById(storyId)
                .map(Story::getAuthorId)
                .orElse(null);
    }
}