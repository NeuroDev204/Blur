package com.contentservice.story.repository;

import com.contentservice.story.entity.Story;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface StoryRepository extends Neo4jRepository<Story, String> {
    List<Story> findAllByAuthorId(String authorId);

    List<Story> findAllByCreatedAtBefore(Instant timestamp);

}
