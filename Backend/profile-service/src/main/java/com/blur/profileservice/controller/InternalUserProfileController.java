package com.blur.profileservice.controller;

import com.blur.common.dto.response.ApiResponse;
import com.blur.common.dto.response.UserProfileResponse;
import com.blur.profileservice.dto.request.ProfileCreationRequest;
import com.blur.profileservice.service.UserProfileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)

public class InternalUserProfileController {
    UserProfileService userProfileService;
    @PostMapping("/internal/users")
    public ApiResponse<UserProfileResponse> createProfile(@RequestBody ProfileCreationRequest request){
        var result = userProfileService.createProfile(request);
        result.setCreatedAt(LocalDate.now());
        return ApiResponse.<UserProfileResponse>builder()
                .code(1000)
                .result(result)
                .build();
    }
    @GetMapping("/internal/users/{userId}")
    public ApiResponse<UserProfileResponse> getProfile(@PathVariable String userId){
        return ApiResponse.<UserProfileResponse>builder()
                .result(userProfileService.getByUserId(userId))
                .build();
    }
}
