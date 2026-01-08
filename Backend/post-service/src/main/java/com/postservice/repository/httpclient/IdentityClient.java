package com.postservice.repository.httpclient;

import com.blur.common.dto.response.ApiResponse;
import com.blur.common.dto.response.UserResponse;
import com.postservice.configuration.AuthenticationRequestInterceptor;
import com.postservice.configuration.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "identity-service", url = "${app.service.identity.url}",configuration = {AuthenticationRequestInterceptor.class, FeignConfig.class})
public interface IdentityClient {
    @GetMapping("/users/{userId}")
    public ApiResponse<UserResponse> getUser(@PathVariable String userId);
}
