package com.blur.communicationservice.dto.response;

import java.time.Instant;
import java.util.List;

import com.blur.communicationservice.entity.MediaAttachment;
import com.blur.communicationservice.entity.ParticipantInfo;
import com.blur.communicationservice.enums.MessageType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class ChatMessageResponse {
    String id;
    String tempMessageId;
    String conversationId;
    String message;
    MessageType messageType;
    List<MediaAttachment> attachments;
    ParticipantInfo sender;
    Instant createdDate;
    Boolean me;
    Boolean isRead;
    List<String> readBy;
}
