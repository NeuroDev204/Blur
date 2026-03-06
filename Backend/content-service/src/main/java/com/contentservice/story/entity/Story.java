package com.contentservice.story.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;


@Document("stories")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Story {
    @MongoId
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
