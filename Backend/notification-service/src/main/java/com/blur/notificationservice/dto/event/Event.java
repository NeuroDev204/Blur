package com.blur.notificationservice.dto.event;

import com.blur.notificationservice.kafka.model.Type;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {
    String postId;
    String senderId;
    String senderUserId;       // ⭐ THÊM
    String senderName;
    String senderFirstName;    // ⭐ THÊM
    String senderLastName;     // ⭐ THÊM
    String senderImageUrl;     // ⭐ THÊM
    String receiverId;
    String receiverUserId;     // ⭐ THÊM
    String receiverEmail;
    String receiverName;
    LocalDateTime timestamp;
    String action;
    String storyId;
    String reactionType;
    Integer viewCount;
}