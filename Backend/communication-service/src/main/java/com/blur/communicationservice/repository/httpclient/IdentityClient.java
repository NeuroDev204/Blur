package com.blur.communicationservice.repository.httpclient;

import java.text.ParseException;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.blur.communicationservice.configuration.AuthenticationRequestInterceptor;
import com.blur.communicationservice.dto.ApiResponse;
import com.blur.communicationservice.dto.request.IntrospectRequest;
import com.blur.communicationservice.dto.response.IntrospecResponse;
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
