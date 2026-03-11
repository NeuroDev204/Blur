package com.contentservice.story.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.time.Instant;

@Node("story_like")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StoryLike {
    @Id
    @GeneratedValue(generatorClass = UUIDStringGenerator.class)
    String id;
    String storyId;
    String userId;
    Instant createdAt;
    Instant updatedAt;
}
