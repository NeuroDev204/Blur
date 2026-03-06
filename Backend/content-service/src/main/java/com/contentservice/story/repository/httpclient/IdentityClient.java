package com.contentservice.story.repository.httpclient;


import com.contentservice.configuration.AuthenticationRequestInterceptor;
import com.contentservice.post.dto.response.UserResponse;
import com.contentservice.story.dto.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "story-identity-service", url = "${app.service.identity.url}", configuration = {AuthenticationRequestInterceptor.class})
public interface IdentityClient {
    @GetMapping("/users/{userId}")
    ApiResponse<UserResponse> getUser(@PathVariable String userId);
}
