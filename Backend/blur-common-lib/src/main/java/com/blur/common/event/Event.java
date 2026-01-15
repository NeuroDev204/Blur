package com.blur.common.event;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class Event {
  String postId;
  String senderId;
  String senderName;
  String senderUserId;
  String receiverUserId;
  String senderFirstName;
  String senderLastName;
  String senderImageUrl;
  String receiverId;
  String receiverEmail;
  String receiverName;
  LocalDateTime timestamp;
  String action;
  String storyId;
  String reactionType;
  Long viewCount;
}
