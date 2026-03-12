package com.blur.communicationservice.chat.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.blur.communicationservice.entity.ChatMessage;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findAllByConversationIdOrderByCreatedDateDesc(String conversationId);

    ChatMessage findFirstByConversationIdOrderByCreatedDateDesc(String conversationId);

    void deleteBySenderUserId(String userId);

    Long countByConversationIdAndReadByNotContains(String conversationId, String userId);
}
