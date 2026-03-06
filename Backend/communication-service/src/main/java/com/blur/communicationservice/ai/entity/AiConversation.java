package com.blur.communicationservice.ai.entity;

import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "ai_conversations")
public class AiConversation {

    @Id
    private String id;

    private String userId;
    private Instant createdAt;
    private Instant updatedAt;
    private List<AiChatMessage> messages;
}
