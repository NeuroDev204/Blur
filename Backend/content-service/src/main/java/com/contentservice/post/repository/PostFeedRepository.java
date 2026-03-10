package com.contentservice.post.repository;

import com.contentservice.post.entity.PostFeedItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostFeedRepository extends MongoRepository<PostFeedItem, String> {
  Page<PostFeedItem> findByTargetUserIdOrderByCreatedDateDesc(String targetUserId, Pageable pageable);

  void deleteAllByPostId(String postId);

  void deleteAllByAuthorId(String authorId);
}
