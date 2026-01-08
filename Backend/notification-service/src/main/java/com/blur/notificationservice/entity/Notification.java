package com.blur.notificationservice.entity;

import com.blur.notificationservice.kafka.model.Type;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

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
    String senderId;           // Profile ID (Neo4j)
    String senderUserId;       // ⭐ THÊM: User ID (identity-service)
    String senderName;
    String senderFirstName;
    String senderLastName;
    String receiverId;         // Profile ID (Neo4j)
    String receiverUserId;     // ⭐ THÊM: User ID (identity-service)
    String receiverName;
    String receiverEmail;
    String senderImageUrl;
    Type type;
    String content;
    LocalDateTime timestamp;
    @Builder.Default
    Boolean read = false;
    String entityId;
    String storyId;
}
