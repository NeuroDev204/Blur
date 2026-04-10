package com.blur.communicationservice.resilience;

import org.springframework.stereotype.Service;

import com.blur.communicationservice.dto.ApiResponse;
import com.blur.communicationservice.dto.response.UserProfileResponse;
import com.blur.communicationservice.repository.httpclient.ProfileClient;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    @CircuitBreaker(name = "userServiceCB", fallbackMethod = "getUserProfileIdFallback")
    @Retry(name = "userServiceRetry")
    public ApiResponse<UserProfileResponse> getUserByProfileId(String profileId) {
        return profileClient.getProfileById(profileId);
    }

    private ApiResponse<UserProfileResponse> getProfileFallBack(String userId, Throwable t) {
        return fallback.getProfileFallback(userId, t);
    }
}
