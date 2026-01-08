package com.blur.chatservice.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.blur.chatservice.dto.ApiResponse;
import com.blur.chatservice.dto.request.ConversationRequest;
import com.blur.chatservice.dto.response.ConversationResponse;
import com.blur.chatservice.service.ChatMessageService;
import com.blur.chatservice.service.ConversationService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("conversations")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConversationController {
    ConversationService conversationService;
    ChatMessageService chatMessageService;

    @PostMapping("/create")
    ApiResponse<ConversationResponse> createConversation(@RequestBody ConversationRequest conversationRequest) {
        return ApiResponse.<ConversationResponse>builder()
                .result(conversationService.createConversation(conversationRequest))
                .build();
    }

    @GetMapping("/my-conversations")
    ApiResponse<List<ConversationResponse>> myConversations() {
        return ApiResponse.<List<ConversationResponse>>builder()
                .result(conversationService.myConversations())
                .build();
    }

    @GetMapping("/{id}/unread-count")
    ApiResponse<Integer> unreadCount(@PathVariable String id) {
        return ApiResponse.<Integer>builder()
                .result(chatMessageService.unreadCount(id))
                .build();
    }

    @PutMapping("/mark-as-read")
    ApiResponse<String> markAsRead(@RequestParam("conversationId") String conversationId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();
        return ApiResponse.<String>builder()
                .result(chatMessageService.markAsRead(conversationId, userId))
                .build();
    }

    @PutMapping("/{conversationId}/ai/toggle")
    ApiResponse<ConversationResponse> toggleAI(@PathVariable String conversationId, @RequestParam Boolean enabled) {
        return ApiResponse.<ConversationResponse>builder()
                .result(conversationService.toggleAI(conversationId, enabled))
                .build();
    }

    @DeleteMapping("")
    ApiResponse<String> deleteConversation(@RequestParam("conversationId") String conversationId) {
        return ApiResponse.<String>builder()
                .result(conversationService.deleteConversation(conversationId))
                .build();
    }
}
