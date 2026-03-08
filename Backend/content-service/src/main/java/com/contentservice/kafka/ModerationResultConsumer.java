package com.contentservice.kafka;

import com.contentservice.post.enums.ModerationStatus;
import com.contentservice.post.exception.AppException;
import com.contentservice.post.exception.ErrorCode;
import com.contentservice.post.repository.CommentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
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
			String status = (String) result.get("status");
			double confidence = ((Number) result.get("confidence")).doubleValue();
			double toxicScore = ((Number) result.get("toxicScore")).doubleValue();
			String modelVersion = (String) result.get("modelVersion");

			commentRepository.findById(commentId).ifPresent(comment -> {
				comment.setModerationStatus(status);
				comment.setModerationConfidence(toxicScore);
				comment.setModerationConfidence(confidence);
				comment.setModelVersion(modelVersion); 
				comment.setModeratedAt(LocalDateTime.now());
				if ("REJECTED".equals(status)) {
					comment.setContent("[Binh luan da bi an boi he thong kiem duyet]");
				}
				commentRepository.save(comment);
			});
		} catch (AppException | JsonProcessingException e ) {
			throw new AppException(ErrorCode.COMMENT_PROCESS_MODERATION_FAILED);
		}
	}
}