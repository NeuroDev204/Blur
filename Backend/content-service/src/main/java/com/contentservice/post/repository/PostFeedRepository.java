package com.contentservice.post.repository;

import com.contentservice.post.entity.PostFeedItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PostFeedRepository extends Neo4jRepository<PostFeedItem, String> {
  Page<PostFeedItem> findByTargetUserIdOrderByCreatedDateDesc(String targetUserId, Pageable pageable);

  void deleteAllByPostId(String postId);

  void deleteAllByAuthorId(String authorId);

  boolean existsByPostIdAndTargetUserId(String postId, String targetUserId);

  /**
   * Delete feed items whose underlying post no longer exists (orphans left behind by
   * deletes that happened before fan-out cleanup was in place). Returns how many were removed.
   */
  @Query("""
          MATCH (f:post_feed)
          OPTIONAL MATCH (p:post {id: f.postId})
          WITH f, p
          WHERE p IS NULL
          WITH collect(f) AS orphans
          FOREACH (x IN orphans | DETACH DELETE x)
          RETURN size(orphans)
          """)
  long deleteOrphanedFeedItems();
}
