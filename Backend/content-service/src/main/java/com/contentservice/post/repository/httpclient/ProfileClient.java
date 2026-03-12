package com.contentservice.post.repository.httpclient;

import com.contentservice.post.dto.response.UserProfileResponse;
import com.contentservice.story.dto.response.ApiResponse;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "profile-service", url = "${app.service.profile.url}")
public interface ProfileClient {
    @GetMapping("/internal/users/{userId}")
    ApiResponse<UserProfileResponse> getProfile(@PathVariable("userId") String userId);

    @GetMapping("/profile/users/{profileId}")
    ApiResponse<UserProfileResponse> getProfileByProfileId(@PathVariable String profileId);

    @GetMapping("/internal/users/{userId}/follower-ids")
    ApiResponse<List<String>> getFollowerIds(@PathVariable("userId") String userId);
}
