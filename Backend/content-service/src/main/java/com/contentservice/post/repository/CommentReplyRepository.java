package com.contentservice.post.repository;

import com.contentservice.post.entity.CommentReply;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CommentReplyRepository extends MongoRepository<CommentReply, String> {
    List<CommentReply> findAllByCommentId(String commentId);

    List<CommentReply> findAllByParentReplyId(String parentReplyId);

}
