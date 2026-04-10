package com.contentservice.post.dto.event;

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
    String senderFirstName;
    String senderLastName;
    String senderImageUrl;
    String receiverId;
    String receiverEmail;
    String receiverName;
    LocalDateTime timestamp;
}
