package com.contentservice.post.controller;

import com.contentservice.post.service.FeedService;
import com.contentservice.story.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/feed")
@RequiredArgsConstructor
public class InternalFeedController {

    private final FeedService feedService;

    @PostMapping("/backfill")
    public ApiResponse<String> backfillFeed(
            @RequestParam String followerUserId,
            @RequestParam String followedUserId) {
        feedService.backfillFeedForFollower(followerUserId, followedUserId);
        return ApiResponse.<String>builder()
                .code(1000)
                .result("Feed backfilled")
                .build();
    }
}
