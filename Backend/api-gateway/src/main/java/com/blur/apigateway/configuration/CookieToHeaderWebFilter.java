package com.blur.apigateway.configuration;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

public class CookieToHeaderWebFilter implements WebFilter {
    private static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final List<String> PUBLIC_PATTERNS = List.of(
            "/api/auth/**",
            "/api/users/registration*",
            "/api/users/registrations*",
            "/api/test-data/**",
            "/api/profile/internal/**",
            "/api/notification/email/send*",
            "/actuator/**",
            "/ws/**"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            return chain.filter(exchange);
        }

        String path = request.getURI().getPath();
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String token = getTokenFromCookie(request);
        if (token == null) {
            return chain.filter(exchange);
        }

        ServerHttpRequest mutatedRequest = request.mutate()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATTERNS.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

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
}
