package com.blur.apigateway.configuration;

import com.blur.apigateway.dto.response.ApiResponse;
import com.blur.apigateway.service.IdentityService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JsonParseException;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationFilter implements GlobalFilter, Ordered {
  private static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
  IdentityService identityService;
  ObjectMapper objectMapper;
  @NonFinal
  final
  String[] publicEndpoints = {
      "/identity/auth/.*",
      "/identity/users/registration.*",
      "/notification/email/send.*",
      "/actuator/.*",
      "/identity/test-data/.*",
      "/profile/internal/generate-follows",
      "/profile/internal/generate-cities"
  };

  @Value("${app.api-prefix}")
  @NonFinal
  private String apiPrefix;

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String path = exchange.getRequest().getURI().getPath();
    String method = exchange.getRequest().getMethod().toString();


    // Bypass OPTIONS for CORS preflight
    if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {

      return chain.filter(exchange);
    }

    // Check if public endpoint
    if (isPublicEndpoint(exchange.getRequest())) {

      return chain.filter(exchange);
    }

    // 1. Thử lấy token từ Cookie trước
    String token = getTokenFromCookie(exchange.getRequest());

    // 2. Fallback: lấy từ Authorization header (backward compatibility)
    if (token == null) {
      token = getTokenFromHeader(exchange.getRequest());
    }

    if (token == null) {
      return unauthenticated(exchange.getResponse());
    }

    final String finalToken = token;

    // Verify token
    return identityService.introspect(token)
        .flatMap(introspectResponse -> {
          if (introspectResponse.getResult() != null && introspectResponse.getResult().isValid()) {

            // Forward Authorization header sang downstream service
            ServerHttpRequest mutatedRequest = exchange.getRequest()
                .mutate()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + finalToken)
                .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
          } else {
            return unauthenticated(exchange.getResponse());
          }
        })
        .onErrorResume(throwable -> {

          return unauthenticated(exchange.getResponse());
        });
  }

  /**
   * Lấy token từ HttpOnly Cookie
   */
  private String getTokenFromCookie(ServerHttpRequest request) {
    MultiValueMap<String, HttpCookie> cookies = request.getCookies();
    if (cookies.containsKey(ACCESS_TOKEN_COOKIE_NAME)) {
      HttpCookie cookie = cookies.getFirst(ACCESS_TOKEN_COOKIE_NAME);
      if (cookie != null && !cookie.getValue().isEmpty()) {

        return cookie.getValue();
      }
    }
    return null;
  }

  /**
   * Lấy token từ Authorization header (backward compatibility)
   */
  private String getTokenFromHeader(ServerHttpRequest request) {
    List<String> authHeader = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
    if (!CollectionUtils.isEmpty(authHeader)) {
      String headerValue = authHeader.get(0);
      if (headerValue.startsWith("Bearer ")) {

        return headerValue.replace("Bearer ", "");
      }
    }
    return null;
  }

  @Override
  public int getOrder() {
    return -1;
  }

  private boolean isPublicEndpoint(ServerHttpRequest request) {
    String path = request.getURI().getPath();
    boolean isPublic = Arrays.stream(publicEndpoints).anyMatch(pattern -> {
      String fullPattern = apiPrefix + pattern;
      boolean matches = path.matches(fullPattern);
      if (matches) {
      }
      return matches;
    });
    return isPublic;
  }

  Mono<Void> unauthenticated(ServerHttpResponse response) {
    ApiResponse<?> apiResponse = ApiResponse.builder()
        .code(1401)
        .message("Unauthenticated")
        .build();

    String body;
    try {
      body = objectMapper.writeValueAsString(apiResponse);
    } catch (JsonProcessingException e) {
      throw new JsonParseException(e);
    }

    response.setStatusCode(HttpStatus.UNAUTHORIZED);
    response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
  }
}