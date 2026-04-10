package com.blur.userservice.profile.controller;

import com.blur.userservice.profile.dto.ApiResponse;
import com.blur.userservice.profile.service.AuthenticationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {
    AuthenticationService authenticationService;

    @PostMapping("/online")
    ApiResponse<Void> setOnline() {
        authenticationService.setOnline();
        return ApiResponse.<Void>builder()
                .code(1000)
                .build();
    }

    @PostMapping("/offline")
    ApiResponse<Void> setOffline() {
        authenticationService.setOffline();
        return ApiResponse.<Void>builder()
                .code(1000)
                .build();
    }
}
