package com.contentservice.post.repository;

import com.contentservice.post.entity.Comment;
import com.contentservice.post.entity.CommentLike;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface CommentRepository extends Neo4jRepository<Comment, String> {

        @Query("MATCH (c:comment {userId: $userId}) DETACH DELETE c")
        void deleteAllByUserId(@Param("userId") String userId);

        /** Create the COMMENTS_ON edge from the comment node to its post. */
        @Query("""
                        MATCH (c:comment {id: $commentId}), (p:post {id: $postId})
                        MERGE (c)-[:COMMENTS_ON]->(p)
                        """)
        void linkCommentToPost(@Param("commentId") String commentId, @Param("postId") String postId);

        /** Traverse (comment)-[:COMMENTS_ON]->(Post) to get all comments for a post. */
        @Query("""
                        MATCH (c:comment)-[:COMMENTS_ON]->(p:post {id: $postId})
                        RETURN c ORDER BY c.createdAt ASC
                        """)
        List<Comment> findAllByPostId(@Param("postId") String postId);

        /**
         * Traverse the COMMENTS_ON edge to find the parent post ID of a comment.
         * Used for cache-key resolution without a stored postId property.
         */
        @Query("""
                        MATCH (c:comment {id: $commentId})-[:COMMENTS_ON]->(p:post)
                        RETURN p.id
                        """)
        String findPostIdByCommentId(@Param("commentId") String commentId);

        // ─────────────────────────────────────────────────────────────
        // AUTHOR RELATIONSHIP
        // (user_profile)-[:COMMENTED {createdAt}]->(comment)
        // ─────────────────────────────────────────────────────────────

        /** Link the commenter's user_profile node to the comment. */
        @Query("""
                        MATCH (u:user_profile {user_id: $userId}), (c:comment {id: $commentId})
                        MERGE (u)-[:COMMENTED {createdAt: $createdAt}]->(c)
                        """)
        void linkCommentToUser(
                        @Param("userId") String userId,
                        @Param("commentId") String commentId,
                        @Param("createdAt") Instant createdAt);

        // ─────────────────────────────────────────────────────────────
        // LIKE RELATIONSHIP
        // (user_profile)-[:LIKED_COMMENT {createdAt}]->(comment)
        // ─────────────────────────────────────────────────────────────

        /** Create the LIKED_COMMENT edge (idempotent). */
        @Query("""
                        MATCH (u:user_profile {user_id: $userId}), (c:comment {id: $commentId})
                        MERGE (u)-[r:LIKED_COMMENT]->(c)
                        ON CREATE SET r.createdAt = $createdAt
                        """)
        void likeComment(
                        @Param("userId") String userId,
                        @Param("commentId") String commentId,
                        @Param("createdAt") Instant createdAt);

        /** Remove the LIKED_COMMENT edge. */
        @Query("""
                        MATCH (u:user_profile {user_id: $userId})-[r:LIKED_COMMENT]->(c:comment {id: $commentId})
                        DELETE r
                        """)
        void unlikeComment(@Param("userId") String userId, @Param("commentId") String commentId);

        /** Return all likes on a comment as @QueryResult CommentLike DTOs. */
        @Query("""
                        MATCH (u:user_profile)-[r:LIKED_COMMENT]->(c:comment {id: $commentId})
                        RETURN u.user_id AS userId, r.createdAt AS createdAt
                        """)
        List<CommentLike> findLikesByCommentId(@Param("commentId") String commentId);
}
