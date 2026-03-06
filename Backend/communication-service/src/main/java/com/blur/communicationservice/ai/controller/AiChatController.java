package com.blur.communicationservice.ai.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blur.communicationservice.ai.service.AiConversationService;
import com.blur.communicationservice.dto.request.AiChatRequest;
import com.blur.communicationservice.dto.response.AiChatResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiConversationService aiConversationService;

    @PostMapping("/chat")
    public AiChatResponse chat(@RequestBody AiChatRequest request) {
        return aiConversationService.chat(request);
    }
}
