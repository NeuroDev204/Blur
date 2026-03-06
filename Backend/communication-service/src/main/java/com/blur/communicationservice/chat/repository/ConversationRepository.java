package com.blur.communicationservice.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.blur.communicationservice.entity.Conversation;

@Repository
public interface ConversationRepository extends MongoRepository<Conversation, String> {
    Optional<Conversation> findByParticipantsHash(String participantsHash);

    @Query("{'participants.userId' :  ?0}")
    List<Conversation> findAllByParticipantIdsContains(String userId);
}
