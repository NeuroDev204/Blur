package com.blur.common.event;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor(force = true)
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentCreatedEvent extends BaseEvent {
  String commentId;
  String postId;
  String content;
  String parentCommentId;
  String authorId;

  String authorName;
  String authorFirstName;
  String authorLastName;
  String authorImageUrl;


  String postOwnerId;
  String postOwnerEmail;
  String postOwnerName;
}
