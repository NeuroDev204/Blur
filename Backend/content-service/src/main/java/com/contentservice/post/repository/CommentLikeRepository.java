package com.contentservice.post.repository;

import com.contentservice.post.entity.CommentLike;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface CommentLikeRepository extends MongoRepository<CommentLike, String> {
}
