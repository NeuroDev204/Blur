package com.contentservice.post.entity;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;

@Data
@Builder
@Document(value = "post-like")
@CompoundIndex(name = "user_post_unique", def = "{'userId': 1, 'postId': 1}", unique = true)

@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostLike {
    @MongoId
    String id;
    String postId;
    String userId;
    Instant createdAt;
}
