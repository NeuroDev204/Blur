package com.contentservice.post.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Node("post_feed")
public class PostFeedItem {
  @Id
  @GeneratedValue(generatorClass = UUIDStringGenerator.class)
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
