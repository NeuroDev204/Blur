package com.contentservice.post.repository;

import com.contentservice.post.entity.CommentLike;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface CommentLikeRepository extends Neo4jRepository<CommentLike, String> {
}
