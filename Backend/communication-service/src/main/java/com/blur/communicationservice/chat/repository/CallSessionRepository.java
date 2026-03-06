package com.blur.communicationservice.chat.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.blur.communicationservice.entity.CallSession;

public interface CallSessionRepository extends MongoRepository<CallSession, String> {
    // Lấy call history của user
    @Query("{ $or: [ { 'callerId': ?0 }, { 'receiverId': ?0 } ] }")
    Page<CallSession> findByCallerIdOrReceiverId(String userId, Pageable pageable);

    // Lấy missed calls
    @Query("{ 'receiverId': ?0, 'status': 'MISSED' }")
    List<CallSession> findMissedCallsByReceiverId(String userId);

    // Đếm missed calls chưa đọc
    @Query(value = "{ 'receiverId': ?0, 'status': 'MISSED' }", count = true)
    long countMissedCallsByReceiverId(String userId);

    // Tìm active call của user
    @Query("{ $or: [ { 'callerId': ?0 }, { 'receiverId': ?0 } ], "
            + "'status': { $in: ['INITIATING', 'RINGING', 'ANSWERED'] } }")
    Optional<CallSession> findActiveCallByUserId(String userId);

    // Statistics: Tổng số cuộc gọi theo ngày
    @Query("{ 'createdAt': { $gte: ?0, $lt: ?1 } }")
    List<CallSession> findCallsByDateRange(LocalDateTime start, LocalDateTime end);
}
