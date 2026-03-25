package com.contentservice.configuration;

import com.contentservice.post.service.PostService;
import com.contentservice.story.service.StoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheWarmupRunner implements ApplicationRunner {
    private final PostService postService;
    private final StoryService storyService;


    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Starting cache warmup...");
        long start = System.currentTimeMillis();
        try {
            postService.getAllPots(1, 20);
            log.info("Warmed up: post page 1");
        } catch (Exception e) {
            log.warn("Failed to warm up posts: {}", e.getMessage());
        }
        try {
            storyService.getAllStories();
            log.info("Warm up: all stories");
        } catch (Exception e) {
            log.warn("Failed to warm up stories: {}", e.getMessage());
        }
        long elapsed = System.currentTimeMillis() - start;
        log.info("Cache warmup completed in {}ms", elapsed);
    }
}
