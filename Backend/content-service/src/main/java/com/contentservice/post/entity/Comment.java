package com.contentservice.post.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.time.Instant;
import java.time.LocalDateTime;

@Node("comment")
@Builder
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class Comment {
    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    String id;
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
