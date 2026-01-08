package com.postservice.repository;

import com.postservice.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends MongoRepository<Post, String> {
    List<Post> findAllByUserIdOrderByCreatedAtDesc(String userId);

    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<Post> findPostById(String postId);

    List<Post> findAllByOrderByCreatedAtDesc();
}
