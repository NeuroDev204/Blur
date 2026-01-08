package com.postservice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class CommentResponse {
    String id;
    String content;
    String userId;
    String userName;
    String firstName;
    String lastName;
    String postId;
    Instant createdAt;
    Instant updatedAt;

    String commentId;
    String parentReplyId;
}
