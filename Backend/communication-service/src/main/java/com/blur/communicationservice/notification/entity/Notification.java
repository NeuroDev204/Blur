package com.blur.communicationservice.notification.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.blur.communicationservice.notification.enums.NotificationType;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Document(collection = "notifications")
@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class Notification {
    @Id
    String id;

    String postId;
    String conversationId;
    String senderId;
    String senderName;
    String senderFirstName;
    String senderLastName;
    String receiverId;
    String receiverName;
    String receiverEmail;
    String senderImageUrl;
    NotificationType type;
    String content;
    LocalDateTime timestamp;

    @Builder.Default
    Boolean read = false;
}
