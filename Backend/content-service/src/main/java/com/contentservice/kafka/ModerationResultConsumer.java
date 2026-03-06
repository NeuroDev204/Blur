package com.contentservice.kafka;

import com.contentservice.post.repository.CommentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ModerationResultConsumer {

    private final ObjectMapper objectMapper;
    private final CommentRepository commentRepository;

    @KafkaListener(topics = "comment-moderation-results", groupId = "content-service")
    public void consume(String json) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(json, Map.class);
            String commentId = (String) result.get("commentId");
            boolean isToxic = (Boolean) result.get("isToxic");
            double toxicScore = ((Number) result.get("toxicScore")).doubleValue();
            String modelVersion = (String) result.get("modelVersion");

            commentRepository.findById(commentId).ifPresent(comment -> {
                comment.setModerationConfidence(toxicScore);
                comment.setModelVersion(modelVersion);
                comment.setModeratedAt(LocalDateTime.now());
                if (isToxic) {
                    comment.setContent("[Comment hidden by moderation]");
                }
                commentRepository.save(comment);
            });
        } catch (Exception e) {
        }
    }
}