package com.blur.userservice.profile.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {

    RedisService redisService;

    public void setOnline() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        redisService.setOnline(userId);
    }

    public void setOffline() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        redisService.setOffline(userId);
    }
}
