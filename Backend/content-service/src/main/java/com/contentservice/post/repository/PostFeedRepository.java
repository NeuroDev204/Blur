package com.contentservice.post.repository;

import com.contentservice.post.entity.PostFeedItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostFeedRepository extends Neo4jRepository<PostFeedItem, String> {
  Page<PostFeedItem> findByTargetUserIdOrderByCreatedDateDesc(String targetUserId, Pageable pageable);

  void deleteAllByPostId(String postId);

  void deleteAllByAuthorId(String authorId);

  boolean existsByPostIdAndTargetUserId(String postId, String targetUserId);
}
