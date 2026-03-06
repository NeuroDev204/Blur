package com.blur.communicationservice.ai.entity;

import java.time.Instant;

import lombok.Data;

@Data
public class AiChatMessage {
    private String role; // "user" hoac "assistant"
    private String content;
    private Instant timestamp;
}
