package com.blur.apigateway.service;

import com.blur.apigateway.dto.request.IntrospectRequest;
import com.blur.apigateway.dto.response.ApiResponse;
import com.blur.apigateway.dto.response.IntrospectResponse;
import com.blur.apigateway.repository.ProfileAuthClient;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthService {
    ProfileAuthClient profileAuthClient;

    public Mono<ApiResponse<IntrospectResponse>> introspect(String token) {
        return profileAuthClient.introspect(IntrospectRequest.builder().token(token).build());
    }
}
