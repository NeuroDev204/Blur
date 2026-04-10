package com.contentservice.resilience;

import com.contentservice.post.dto.response.UserProfileResponse;
import com.contentservice.story.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class UserServiceFallback {
    public ApiResponse<UserProfileResponse> getProfileFallback(String userId, Throwable throwable) {

        UserProfileResponse fallbackProfile = UserProfileResponse.builder()
                .userId(userId)
                .username("Người dùng")
                .imageUrl(null)
                .build();
        return ApiResponse.<UserProfileResponse>builder()
                .code(200)
                .result(fallbackProfile)
                .build();
    }

    public ApiResponse<List<String>> getFollowerIdsFallback(String userId, Throwable throwable) {
        return ApiResponse.<List<String>>builder()
                .code(200)
                .result(Collections.emptyList())
                .build();
    }

}
