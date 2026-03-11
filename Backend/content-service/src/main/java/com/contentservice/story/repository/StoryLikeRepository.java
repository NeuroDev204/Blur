package com.contentservice.story.repository;


import com.contentservice.story.entity.StoryLike;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoryLikeRepository extends Neo4jRepository<StoryLike, String> {
    StoryLike findByStoryId(String storyId);

    void deleteByStoryIdAndUserId(String storyId, String userId);
}
