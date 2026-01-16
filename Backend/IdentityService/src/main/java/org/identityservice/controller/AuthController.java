package org.identityservice.controller;

import java.text.ParseException;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

import org.identityservice.dto.request.AuthRequest;
import org.identityservice.dto.request.LogoutRequest;
import org.identityservice.dto.request.RefreshRequest;
import org.identityservice.dto.response.AuthResponse;
import org.identityservice.service.AuthenticationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.*;

import com.blur.common.dto.request.IntrospectRequest;
import com.blur.common.dto.response.ApiResponse;
import com.blur.common.dto.response.IntrospectResponse;
import com.nimbusds.jose.JOSEException;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {
    private final JwtDecoder jwtDecoderByJwkKeySetUri;

    @NonFinal
    @Value("${jwt.valid-duration}")
    long validDuration;

    @NonFinal
    @Value("${server.servlet.session.cookie.secure}")
    boolean cookieSecure;

    @NonFinal
    @Value("${server.servlet.session.cookie.domain}")
    String cookieDomain;

    AuthenticationService authenticationService;

    @PostMapping("/token")
    ApiResponse<AuthResponse> authenticate(@RequestBody AuthRequest authRequest, HttpServletResponse response) {
        var result = authenticationService.authenticate(authRequest);
        // set access token cookie
        Cookie accessCookie = new Cookie("access_token", result.getToken());
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(cookieSecure);
        accessCookie.setPath("/");
        accessCookie.setMaxAge((int) validDuration);
        accessCookie.setAttribute("SameSite", "Strict");

        if (!cookieDomain.isEmpty()) {
            accessCookie.setDomain(cookieDomain);
        }
        response.addCookie(accessCookie);

        return ApiResponse.<AuthResponse>builder()
                .code(1000)
                .result(AuthResponse.builder().authenticated(true).build())
                .build();
    }

    @PostMapping("/introspect")
    ApiResponse<IntrospectResponse> introspect(@RequestBody IntrospectRequest request)
            throws ParseException, JOSEException {
        return ApiResponse.<IntrospectResponse>builder()
                .result(authenticationService.introspect(request))
                .build();
    }

    @PostMapping("/logout")
    ApiResponse<Void> logout(
            @CookieValue(name = "access_token", required = false) String token, HttpServletResponse response)
            throws ParseException, JOSEException {
        if (token != null) {
            authenticationService.logout(LogoutRequest.builder().token(token).build());
        }
        // clear cookie
        Cookie cookie = new Cookie("access_token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(0); // delete cookie
        response.addCookie(cookie);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/refresh")
    public ApiResponse<Void> refreshToken(
            @CookieValue(name = "access_token") String token, HttpServletResponse response)
            throws ParseException, JOSEException {

        AuthResponse authResponse = authenticationService.refreshToken(
                RefreshRequest.builder().token(token).build());

        // Set new cookie
        Cookie accessCookie = new Cookie("access_token", authResponse.getToken());
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(cookieSecure);
        accessCookie.setPath("/");
        accessCookie.setMaxAge((int) validDuration);
        accessCookie.setAttribute("SameSite", "Strict");

        response.addCookie(accessCookie);

        return ApiResponse.<Void>builder().build();
    }

    // login with google
    @PostMapping("/outbound/authentication")
    ApiResponse<AuthResponse> outboundAuthenticate(@RequestParam("code") String code) {
        var result = authenticationService.outboundAuthenticationService(code);
        return ApiResponse.<AuthResponse>builder().code(1000).result(result).build();
    }
}
