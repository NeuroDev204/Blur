package com.contentservice.post.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

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
    String jobId; // uuid cho polling
    Double moderationConfidence; // 0.0 -1.0
    String modelVersion;
    LocalDateTime moderatedAt;
    Instant createdAt;
    Instant updatedAt;
}
