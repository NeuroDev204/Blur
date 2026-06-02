package com.contentservice.post.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)

public class PostResponse {
    String id;
    String userId;
    String profileId;
    String userName;
    String firstName;
    String lastName;
    String userImageUrl;
    String content;
    List<String> mediaUrls;
    Instant createdAt;
    Instant updatedAt;
}
