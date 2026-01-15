package com.blur.chatservice.repository.httpclient;

import com.blur.common.configuration.AuthenticationRequestInterceptor;
import com.blur.common.dto.request.IntrospectRequest;
import com.blur.common.dto.response.ApiResponse;
import com.blur.common.dto.response.IntrospectResponse;
import com.nimbusds.jose.JOSEException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.text.ParseException;

@FeignClient(
    name = "identity-service",
    url = "${app.services.identity.url}",
    configuration = {AuthenticationRequestInterceptor.class})
public interface IdentityClient {
  @PostMapping(value = "/auth/introspect", produces = MediaType.APPLICATION_JSON_VALUE)
  ApiResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest request)
      throws ParseException, JOSEException;
}
