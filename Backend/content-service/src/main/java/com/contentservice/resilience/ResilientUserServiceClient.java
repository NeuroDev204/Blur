package com.contentservice.resilience;

import com.contentservice.post.dto.response.UserProfileResponse;
import com.contentservice.post.repository.httpclient.ProfileClient;
import com.contentservice.story.dto.response.ApiResponse;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResilientUserServiceClient {
    private final ProfileClient profileClient;
    private final UserServiceFallback fallback;

    @Bulkhead(name = "userServiceBH")
    @CircuitBreaker(name = "userServiceCB", fallbackMethod = "getProfileFallback")
    @Retry(name = "userServiceRetry")
    public ApiResponse<UserProfileResponse> getProfile(String userId) {
        return profileClient.getProfile(userId);
    }

    @Bulkhead(name = "userServiceBH")
    @CircuitBreaker(name = "userServiceCB", fallbackMethod = "getFollowerIdsFallback")
    @Retry(name = "userServiceRetry")
    public ApiResponse<List<String>> getFollowerIds(String userId) {
        return profileClient.getFollowerIds(userId);
    }

    private ApiResponse<UserProfileResponse> getProfileFallback(String userId, Throwable t) {
        return fallback.getProfileFallback(userId, t);
    }

    private ApiResponse<List<String>> getFollowerIdsFallback(String userId, Throwable t) {
        return fallback.getFollowerIdsFallback(userId, t);
    }
}

