package com.blur.chatservice.repository.httpclient;

import java.text.ParseException;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.blur.chatservice.configuration.AuthenticationRequestInterceptor;
import com.blur.chatservice.dto.ApiResponse;
import com.blur.chatservice.dto.request.IntrospectRequest;
import com.blur.chatservice.dto.response.IntrospecResponse;
import com.nimbusds.jose.JOSEException;

@FeignClient(
        name = "identity-service",
        url = "${app.services.identity.url}",
        configuration = {AuthenticationRequestInterceptor.class})
public interface IdentityClient {
    @PostMapping(value = "/auth/introspect", produces = MediaType.APPLICATION_JSON_VALUE)
    ApiResponse<IntrospecResponse> introspect(@RequestBody IntrospectRequest request)
            throws ParseException, JOSEException;
}
