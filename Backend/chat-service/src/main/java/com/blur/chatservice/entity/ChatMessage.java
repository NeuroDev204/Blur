package com.blur.chatservice.entity;

import java.time.Instant;
import java.util.List;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import com.blur.chatservice.enums.MessageType;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Document(collection = "chat_messages")
@Builder
@Data
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class ChatMessage {
    @MongoId
    String id;

    @Indexed
    String conversationId;

    String message;
    ParticipantInfo sender;

    @Indexed
    Instant createdDate;

    MessageType messageType;
    List<MediaAttachment> attachments;
    Boolean isRead;
    List<String> readBy;
}
