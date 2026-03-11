package com.contentservice.post.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

/**
 * Plain DTO — NOT a Neo4j node.
 * Holds properties projected from the graph relationship:
 *   (user_profile)-[:SAVED_POST {userId, savedAt}]->(Post)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostSave {
    String userId;
    Instant savedAt;
}
