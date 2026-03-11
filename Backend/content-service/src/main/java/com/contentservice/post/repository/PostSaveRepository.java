package com.contentservice.post.repository;

import com.contentservice.post.entity.PostSave;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostSaveRepository extends Neo4jRepository<PostSave, String> {
    PostSave findByPostId(String postId);
}
