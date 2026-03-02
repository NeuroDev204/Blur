package org.identityservice.repository.httpclient;

import java.util.List;

import org.identityservice.configuration.AuthenticationRequestInterceptor;
import org.identityservice.dto.request.ApiResponse;
import org.identityservice.dto.request.ProfileCreationRequest;
import org.identityservice.dto.response.UserProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "profile-service",
        url = "${app.services.profile}",
        configuration = {AuthenticationRequestInterceptor.class})
public interface ProfileClient {
    @PostMapping(value = "/internal/users", produces = MediaType.APPLICATION_JSON_VALUE)
    ApiResponse<UserProfileResponse> createProfile(@RequestBody ProfileCreationRequest request);

    @GetMapping(value = "/internal/users/all", produces = MediaType.APPLICATION_JSON_VALUE)
    ApiResponse<List<UserProfileResponse>> getAllProfiles();

    @PostMapping(value = "/internal/users/follow", produces = MediaType.APPLICATION_JSON_VALUE)
    ApiResponse<String> followUserInternal(
            @RequestParam("fromProfileId") String fromProfileId, @RequestParam("toProfileId") String toProfileId);
}
