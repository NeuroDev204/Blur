package com.contentservice.configuration;

import com.contentservice.post.repository.PostFeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * One-time-per-startup cleanup of orphaned feed items — entries whose underlying post was
 * deleted before fan-out cleanup existed. These showed up as un-interactable ghost posts
 * (e.g. a 404 on like) in followers' feeds. Runs before cache warmup so the warmed pages are clean.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class OrphanFeedCleanupRunner implements ApplicationRunner {
    private final PostFeedRepository postFeedRepository;

    @Override
    public void run(ApplicationArguments args) {
        try {
            long removed = postFeedRepository.deleteOrphanedFeedItems();
            if (removed > 0) {
                log.info("Removed {} orphaned post_feed items (underlying post no longer exists)", removed);
            } else {
                log.info("No orphaned post_feed items found");
            }
        } catch (Exception e) {
            log.warn("Orphan feed cleanup failed: {}", e.getMessage());
        }
    }
}
