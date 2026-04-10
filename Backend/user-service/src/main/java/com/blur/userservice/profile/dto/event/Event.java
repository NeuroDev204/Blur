package com.blur.userservice.profile.dto.event;

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
    String senderName;
    String receiverId;
    String receiverEmail;
    String receiverName;
    LocalDateTime timestamp;
}
