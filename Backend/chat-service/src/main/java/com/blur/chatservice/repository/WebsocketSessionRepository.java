package com.blur.chatservice.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.blur.chatservice.entity.WebsocketSession;

@Repository
public interface WebsocketSessionRepository extends MongoRepository<WebsocketSession, String> {

    void deleteBySocketSessionId(String socketSessionId);

    // ✅ Thêm @Query để đảm bảo query đúng với MongoDB
    @Query("{'userId': {$in: ?0}}")
    List<WebsocketSession> findALlByUserIdIn(List<String> userIds);

    List<WebsocketSession> findByUserId(String userId);
}
