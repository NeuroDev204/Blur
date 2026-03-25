package com.contentservice.post.repository;

import com.contentservice.post.entity.Post;
import com.contentservice.post.entity.PostLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PostRepository extends Neo4jRepository<Post, String> {

        @Query("MATCH (p:post {userId: $userId}) DETACH DELETE p")
        void deleteAllByUserId(@Param("userId") String userId);

        /** Create the POSTED edge from the author's user_profile node to the Post. */
        @Query("""
                        MATCH (u:user_profile {user_id: $userId})
                        MATCH (p:post {id: $postId})
                        MERGE (u)-[:POSTED {profileId: $profileId, createdAt: $createdAt}]->(p)
                        """)
        void linkPostToAuthor(
                        @Param("userId") String userId,
                        @Param("postId") String postId,
                        @Param("profileId") String profileId,
                        @Param("createdAt") Instant createdAt);

        /**
         * Traverse (user_profile)-[:POSTED]->(Post) instead of a plain property scan.
         */
        @Query("""
                        MATCH (u:user_profile {user_id: $userId})-[:POSTED]->(p:post)
                        RETURN p ORDER BY p.createdAt DESC
                        """)
        List<Post> findAllByUserIdOrderByCreatedAtDesc(@Param("userId") String userId);

        // ─────────────────────────────────────────────────────────────
        // LIKE RELATIONSHIP
        // (user_profile)-[:LIKED_POST {createdAt}]->(Post)
        // ─────────────────────────────────────────────────────────────

        /** Create or ensure the LIKED_POST edge (idempotent via MERGE). */
        @Query("""
                        MATCH (u:user_profile {user_id: $userId})
                        MATCH (p:post {id: $postId})
                        MERGE (u)-[r:LIKED_POST]->(p)
                        ON CREATE SET r.createdAt = $createdAt
                        """)
        void likePost(
                        @Param("userId") String userId,
                        @Param("postId") String postId,
                        @Param("createdAt") Instant createdAt);

        /** Remove the LIKED_POST edge. */
        @Query("""
                        MATCH (u:user_profile {user_id: $userId})-[r:LIKED_POST]->(p:post {id: $postId})
                        DELETE r
                        """)
        void unlikePost(@Param("userId") String userId, @Param("postId") String postId);

        /** Check whether a LIKED_POST edge already exists. */
        @Query("""
                        MATCH (u:user_profile {user_id: $userId})-[r:LIKED_POST]->(p:post {id: $postId})
                        RETURN COUNT(r) > 0
                        """)
        boolean hasUserLikedPost(@Param("userId") String userId, @Param("postId") String postId);

        /**
         * Return all likes on a post as @QueryResult PostLike DTOs.
         * Traverses incoming LIKED_POST edges to gather userId + createdAt from the
         * relationship.
         */
        @Query("""
                        MATCH (u:user_profile)-[r:LIKED_POST]->(p:post {id: $postId})
                        RETURN u.user_id AS userId, r.createdAt AS createdAt
                        """)
        List<PostLike> findLikesByPostId(@Param("postId") String postId);

        // ─────────────────────────────────────────────────────────────
        // SAVE RELATIONSHIP
        // (user_profile)-[:SAVED_POST {savedAt}]->(Post)
        // ─────────────────────────────────────────────────────────────

        /** Create the SAVED_POST edge (idempotent). */
        @Query("""
                        MATCH (u:user_profile {user_id: $userId})
                        MATCH (p:post {id: $postId})
                        MERGE (u)-[r:SAVED_POST]->(p)
                        ON CREATE SET r.savedAt = $savedAt
                        """)
        void savePost(
                        @Param("userId") String userId,
                        @Param("postId") String postId,
                        @Param("savedAt") Instant savedAt);

        /** Remove the SAVED_POST edge. */
        @Query("""
                        MATCH (u:user_profile {user_id: $userId})-[r:SAVED_POST]->(p:post {id: $postId})
                        DELETE r
                        """)
        void unsavePost(@Param("userId") String userId, @Param("postId") String postId);

        /**
         * Traverse (user_profile)-[:SAVED_POST]->(Post) to return all posts
         * saved by the given user — single graph query, no N+1.
         */
        @Query("""
                        MATCH (u:user_profile {user_id: $userId})-[:SAVED_POST]->(p:post)
                        RETURN p ORDER BY p.createdAt DESC
                        """)
        List<Post> findSavedPostsByUserId(@Param("userId") String userId);

        // ─────────────────────────────────────────────────────────────
        // PAGINATION (SDN-native, kept for the /all endpoint)
        // ─────────────────────────────────────────────────────────────
        Page<Post> findAll(Pageable pageable);
}
