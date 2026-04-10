package com.contentservice.resilience;

import com.contentservice.client.CommunicationServiceClient;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResilientCommunicationClient {
    private final CommunicationServiceClient communicationServiceClient;
    private final CommunicationServiceFallBack fallBack;

    @Bulkhead(name = "communicationServiceBH")
    @CircuitBreaker(name = "communicationServiceCB", fallbackMethod = "sendModerationNotificationFallback")
    @Retry(name = "communicationServiceRetry")
    public void sendModerationNotification(Object request) {
        communicationServiceClient.sendModerationUpdate((Map<String, String>) request);
    }

    private void sendModerationNotificationFallback(Object request, Throwable throwable) {
        fallBack.sendModerationNotificationFallback(request, throwable);
    }


}
