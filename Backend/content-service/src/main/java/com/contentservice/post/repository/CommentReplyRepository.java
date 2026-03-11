package com.contentservice.post.repository;

import com.contentservice.post.entity.CommentReply;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface CommentReplyRepository extends Neo4jRepository<CommentReply, String> {

    // ─────────────────────────────────────────────────────────────
    // REPLY → COMMENT RELATIONSHIP
    // (comment_reply)-[:REPLIES_TO]->(comment)
    // ─────────────────────────────────────────────────────────────

    /** Create the REPLIES_TO edge from a reply to its parent comment. */
    @Query("""
            MATCH (r:comment_reply {id: $replyId}), (c:comment {id: $commentId})
            MERGE (r)-[:REPLIES_TO]->(c)
            """)
    void linkReplyToComment(@Param("replyId") String replyId, @Param("commentId") String commentId);

    /** Traverse (comment_reply)-[:REPLIES_TO]->(comment) to get all direct replies. */
    @Query("""
            MATCH (r:comment_reply)-[:REPLIES_TO]->(c:comment {id: $commentId})
            RETURN r ORDER BY r.createdAt ASC
            """)
    List<CommentReply> findAllByCommentId(@Param("commentId") String commentId);

    /**
     * Traverse REPLIES_TO to find the parent comment ID of a reply.
     * Used for cache-key resolution without a stored commentId property.
     */
    @Query("""
            MATCH (r:comment_reply {id: $replyId})-[:REPLIES_TO]->(c:comment)
            RETURN c.id
            """)
    String findCommentIdByReplyId(@Param("replyId") String replyId);

    // ─────────────────────────────────────────────────────────────
    // NESTED REPLY → REPLY RELATIONSHIP
    // (comment_reply)-[:NESTED_REPLY_OF]->(comment_reply)
    // ─────────────────────────────────────────────────────────────

    /** Create the NESTED_REPLY_OF edge from a child reply to its parent reply. */
    @Query("""
            MATCH (child:comment_reply {id: $childId}), (parent:comment_reply {id: $parentId})
            MERGE (child)-[:NESTED_REPLY_OF]->(parent)
            """)
    void linkReplyToParentReply(@Param("childId") String childId, @Param("parentId") String parentId);

    /** Traverse NESTED_REPLY_OF to get all children of a parent reply. */
    @Query("""
            MATCH (child:comment_reply)-[:NESTED_REPLY_OF]->(parent:comment_reply {id: $parentReplyId})
            RETURN child ORDER BY child.createdAt ASC
            """)
    List<CommentReply> findAllByParentReplyId(@Param("parentReplyId") String parentReplyId);

    /**
     * Traverse NESTED_REPLY_OF to find the parent reply ID.
     * Used for cache-key resolution without a stored parentReplyId property.
     */
    @Query("""
            MATCH (child:comment_reply {id: $replyId})-[:NESTED_REPLY_OF]->(parent:comment_reply)
            RETURN parent.id
            """)
    String findParentReplyIdByReplyId(@Param("replyId") String replyId);

    // ─────────────────────────────────────────────────────────────
    // AUTHOR RELATIONSHIP
    // (user_profile)-[:REPLIED {createdAt}]->(comment_reply)
    // ─────────────────────────────────────────────────────────────

    /** Link the replier's user_profile node to the reply. */
    @Query("""
            MATCH (u:user_profile {user_id: $userId}), (r:comment_reply {id: $replyId})
            MERGE (u)-[:REPLIED {createdAt: $createdAt}]->(r)
            """)
    void linkReplyToUser(
            @Param("userId") String userId,
            @Param("replyId") String replyId,
            @Param("createdAt") Instant createdAt
    );
}
