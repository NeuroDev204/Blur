package com.contentservice.post.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

/**
 * Plain DTO — NOT a Neo4j node.
 * Holds properties projected from the graph relationship:
 *   (user_profile)-[:LIKED_COMMENT {userId, createdAt}]->(comment)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentLike {
    String userId;
    Instant createdAt;
}
