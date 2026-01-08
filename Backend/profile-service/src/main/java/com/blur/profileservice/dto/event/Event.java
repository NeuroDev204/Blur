package com.blur.profileservice.dto.event;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class Event {
    String senderId;           // Profile ID (Neo4j UUID)
    String senderUserId;       // ⭐ THÊM: User ID từ identity-service
    String senderName;
    String senderFirstName;    // ⭐ THÊM
    String senderLastName;     // ⭐ THÊM
    String senderImageUrl;     // ⭐ THÊM: Avatar
    String receiverId;         // Profile ID
    String receiverUserId;     // ⭐ THÊM
    String receiverEmail;
    String receiverName;
    LocalDateTime timestamp;
}
