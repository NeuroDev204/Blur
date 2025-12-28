package org.identityservice.controller;

import java.text.ParseException;

import org.identityservice.dto.request.*;
import org.identityservice.dto.response.AuthResponse;
import org.identityservice.dto.response.IntrospecResponse;
import org.identityservice.service.AuthenticationService;
import org.springframework.web.bind.annotation.*;

import com.nimbusds.jose.JOSEException;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {
    AuthenticationService authenticationService;

    @PostMapping("/token")
    ApiResponse<AuthResponse> authenticate(@RequestBody AuthRequest authRequest) {
        var result = authenticationService.authenticate(authRequest);
        return ApiResponse.<AuthResponse>builder().code(1000).result(result).build();
    }

    @PostMapping("/introspect")
    ApiResponse<IntrospecResponse> introspect(@RequestBody IntrospectRequest introspecRequest)
            throws ParseException, JOSEException {
        var result = authenticationService.introspect(introspecRequest);
        return ApiResponse.<IntrospecResponse>builder().result(result).build();
    }

    @PostMapping("/logout")
    ApiResponse<Void> logout(@RequestBody LogoutRequest logoutRequest) throws ParseException, JOSEException {
        authenticationService.logout(logoutRequest);
        return ApiResponse.<Void>builder().build();
    }

    @PostMapping("/refresh")
    ApiResponse<AuthResponse> refresh(@RequestBody RefreshRequest refreshRequest) throws ParseException, JOSEException {
        var result = authenticationService.refreshToken(refreshRequest);
        return ApiResponse.<AuthResponse>builder().result(result).build();
    }

    // login with google
    @PostMapping("/outbound/authentication")
    ApiResponse<AuthResponse> outboundAuthenticate(@RequestParam("code") String code) {
        var result = authenticationService.outboundAuthenticationService(code);
        return ApiResponse.<AuthResponse>builder().code(1000).result(result).build();
    }
}
