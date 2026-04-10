package com.contentservice.story.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.time.Instant;


@Node("story")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Story {
    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    String id;
    String content;
    String authorId;
    String mediaUrl;
    Instant timestamp;
    String firstName;
    String lastName;
    String thumbnailUrl;
    Instant createdAt;
    Instant updatedAt;
}
