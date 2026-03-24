package com.contentservice.story.service;

import com.contentservice.post.exception.AppException;
import com.contentservice.post.exception.ErrorCode;
import com.contentservice.post.repository.httpclient.ProfileClient;
import com.contentservice.story.dto.request.CreateStoryRequest;
import com.contentservice.story.entity.Story;
import com.contentservice.story.repository.StoryRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
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

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StoryService {
    StoryRepository storyRepository;
    ProfileClient profileClient;
    private final CacheManager cacheManager;

    @Caching(evict = {
            @CacheEvict(value = "stories", allEntries = true),
            @CacheEvict(value = "storiesByUser", key = "#root.target.getCurrentUserId()"),
            @CacheEvict(value = "myStories", key = "#root.target.getCurrentUserId()")
    })
    public Story createStory(CreateStoryRequest createStoryRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var userId = authentication.getName();
        var profileResult = profileClient.getProfile(userId).getResult();

        Story story = Story.builder()
                .content(createStoryRequest.getContent())
                .mediaUrl(createStoryRequest.getMediaUrl())
                .timestamp(createStoryRequest.getTimestamp())
                .authorId(userId)
                .firstName(profileResult.getFirstName())
                .lastName(profileResult.getLastName())
                .thumbnailUrl(createStoryRequest.getThumbnailUrl())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        story = storyRepository.save(story);

        // Graph: (user_profile)-[:CREATED_STORY {createdAt}]->(story)
        storyRepository.linkStoryToAuthor(userId, story.getId(), story.getCreatedAt());

        return story;
    }

    @Cacheable(value = "storyById", key = "#id", sync = true)
    public Story getStoryById(String id) {
        return storyRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.STORY_NOT_FOUND));
    }

    @PostAuthorize("returnObject.get(0).authorId == authentication.name")
    @Cacheable(
            value = "myStories",
            key = "#root.target.getCurrentUserId()",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<Story> getAllMyStories() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var userId = authentication.getName();
        // Traverses (user_profile)-[:CREATED_STORY]->(story) graph edge
        return storyRepository.findAllByAuthorId(userId);
    }

    @Cacheable(value = "stories", key = "'all'", sync = true)
    public List<Story> getAllStories() {
        return storyRepository.findAll();
    }

    @Cacheable(value = "storiesByUser", key = "#userId", sync = true)
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

    @Scheduled(fixedRate = 3600000)
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
            evictAllStoryCaches();
        }
    }

    private void evictAllStoryCaches() {
        String[] storyCaches = {"stories", "storyById", "storiesByUser", "myStories", "storyLikes"};
        for (String cacheName : storyCaches) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
        log.debug("All story caches evicted");
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
