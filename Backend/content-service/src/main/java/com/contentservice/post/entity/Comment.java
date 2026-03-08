package com.contentservice.post.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import com.contentservice.post.enums.ModerationStatus;

import java.time.Instant;
import java.time.LocalDateTime;

@Document(value = "comment")
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class Comment {
    @MongoId
    String id;
    String postId;
    String userId;
    String firstName;
    String lastName;
    String content;
    String originalContent; // noi dung goc truoc khi moderate

    String moderationStatus;
    String jobId;
    Double moderationConfidence;
    String modelVersion;
    LocalDateTime moderatedAt;
    Instant createdAt;
    Instant updatedAt;
}
