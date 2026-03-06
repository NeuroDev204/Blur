package com.contentservice.post.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;

@Data
@Builder
@Document(value = "comment-reply")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentReply {
    @MongoId
    String id;
    String userId;
    String postId;
    String userName;
    String content;
    String commentId;
    String parentReplyId;
    Instant createdAt;
    Instant updatedAt;
}
