package com.blur.communicationservice.repository.httpclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.blur.communicationservice.dto.ApiResponse;
import com.blur.communicationservice.dto.response.UserProfileResponse;

@FeignClient(name = "profile-service", url = "${app.services.profile.url}")
public interface ProfileClient {
    @GetMapping("/internal/users/{userId}")
    ApiResponse<UserProfileResponse> getProfile(@PathVariable("userId") String userId);

    @GetMapping("/profile/users/{profileId}")
    ApiResponse<UserProfileResponse> getProfileById(@PathVariable("profileId") String profileId);
}
