package com.contentservice.post.repository;

import com.contentservice.post.entity.CommentReply;
import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.util.List;

public interface CommentReplyRepository extends Neo4jRepository<CommentReply, String> {
    List<CommentReply> findAllByCommentId(String commentId);

    List<CommentReply> findAllByParentReplyId(String parentReplyId);

}
