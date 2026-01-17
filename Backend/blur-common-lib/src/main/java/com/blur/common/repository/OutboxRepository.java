package com.blur.common.repository;

import com.blur.common.enums.OutboxStatus;
import com.blur.common.outbox.OutboxEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxRepository extends MongoRepository<OutboxEvent, String> {
  List<OutboxEvent> findTop100ByStatusOrderByCreatedAt(OutboxStatus outboxStatus);

  long countByStatus(OutboxStatus outboxStatus);

  List<OutboxEvent> findByStatusOrderByCreatedAtDesc(OutboxStatus status);
}
