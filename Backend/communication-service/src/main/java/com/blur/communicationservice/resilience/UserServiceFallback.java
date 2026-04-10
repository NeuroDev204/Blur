package com.blur.communicationservice.resilience;

import org.springframework.stereotype.Component;

import com.blur.communicationservice.dto.ApiResponse;
import com.blur.communicationservice.dto.response.UserProfileResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class UserServiceFallback {
    public ApiResponse<UserProfileResponse> getProfileFallback(String userId, Throwable throwable) {
        UserProfileResponse fallback = UserProfileResponse.builder()
                .userId(userId)
                .username("Người dùng")
                .build();
        return ApiResponse.<UserProfileResponse>builder()
                .code(200)
                .result(fallback)
                .build();
    }
}
