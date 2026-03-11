package com.contentservice.post.repository;

import com.contentservice.post.entity.Comment;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.List;

public interface CommentRepository extends Neo4jRepository<Comment, String> {
    List<Comment> findAllByPostId(String postId);
}
