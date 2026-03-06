package com.blur.userservice.profile.controller;

import com.blur.userservice.identity.dto.request.ApiResponse;
import com.blur.userservice.profile.dto.request.UserProfileUpdateRequest;
import com.blur.userservice.profile.dto.response.UserProfileResponse;
import com.blur.userservice.profile.mapper.UserProfileMapper;
import com.blur.userservice.profile.service.UserProfileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserProfileController {
    UserProfileService userProfileService;
    UserProfileMapper userProfileMapper;

    @GetMapping("/users/{profileId}")
    public ApiResponse<UserProfileResponse> getProfile(@PathVariable String profileId) {
        var result = userProfileMapper.toUserProfileResponse(userProfileService.getUserProfile(profileId));
        return ApiResponse.<UserProfileResponse>builder()
                .code(1000)
                .result(result)
                .build();
    }

    @GetMapping("/users/")
    public ApiResponse<List<UserProfileResponse>> getUserProfiles() {
        var result = userProfileService.getAllUserProfiles();
        return ApiResponse.<List<UserProfileResponse>>builder()
                .code(1000)
                .result(result)
                .build();
    }

    @PutMapping("/users/{userProfileId}")
    public ApiResponse<UserProfileResponse> updateUserProfile(@PathVariable String userProfileId, @RequestBody UserProfileUpdateRequest request) {
        var profileUpdated = userProfileService.updateUserProfile(userProfileId, request);
        return ApiResponse.<UserProfileResponse>builder()
                .code(1000)
                .result(userProfileMapper.toUserProfileResponse(profileUpdated))
                .build();
    }

    @DeleteMapping("/users/{userProfileId}")
    public ApiResponse<String> deleteUserProfile(@PathVariable String userProfileId) {
        userProfileService.deleteUserProfile(userProfileId);
        return ApiResponse.<String>builder()
                .code(1000)
                .result("User Profile has been deleted")
                .build();
    }

    @GetMapping("/users/myInfo")
    public ApiResponse<UserProfileResponse> myInfo() {
        return ApiResponse.<UserProfileResponse>builder()
                .result(userProfileService.myProfile())
                .build();
    }

    @PutMapping("/users/follow/{userId}")
    public ApiResponse<String> followUser(@PathVariable String userId) {
        return ApiResponse.<String>builder()
                .result(userProfileService.followUser(userId))
                .build();
    }

    @PutMapping("/users/unfollow/{userId}")
    public ApiResponse<String> unfollowUser(@PathVariable String userId) {
        return ApiResponse.<String>builder()
                .result(userProfileService.unfollowUser(userId))
                .build();
    }

    @GetMapping("/users/search/{firstName}")
    public ApiResponse<List<UserProfileResponse>> searchUserProfiles(@PathVariable String firstName) {
        var result = userProfileService.findUserProfileByFirstName(firstName);
        return ApiResponse.<List<UserProfileResponse>>builder()
                .result(result)
                .build();
    }

    @GetMapping("/users/follower/{profileId}")
    public ApiResponse<List<UserProfileResponse>> followers(@PathVariable String profileId) {
        var result = userProfileService.getFollowers(profileId);

        return ApiResponse.<List<UserProfileResponse>>builder()
                .result(result)
                .build();
    }

    @GetMapping("/users/following/{profileId}")
    public ApiResponse<List<UserProfileResponse>> followings(@PathVariable String profileId) {
        var result = userProfileService.getFollowing(profileId);
        return ApiResponse.<List<UserProfileResponse>>builder()
                .result(result)
                .build();
    }

    @PostMapping("/users/search")
    ApiResponse<List<UserProfileResponse>> search(@RequestParam(value = "name") String request) {
        return ApiResponse.<List<UserProfileResponse>>builder()
                .result(userProfileService.search(request))
                .build();
    }

}
