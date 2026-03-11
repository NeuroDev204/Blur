package com.blur.userservice.profile.controller;

import com.blur.userservice.profile.dto.ApiResponse;
import com.blur.userservice.profile.dto.request.AuthRequest;
import com.blur.userservice.profile.dto.request.IntrospectRequest;
import com.blur.userservice.profile.dto.request.LogoutRequest;
import com.blur.userservice.profile.dto.request.RefreshRequest;
import com.blur.userservice.profile.dto.response.AuthResponse;
import com.blur.userservice.profile.dto.response.IntrospectResponse;
import com.blur.userservice.profile.service.AuthenticationService;
import com.blur.userservice.profile.util.CookieUtil;
import com.nimbusds.jose.JOSEException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {
    AuthenticationService authenticationService;
    CookieUtil cookieUtil;

    @PostMapping("/token")
    ApiResponse<AuthResponse> authenticate(@RequestBody AuthRequest authRequest, HttpServletResponse response) {
        var result = authenticationService.authenticate(authRequest);

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookieUtil.createAccessTokenCookie(result.getToken()).toString());

        return ApiResponse.<AuthResponse>builder()
                .code(1000)
                .result(AuthResponse.builder()
                        .authenticated(result.isAuthenticated())
                        .build())
                .build();
    }

    @PostMapping("/introspect")
    ApiResponse<IntrospectResponse> introspect(
            @CookieValue(name = "access_token", required = false) String tokenFromCookie,
            @RequestBody(required = false) IntrospectRequest introspecRequest)
            throws ParseException, JOSEException {
        String token = tokenFromCookie != null
                ? tokenFromCookie
                : (introspecRequest != null ? introspecRequest.getToken() : null);

        if (token == null) {
            return ApiResponse.<IntrospectResponse>builder()
                    .result(IntrospectResponse.builder().valid(false).build())
                    .build();
        }

        var result = authenticationService.introspect(
                IntrospectRequest.builder().token(token).build());
        return ApiResponse.<IntrospectResponse>builder().result(result).build();
    }

    @PostMapping("/logout")
    ApiResponse<Void> logout(
            @CookieValue(name = "access_token", required = false) String tokenFromCookie,
            @RequestBody(required = false) LogoutRequest logoutRequest,
            HttpServletResponse response)
            throws ParseException, JOSEException {
        String token =
                tokenFromCookie != null ? tokenFromCookie : (logoutRequest != null ? logoutRequest.getToken() : null);

        if (token != null) {
            authenticationService.logout(LogoutRequest.builder().token(token).build());
        }

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookieUtil
                        .createLogoutCookie(CookieUtil.ACCESS_TOKEN_COOKIE_NAME)
                        .toString());
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookieUtil
                        .createLogoutCookie(CookieUtil.REFRESH_TOKEN_COOKIE_NAME)
                        .toString());

        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/refresh")
    ApiResponse<AuthResponse> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshTokenFromCookie,
            @CookieValue(name = "access_token", required = false) String accessTokenFromCookie,
            @RequestBody(required = false) RefreshRequest refreshRequest,
            HttpServletResponse response)
            throws ParseException, JOSEException {
        String token = accessTokenFromCookie != null
                ? accessTokenFromCookie
                : (refreshRequest != null ? refreshRequest.getToken() : null);

        var result = authenticationService.refreshToken(
                RefreshRequest.builder().token(token).build());

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookieUtil.createAccessTokenCookie(result.getToken()).toString());

        return ApiResponse.<AuthResponse>builder()
                .result(AuthResponse.builder().authenticated(true).build())
                .build();
    }

    @PostMapping("/outbound/authentication")
    ApiResponse<AuthResponse> outboundAuthenticate(@RequestParam("code") String code, HttpServletResponse response) {
        var result = authenticationService.outboundAuthenticationService(code);

        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookieUtil.createAccessTokenCookie(result.getToken()).toString());

        return ApiResponse.<AuthResponse>builder()
                .code(1000)
                .result(AuthResponse.builder().authenticated(true).build())
                .build();
    }
}
