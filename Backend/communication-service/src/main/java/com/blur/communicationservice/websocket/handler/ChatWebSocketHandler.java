package com.blur.communicationservice.websocket.handler;

import java.security.Principal;
import java.util.Map;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import com.blur.communicationservice.chat.service.ChatMessageService;
import com.blur.communicationservice.dto.request.ChatMessageRequest;
import com.blur.communicationservice.dto.response.ChatMessageResponse;
import com.blur.communicationservice.entity.ParticipantInfo;
import com.blur.communicationservice.service.ConversationService;
import com.blur.communicationservice.service.RedisCacheService;
import com.blur.communicationservice.websocket.service.WebSocketNotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketHandler {

    private final ChatMessageService chatMessageService;
    private final ConversationService conversationService;
    private final WebSocketNotificationService wsService;
    private final RedisCacheService redisCacheService;

    /**
     * Client gui: SEND /app/chat.send {conversationId, message, attachments}
     * Server push: /user/{receiverId}/chat/message
     */
    @MessageMapping("/chat.send")
    public void handleSendMessage(@Payload Map<String, Object> data, Principal principal) {
        String senderId = principal.getName();
        String conversationId = (String) data.get("conversationId");
        String message = (String) data.get("message");

        // Deduplication
        String tempMessageId = (String) data.get("messageId");
        if (tempMessageId != null) {
            String dedupeKey = conversationId + ":" + tempMessageId;
            if (redisCacheService.isMessageProcessed(dedupeKey)) {
                return;
            }
            redisCacheService.markMessageAsProcessed(dedupeKey, 3000);
        }

        ChatMessageRequest request = ChatMessageRequest.builder()
                .conversationId(conversationId)
                .message(message)
                .build();

        ChatMessageResponse savedMessage = chatMessageService.create(request, senderId);

        // Tim receiver
        var conversation = conversationService.getConversationById(conversationId);
        String receiverId = conversation.getParticipants().stream()
                .map(ParticipantInfo::getUserId)
                .filter(id -> !id.equals(senderId))
                .findFirst()
                .orElse(null);

        // Push cho ca sender va receiver
        wsService.sendChatMessage(senderId, savedMessage);
        if (receiverId != null) {
            wsService.sendChatMessage(receiverId, savedMessage);
        }
    }

    /**
     * Client gui: SEND /app/chat.typing {conversationId, isTyping}
     * Server push: /user/{receiverId}/chat/typing
     */
    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload Map<String, Object> data, Principal principal) {
        String senderId = principal.getName();
        String conversationId = (String) data.get("conversationId");

        var conversation = conversationService.getConversationById(conversationId);
        String receiverId = conversation.getParticipants().stream()
                .map(ParticipantInfo::getUserId)
                .filter(id -> !id.equals(senderId))
                .findFirst()
                .orElse(null);

        if (receiverId != null) {
            wsService.sendTypingIndicator(
                    receiverId,
                    Map.of(
                            "conversationId", conversationId,
                            "userId", senderId,
                            "isTyping", data.getOrDefault("isTyping", false)));
        }
    }
}
