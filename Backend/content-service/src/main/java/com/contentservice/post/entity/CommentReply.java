package com.contentservice.post.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Node("comment_reply")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentReply {
    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    String id;
    String userId;
    String userName;
    String content;
    Instant createdAt;
    Instant updatedAt;
}
