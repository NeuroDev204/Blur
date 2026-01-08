package com.example.storyservice.dto.event;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class Event {
    String senderId;
    String senderUserId;
    String senderName;
    String senderFirstName;
    String senderLastName;
    String senderImageUrl;

    String receiverId;
    String receiverUserId;
    String receiverEmail;
    String receiverName;

    LocalDateTime timestamp;
    String action;        // "REACT" | "EXPIRED_SUMMARY"
    String storyId;
    String reactionType;  // "LIKE" | "LOVE" | "HAHA"...
    Long viewCount;       // dùng cho summary

}
