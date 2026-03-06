package com.blur.communicationservice.dto.request;

// AiChatRequest.java

import lombok.Data;

@Data
public class AiChatRequest {
    private String conversationId;
    private String userId;
    private String message;
}
