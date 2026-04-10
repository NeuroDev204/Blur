package com.contentservice.story.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

/**
 * Plain DTO — NOT a Neo4j node.
 * Holds properties projected from the graph relationship:
 *   (user_profile)-[:LIKED_STORY {userId, createdAt}]->(story)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StoryLike {
    String userId;
    Instant createdAt;
}
