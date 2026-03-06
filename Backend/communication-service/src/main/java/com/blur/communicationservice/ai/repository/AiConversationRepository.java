package com.blur.communicationservice.ai.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.blur.communicationservice.ai.entity.AiConversation;

@Repository
public interface AiConversationRepository extends MongoRepository<AiConversation, String> {

    List<AiConversation> findByUserIdOrderByUpdatedAtDesc(String userId);
}
