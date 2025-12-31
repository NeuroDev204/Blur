package com.blur.chatservice.dto.response;

import java.time.Instant;
import java.util.List;

import com.blur.chatservice.entity.ParticipantInfo;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class ConversationResponse {

    String id;
    String type;
    String participantsHash;
    String conversationAvatar;
    String conversationName;
    List<ParticipantInfo> participants;
    Instant createdDate;
    Instant modifiedDate;
    String lastMessage;
    Instant lastMessageTime;
    String lastMessageSender;
}
