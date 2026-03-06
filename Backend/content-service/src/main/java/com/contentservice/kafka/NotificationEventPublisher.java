package com.contentservice.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishLikeEvent(Object payload) {
        publish("user-like-events", payload);
    }

    public void publishCommentEvent(Object payload) {
        publish("user-comment-events", payload);
    }

    public void publishReplyCommentEvent(Object payload) {
        publish("user-reply-comment-events", payload);
    }

    public void publishLikeStoryEvent(Object payload) {
        publish("user-like-story-events", payload);
    }

    private void publish(String topic, Object payload) {
        try {
            kafkaTemplate.send(topic, objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize event", e);
        }
    }
}