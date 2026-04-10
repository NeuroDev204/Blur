package com.contentservice.outbox.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import com.contentservice.outbox.entity.OutboxEvent;

import feign.Param;

@Repository
public interface OutboxRepository extends Neo4jRepository<OutboxEvent,String>{
  @Query("""
      MATCH (e:outbox_event)
      WHERE e.published = false AND e.retryCount < 5
      RETURN e
      ORDER BY e.createdAt ASC
      LIMIT 100
      """)
  List<OutboxEvent> findUnpublishedEvents();

  @Query("""
      MATCH (e:outbox_event)
      WHERE e.published = true AND e.publishedAt < $before
      DELETE e
      """)
  void deletePublishedEventsBefore(@Param("before") Instant before);
}
