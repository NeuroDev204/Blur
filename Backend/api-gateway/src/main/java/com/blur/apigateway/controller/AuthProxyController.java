package com.blur.apigateway.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

// giu nguyen cac API hien tai nhung ben trong delegate cho Keycloak
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthProxyController {
    private final WebClient.Builder webClientBuilder;

    @Value("${keycloak.token-uri}")
    private String keycloakTokenUri;
    @Value("${keycloak.logout-uri}")
    private String keycloakLogoutUri;
    @Value("${keycloak.client-id}")
    private String clientId;
    @Value("${keycloak.client-secret}")
    private String clientSecret;
    @Value("${cookie.domain}")
    private String cookieDomain;
    @Value("${cookie.secure}")
    private boolean cookieSecure;


    @PostMapping("/token")
    public Mono<Map<String, Object>> login(
            @RequestBody Map<String, String> request,
            ServerHttpResponse response
    ) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("username", request.get("username"));
        formData.add("password", request.get("password"));
        formData.add("scope", "openid");

        return webClientBuilder.build()
                .post()
                .uri(keycloakTokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(Map.class)
                .map(keycloakResponse -> {
                    String newAccessToken = (String) keycloakResponse.get("access_token");
                    String newRefreshToken = (String) keycloakResponse.get("refresh_token");
                    Number expiresIn = (Number) keycloakResponse.get("expires_in");
                    Number refreshExpiresIn = (Number) keycloakResponse.get("refresh_expires_in");

                    // set cookie
                    response.addCookie(createAccessTokenCookie(newAccessToken, expiresIn.longValue()));
                    response.addCookie(createRefreshTokenCookie(newRefreshToken, refreshExpiresIn.longValue()));

                    return Map.of(
                            "code", 1000,
                            "result", Map.of("authenticated", true)
                    );
                })
                .onErrorResume(e -> {
                    if (e instanceof WebClientResponseException wcre) {
                        log.error("Keycloak login failed: status={}, body={}", wcre.getStatusCode(), wcre.getResponseBodyAsString());
                    } else {
                        log.error("Keycloak login failed: {}", e.getMessage(), e);
                    }
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    return Mono.just(Map.of("code", 1006, "message", "Refresh failed"));
                });
    }

    // goi keycloak logout
    @PostMapping("/logout")
    public Mono<Map<String, Object>> logout(
            @CookieValue(name = "access_token", required = false) String accessToken,
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            ServerHttpResponse response
    ) {
        // xoa cookies truoc
        response.addCookie(createLogoutCookie("access_token"));
        response.addCookie(createLogoutCookie("refresh_token"));

        // goi keycloak logout (neu co refresh token)
        if (refreshToken != null && !refreshToken.isEmpty()) {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("client_id", clientId);
            formData.add("client_secret", clientSecret);
            formData.add("refresh_token", refreshToken);

            return webClientBuilder.build()
                    .post()
                    .uri(keycloakLogoutUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .toBodilessEntity()
                    .thenReturn(Map.of("code", 1000, "result", Map.of()))
                    .onErrorReturn(Map.of("code", 1000, "result", Map.of()));
        }
        return Mono.just(Map.of("code", 1000, "result", Map.of()));
    }

    // validate JWT locally (khong goi keycloak)
    // token da duoc spring security verify -> ne request den day thi token hop le
    @PostMapping("/introspect")
    public Mono<Map<String, Object>> introspect(
            @CookieValue(name = "access_token", required = false) String tokenfromCookie
    ) {
        if (tokenfromCookie == null || tokenfromCookie.isEmpty()) {
            return Mono.just(Map.of(
                    "result", Map.of("valid", false)
            ));
        }
        return Mono.just(Map.of("result", Map.of("valid", true)));
    }

    private ResponseCookie createAccessTokenCookie(String token, long maxAgeSeconds) {
        return ResponseCookie.from("access_token", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .sameSite("Lax")
                .domain(cookieDomain)
                .build();
    }

    private ResponseCookie createRefreshTokenCookie(String token, long maxAgeSeconds) {
        return ResponseCookie.from("refresh_token", token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/auth/refresh")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .sameSite("Strict")
                .domain(cookieDomain)
                .build();
    }

    private ResponseCookie createLogoutCookie(String name) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .domain(cookieDomain)
                .build();
    }
}
