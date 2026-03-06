package com.contentservice.story.repository;

import com.contentservice.story.entity.Story;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface StoryRepository extends MongoRepository<Story, String> {
    List<Story> findAllByAuthorId(String authorId);

    @Query("{'createdAt': {$lt: ?0}}")
    List<Story> findAllByCreatedAtBefore(Instant timestamp);

}
