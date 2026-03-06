package com.blur.communicationservice.websocket.handler;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import com.blur.communicationservice.chat.service.ChatMessageService;
import com.blur.communicationservice.dto.request.ChatMessageRequest;
import com.blur.communicationservice.dto.response.ChatMessageResponse;
import com.blur.communicationservice.entity.ParticipantInfo;
import com.blur.communicationservice.notification.entity.Notification;
import com.blur.communicationservice.notification.enums.NotificationType;
import com.blur.communicationservice.notification.service.NotificationService;
import com.blur.communicationservice.service.ConversationService;
import com.blur.communicationservice.service.RedisCacheService;
import com.blur.communicationservice.websocket.service.WebSocketNotificationService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketHandler {

    private final ChatMessageService chatMessageService;
    private final ConversationService conversationService;
    private final WebSocketNotificationService wsService;
    private final RedisCacheService redisCacheService;
    private final NotificationService notificationService;

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
        ChatMessageResponse senderMessage = copyMessageForUser(savedMessage, true, tempMessageId);

        // Tim receiver
        var conversation = conversationService.getConversationById(conversationId);
        List<String> receiverIds = conversation.getParticipants().stream()
                .map(ParticipantInfo::getUserId)
                .filter(id -> !id.equals(senderId))
                .toList();

        // Push cho ca sender va receiver
        wsService.sendChatMessage(senderId, senderMessage);
        for (String receiverId : receiverIds) {
            ChatMessageResponse receiverMessage = copyMessageForUser(savedMessage, false, null);
            wsService.sendChatMessage(receiverId, receiverMessage);
            sendMessageNotification(receiverId, savedMessage);
        }
    }

    private ChatMessageResponse copyMessageForUser(ChatMessageResponse source, boolean isMe, String tempMessageId) {
        return ChatMessageResponse.builder()
                .id(source.getId())
                .tempMessageId(tempMessageId)
                .conversationId(source.getConversationId())
                .message(source.getMessage())
                .messageType(source.getMessageType())
                .attachments(source.getAttachments())
                .sender(source.getSender())
                .createdDate(source.getCreatedDate())
                .me(isMe)
                .isRead(source.getIsRead())
                .readBy(source.getReadBy())
                .build();
    }

    private void sendMessageNotification(String receiverId, ChatMessageResponse savedMessage) {
        if (receiverId == null || savedMessage.getSender() == null) {
            return;
        }

        Notification notification = Notification.builder()
                .conversationId(savedMessage.getConversationId())
                .senderId(savedMessage.getSender().getUserId())
                .senderName(buildSenderName(savedMessage))
                .receiverId(receiverId)
                .senderImageUrl(savedMessage.getSender().getAvatar())
                .type(NotificationType.Message)
                .content(buildNotificationContent(savedMessage))
                .timestamp(LocalDateTime.now())
                .read(false)
                .build();

        notificationService.save(notification);

        if (redisCacheService.isUserOnline(receiverId)) {
            wsService.sendNotification(notification);
        }
    }

    private String buildSenderName(ChatMessageResponse message) {
        if (message.getSender() == null) {
            return "Người dùng";
        }

        String firstName = message.getSender().getFirstName();
        String lastName = message.getSender().getLastName();
        String fullName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();

        if (!fullName.isBlank()) {
            return fullName;
        }

        if (message.getSender().getUsername() != null
                && !message.getSender().getUsername().isBlank()) {
            return message.getSender().getUsername();
        }

        return "Người dùng";
    }

    private String buildNotificationContent(ChatMessageResponse message) {
        if (message.getMessage() != null && !message.getMessage().isBlank()) {
            String trimmedMessage = message.getMessage().trim();
            if (trimmedMessage.length() > 80) {
                trimmedMessage = trimmedMessage.substring(0, 77) + "...";
            }
            return "đã gửi cho bạn: " + trimmedMessage;
        }

        if (message.getAttachments() != null && !message.getAttachments().isEmpty()) {
            return "đã gửi cho bạn một tệp đính kèm.";
        }

        return "đã gửi cho bạn một tin nhắn.";
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
