package com.contentservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ModerationProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void submit(String commentId, String postId, String userId, String content) {
        try {
            var payload = java.util.Map.of(
                    "commentId", commentId,
                    "postId", postId,
                    "userId", userId,
                    "content", content
            );
            kafkaTemplate.send("comment-moderation-request", objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            throw new IllegalStateException("Publish moderation request failed", e);
        }
    }
}