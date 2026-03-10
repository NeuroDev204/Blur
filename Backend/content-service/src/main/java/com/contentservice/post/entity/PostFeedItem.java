package com.contentservice.post.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Document("post_feed")
@CompoundIndex(name = "idx_feed_user_time", def = "{'targetUserId' : 1 , 'createdDate':-1}")

public class PostFeedItem {
  @Id
  String id;

  // post data
  String postId; // tro ve post goc
  String content;
  List<String> imageUrls;
  String videoUrl;

  // author data
  String authorId;
  String authorUsername;
  String authorFirstName;
  String authorLastName;
  String authorAvatar;

  //engagement counts
  int likeCount;
  int commentCount;
  int shareCount;

  // feed targeting
  String targetUserId;
  LocalDateTime createdDate;
  LocalDateTime updatedDate;
}
