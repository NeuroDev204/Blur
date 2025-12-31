package com.blur.chatservice.entity;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Document(collection = "conversation")
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {
    @Id
    String id;

    String aiConversationId;

    String type; // GROUP, DIRECT
    Boolean aiEnabled;

    @Indexed(unique = true)
    String participantsHash;

    List<ParticipantInfo> participants;
    Instant createdDate;
    Instant modifiedDate;
    String lastMessage;
    Instant lastMessageTime;
    String lastMessageSender;
}
