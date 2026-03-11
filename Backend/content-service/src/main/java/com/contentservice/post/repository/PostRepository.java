package com.contentservice.post.repository;


import com.contentservice.post.entity.Post;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends Neo4jRepository<Post, String> {
    List<Post> findAllByUserIdOrderByCreatedAtDesc(String userId);
}
