package com.blur.chatservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.blur.chatservice.entity.Conversation;

@Repository
public interface ConversationRepository extends MongoRepository<Conversation, String> {
    Optional<Conversation> findByParticipantsHash(String participantsHash);

    @Query("{'participants.userId' :  ?0}")
    List<Conversation> findAllByParticipantIdsContains(String userId);
}
