package com.contentservice.story.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateStoryRequest {
    String content;
    String mediaUrl;
    String thumbnailUrl;
    Instant timestamp;

}
