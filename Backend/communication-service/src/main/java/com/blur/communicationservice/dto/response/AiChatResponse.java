package com.blur.communicationservice.dto.response;

import lombok.Data;

@Data
public class AiChatResponse {
    private String conversationId;
    private String response;
    private boolean success;
    private String error;
}
