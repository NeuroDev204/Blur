package com.contentservice.resilience;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CommunicationServiceFallBack {
    public void sendModerationNotificationFallback(Object request, Throwable throwable) {
        log.warn("Communication Service không khả dụng, moderation notification bị bỏ qua. Lỗi: {}",
                throwable.getMessage());
    }
}
