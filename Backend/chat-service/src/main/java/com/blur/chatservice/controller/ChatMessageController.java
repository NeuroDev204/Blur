package com.blur.chatservice.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.blur.chatservice.dto.ApiResponse;
import com.blur.chatservice.dto.request.ChatMessageRequest;
import com.blur.chatservice.dto.response.ChatMessageResponse;
import com.blur.chatservice.service.ChatMessageService;
import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatMessageController {
    ChatMessageService chatMessageService;

    @PostMapping("/create")
    ApiResponse<ChatMessageResponse> create(@RequestBody @Valid ChatMessageRequest chatMessageRequest)
            throws JsonProcessingException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        return ApiResponse.<ChatMessageResponse>builder()
                .result(chatMessageService.create(chatMessageRequest, userId))
                .build();
    }

    @GetMapping
    ApiResponse<List<ChatMessageResponse>> getMessages(@RequestParam("conversationId") String conversationId) {
        return ApiResponse.<List<ChatMessageResponse>>builder()
                .result(chatMessageService.getMessages(conversationId))
                .build();
    }
}
