# Code full: api-gateway

## `Backend/api-gateway/src/main/java/com/blur/apigateway/configuration/AuthenticationFilter.java`
```java
package com.blur.apigateway.configuration;

import com.blur.apigateway.dto.response.ApiResponse;
import com.blur.apigateway.service.AuthService;
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
  AuthService authService;
  ObjectMapper objectMapper;
  @NonFinal
  final
  String[] publicEndpoints = {
      "/auth/.*",
      "/users/registration.*",
      "/notification/email/send.*",
      "/actuator/.*",
      "/test-data/.*",
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

    // 1. Thá»­ láº¥y token tá»« Cookie trÆ°á»›c
    String token = getTokenFromCookie(exchange.getRequest());

    // 2. Fallback: láº¥y tá»« Authorization header (backward compatibility)
    if (token == null) {
      token = getTokenFromHeader(exchange.getRequest());
    }

    if (token == null) {
      return unauthenticated(exchange.getResponse());
    }

    final String finalToken = token;

    // Verify token
    return authService.introspect(token)
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
   * Láº¥y token tá»« HttpOnly Cookie
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
   * Láº¥y token tá»« Authorization header (backward compatibility)
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
```

## `Backend/api-gateway/src/main/java/com/blur/apigateway/configuration/WebClientConfiguration.java`
```java
package com.blur.apigateway.configuration;

import com.blur.apigateway.repository.ProfileAuthClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.Arrays;
import java.util.List;

@Configuration
public class WebClientConfiguration {

    @Value("${app.services.user}")
    private String userServiceUrl;

    @Value("${app.services.communication}")
    private String chatServiceUrl;

    @Value("${CORS_ALLOWED_ORIGIN:http://localhost:3000}")
    private String allowedOrigin;

    @Value("${CORS_ALLOWED_HEADERS:*}")
    private String allowedHeaders;

    @Value("${CORS_ALLOWED_METHODS:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${CORS_ALLOW_CREDENTIALS:true}")
    private Boolean allowCredentials;

    @Value("${CORS_MAX_AGE:3600}")
    private Long maxAge;

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl(userServiceUrl)
                .build();
    }

    @Bean
    ProfileAuthClient profileAuthClient(WebClient webClient) {
        HttpServiceProxyFactory httpServiceProxyFactory = HttpServiceProxyFactory
                .builderFor(WebClientAdapter.create(webClient))
                .build();
        return httpServiceProxyFactory.createClient(ProfileAuthClient.class);
    }

    @Bean
    @Order(-2)
    CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        corsConfig.setAllowCredentials(allowCredentials);
        corsConfig.setAllowedOrigins(Arrays.asList(allowedOrigin.split(",")));
        corsConfig.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
        corsConfig.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
        corsConfig.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type"
        ));
        corsConfig.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);


        return new CorsWebFilter(source);
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("websocket_route", r -> r.path("/ws/**")
                        .filters(f -> f.prefixPath("/chat")
                                .dedupeResponseHeader("Access-Control-Allow-Origin Access-Control-Allow-Credentials Access-Control-Allow-Methods Access-Control-Allow-Headers", "RETAIN_FIRST"))
                        .uri(chatServiceUrl))
                .build();
    }
}
```

## `Backend/api-gateway/src/main/java/com/blur/apigateway/repository/ProfileAuthClient.java`
```java
package com.blur.apigateway.repository;


import com.blur.apigateway.dto.request.IntrospectRequest;
import com.blur.apigateway.dto.response.ApiResponse;
import com.blur.apigateway.dto.response.IntrospectResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.PostExchange;
import reactor.core.publisher.Mono;

public interface ProfileAuthClient {
    @PostExchange(url = "/auth/introspect", contentType = MediaType.APPLICATION_JSON_VALUE)
    Mono<ApiResponse<IntrospectResponse>> introspect(@RequestBody IntrospectRequest request);
}
```

## `Backend/api-gateway/src/main/java/com/blur/apigateway/service/AuthService.java`
```java
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
```

## `Backend/api-gateway/src/main/resources/application.yaml`
```yaml
server:
  port: 8888

app:
  api-prefix: /api
  services:
    user: ${USER_SERVICE_URL:http://localhost:8081}
    content: ${CONTENT_SERVICE_URL:http://localhost:8082}
    communication: ${COMMUNICATION_SERVICE_URL:http://localhost:8083}

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      default-filters:
        - DedupeResponseHeader=Access-Control-Allow-Origin Access-Control-Allow-Credentials Access-Control-Allow-Methods Access-Control-Allow-Headers, RETAIN_FIRST
      routes:
        # User Service - Auth endpoints
        - id: user_auth
          uri: ${app.services.user}
          predicates:
            - Path=${app.api-prefix}/auth/**
          filters:
            - RewritePath=/api/auth/(?<segment>.*), /auth/${segment}

        # User Service - Account endpoints
        - id: user_users
          uri: ${app.services.user}
          predicates:
            - Path=${app.api-prefix}/users/**
          filters:
            - RewritePath=/api/users/(?<segment>.*), /users/${segment}

        # User Service - Test data endpoints
        - id: user_test_data
          uri: ${app.services.user}
          predicates:
            - Path=${app.api-prefix}/test-data/**
          filters:
            - RewritePath=/api/test-data/(?<segment>.*), /test-data/${segment}

        # User Service - Profile endpoints
        - id: user_profile_internal
          uri: ${app.services.user}
          predicates:
            - Path=${app.api-prefix}/profile/internal/**
          filters:
            - RewritePath=/api/profile/internal/(?<segment>.*), /internal/${segment}

        - id: user_profile
          uri: ${app.services.user}
          predicates:
            - Path=${app.api-prefix}/profile/**
          filters:
            - RewritePath=/api/profile/(?<segment>.*), /profile/${segment}

        # Content Service - Comment endpoints (must be before post route)
        - id: content_comment
          uri: ${app.services.content}
          predicates:
            - Path=${app.api-prefix}/post/comment/**
          filters:
            - RewritePath=/api/post/(?<segment>.*), /${segment}

        # Content Service - Post endpoints
        - id: content_post
          uri: ${app.services.content}
          predicates:
            - Path=${app.api-prefix}/post/**
          filters:
            - RewritePath=/api/post/(?<segment>.*), /posts/${segment}

        # Content Service - Story endpoints
        - id: content_story
          uri: ${app.services.content}
          predicates:
            - Path=${app.api-prefix}/stories/**
          filters:
            - RewritePath=/api/stories/(?<segment>.*), /stories/${segment}

        # Communication Service - Chat endpoints
        - id: communication_chat
          uri: ${app.services.communication}
          predicates:
            - Path=${app.api-prefix}/chat/**
          filters:
            - StripPrefix=1

        # Communication Service - Notification endpoints
        - id: communication_notification
          uri: ${app.services.communication}
          predicates:
            - Path=${app.api-prefix}/notification/**
          filters:
            - RewritePath=/api/(?<segment>.*), /chat/${segment}

management:
  endpoints:
    web:
      exposure:
        include: health,info,gateway
```

