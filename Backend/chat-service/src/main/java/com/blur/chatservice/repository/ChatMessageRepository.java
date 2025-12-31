package com.blur.chatservice.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.blur.chatservice.entity.ChatMessage;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findAllByConversationIdOrderByCreatedDateDesc(String conversationId);

    ChatMessage findFirstByConversationIdOrderByCreatedDateDesc(String conversationId);

    Long countByConversationIdAndReadByNotContains(String conversationId, String userId);
}
