package com.blur.notificationservice.kafka.consumer;

import com.blur.notificationservice.kafka.handler.EventHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.List;
import jakarta.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class EventListener {
    private final List<EventHandler<?>> handlers;

    @KafkaListener(
            topics = {"user-follow-events", "user-like-events", "user-comment-events",
                        "user-reply-comment-events","user-like-story-events","user-react-story-events"},
            groupId = "notification-service")
    public void listen(ConsumerRecord<String, String> record,
                       @Header(KafkaHeaders.RECEIVED_TOPIC) String topic)
            throws JsonProcessingException {

        String message = record.value(); // Đây là JSON string
        log.info("📌 Received topic: {}", topic);

        for (EventHandler<?> handler : handlers) {
            if (handler.canHandle(topic)) {
                handler.handleEvent(message);
                return;
            }
        }
        log.warn("No handler found for topic: {}", topic);
    }
    @PostConstruct
    public void init() {
        log.info("✅ Loaded {} handlers:", handlers.size());
        handlers.forEach(h -> log.info(" - {}", h.getClass().getName()));
    }

}
