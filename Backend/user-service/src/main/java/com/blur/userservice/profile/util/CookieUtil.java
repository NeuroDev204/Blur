package com.blur.userservice.profile.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CookieUtil {

    public static final String ACCESS_TOKEN_COOKIE_NAME = "access_token";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    @Value("${jwt.valid-duration}")
    private long validDuration;
    @Value("${cookie.domain:localhost}")
    private String cookieDomain;
    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    public ResponseCookie createAccessTokenCookie(String token) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(Duration.ofSeconds(validDuration))
                .sameSite("Lax")
                .domain(cookieDomain)
                .build();
    }

    public ResponseCookie createRefreshTokenCookie(String token, long refreshDuration) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, token)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/auth/refresh")
                .maxAge(Duration.ofSeconds(refreshDuration))
                .sameSite("Strict")
                .domain(cookieDomain)
                .build();
    }

    public ResponseCookie createLogoutCookie(String cookieName) {
        return ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .domain(cookieDomain)
                .build();
    }
}
