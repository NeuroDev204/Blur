package com.example.storyservice.controller;

import com.example.storyservice.dto.request.CreateStoryRequest;
import com.example.storyservice.dto.response.ApiResponse;
import com.example.storyservice.entity.Story;
import com.example.storyservice.service.StoryLikeService;
import com.example.storyservice.service.StoryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StoryController {
    StoryService storyService;
    StoryLikeService storyLikeService;
    @PostMapping("/create")
    public ApiResponse<Story> createStory(@RequestBody CreateStoryRequest createStoryRequest) {
        return ApiResponse.<Story>builder()
                .result(storyService.createStory(createStoryRequest))
                .build();
    }
    @GetMapping("/{id}")
    public ApiResponse<Story> getStoryById(@PathVariable String id) {
        return ApiResponse.<Story>builder()
                .result(storyService.getStoryById(id))
                .build();
    }
    @GetMapping("/my-stories")
    public ApiResponse<List<Story>> getAllMyStories() {
        return ApiResponse.<List<Story>>builder()
                .result(storyService.getAllMyStories())
                .build();
    }
    @GetMapping("/all")
    public ApiResponse<List<Story>> getAllStories() {
        return ApiResponse.<List<Story>>builder()
                .result(storyService.getAllStories())
                .build();
    }
    @GetMapping("/user/{userId}")
    public ApiResponse<List<Story>> getAllStoriesByUserId(@PathVariable String userId) {
        return ApiResponse.<List<Story>>builder()
                .result(storyService.getAllStoriesByUserId(userId))
                .build();
    }
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteStory(@PathVariable String id) {
        var result = storyService.deleteStoryById(id);
        return ApiResponse.<String>builder()
                .result(result)
                .build();
    }
    @PutMapping("/{id}")
    public ApiResponse<Story> updateStory(@PathVariable String id, @RequestBody CreateStoryRequest createStoryRequest) {
        return ApiResponse.<Story>builder()
                .result(storyService.updateStory(id, createStoryRequest))
                .build();
    }
    /*
    @PutMapping("/like/{storyId}")
    public ApiResponse<String> likeStory(@PathVariable String storyId) {
        return ApiResponse.<String>builder()
                .result(storyLikeService.likeStory(storyId))
                .build();
    }

     */
    @PutMapping("/like/{storyId}")
    public ApiResponse<String> likeStory(
            @PathVariable String storyId,
            @RequestParam(defaultValue = "LIKE") String reactionType
    ) {
        return ApiResponse.<String>builder()
                .result(storyLikeService.likeStory(storyId, reactionType))
                .build();
    }


    @PutMapping("/unlike/{storyId}")
    public ApiResponse<String> unlikeStory(@PathVariable String storyId) {
        return ApiResponse.<String>builder()
                .result(storyLikeService.unlikeStory(storyId))
                .build();
    }

}
