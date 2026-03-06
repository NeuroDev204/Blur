package com.blur.communicationservice.chat.service;

import java.time.Instant;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blur.communicationservice.ai.service.AiConversationService;
import com.blur.communicationservice.chat.repository.ChatMessageRepository;
import com.blur.communicationservice.chat.repository.ConversationRepository;
import com.blur.communicationservice.dto.ApiResponse;
import com.blur.communicationservice.dto.request.AiChatRequest;
import com.blur.communicationservice.dto.request.ChatMessageRequest;
import com.blur.communicationservice.dto.response.AiChatResponse;
import com.blur.communicationservice.dto.response.ChatMessageResponse;
import com.blur.communicationservice.dto.response.UserProfileResponse;
import com.blur.communicationservice.entity.ChatMessage;
import com.blur.communicationservice.entity.MediaAttachment;
import com.blur.communicationservice.entity.ParticipantInfo;
import com.blur.communicationservice.enums.MessageType;
import com.blur.communicationservice.exception.AppException;
import com.blur.communicationservice.exception.ErrorCode;
import com.blur.communicationservice.repository.httpclient.ProfileClient;
import com.blur.communicationservice.service.RedisCacheService;
import com.blur.communicationservice.websocket.service.WebSocketNotificationService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatMessageService {

    ConversationRepository conversationRepository;
    ProfileClient profileClient;
    ChatMessageRepository chatMessageRepository;
    RedisCacheService redisCacheService;
    AiConversationService aiConversationService;
    WebSocketNotificationService webSocketNotificationService;

    @Transactional
    public ChatMessageResponse create(ChatMessageRequest request, String userId) {
        if (request.getConversationId() == null || request.getConversationId().isEmpty()) {
            throw new AppException(ErrorCode.CONVERSATION_NOT_FOUND);
        }

        boolean hasMessage =
                request.getMessage() != null && !request.getMessage().trim().isEmpty();
        boolean hasAttachments =
                request.getAttachments() != null && !request.getAttachments().isEmpty();

        if (!hasMessage && !hasAttachments) {
            throw new AppException(ErrorCode.EMPTY_MESSAGE);
        }

        if (request.getAttachments() != null) {
            for (MediaAttachment att : request.getAttachments()) {
                if (att.getUrl() == null || att.getUrl().isEmpty()) {
                    throw new AppException(ErrorCode.INVALID_FILE);
                }

                if (att.getFileSize() != null && att.getFileSize() > 10485760) {
                    throw new AppException(ErrorCode.FILE_TOO_LARGE);
                }
            }
        }

        var userResponse = profileClient.getProfile(userId);
        if (userResponse == null || userResponse.getResult() == null) {
            throw new AppException(ErrorCode.USER_PROFILE_NOT_FOUND);
        }

        var conversation = conversationRepository
                .findById(request.getConversationId())
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> userResponse.getResult().getUserId().equals(p.getUserId()));

        if (!isParticipant) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        var userInfo = userResponse.getResult();

        ChatMessage chatMessage = ChatMessage.builder()
                .conversationId(request.getConversationId())
                .message(request.getMessage())
                .attachments(request.getAttachments())
                .messageType(determineMessageType(request))
                .sender(ParticipantInfo.builder()
                        .userId(userInfo.getUserId())
                        .username(userInfo.getUsername())
                        .firstName(userInfo.getFirstName())
                        .lastName(userInfo.getLastName())
                        .avatar(userInfo.getImageUrl())
                        .build())
                .createdDate(Instant.now())
                .readBy(List.of(userInfo.getUserId()))
                .build();

        chatMessage = chatMessageRepository.save(chatMessage);

        // ==================== AI LOGIC WITH BROADCASTING ====================
        if (Boolean.TRUE.equals(conversation.getAiEnabled())
                && request.getMessage() != null
                && !request.getMessage().isBlank()) {

            log.info("AI enabled for conversation: {}", conversation.getId());

            try {
                // 1. Goi AI Service (LOCAL CALL - khong qua Feign)
                AiChatRequest aiReq = new AiChatRequest();
                aiReq.setConversationId(conversation.getAiConversationId());
                aiReq.setUserId(userInfo.getUserId());
                aiReq.setMessage(request.getMessage());

                AiChatResponse aiRes = aiConversationService.chat(aiReq);

                if (aiRes.isSuccess()) {
                    log.info("AI response received successfully");

                    // 2. Luu AI conversation ID lan dau
                    if (conversation.getAiConversationId() == null && aiRes.getConversationId() != null) {
                        conversation.setAiConversationId(aiRes.getConversationId());
                        conversationRepository.save(conversation);
                        log.info("Saved AI conversation ID: {}", aiRes.getConversationId());
                    }

                    // 3. Tao AI message
                    ChatMessage aiMessage = ChatMessage.builder()
                            .conversationId(request.getConversationId())
                            .message(aiRes.getResponse())
                            .attachments(null)
                            .messageType(MessageType.TEXT)
                            .sender(ParticipantInfo.builder()
                                    .userId("AI_BOT")
                                    .username("AI Assistant")
                                    .firstName("AI")
                                    .lastName("Assistant")
                                    .avatar(null)
                                    .build())
                            .createdDate(Instant.now())
                            .readBy(List.of(userInfo.getUserId()))
                            .build();

                    // 4. Luu vao database
                    ChatMessage savedAiMessage = chatMessageRepository.save(aiMessage);
                    log.info("Saved AI message to database: {}", savedAiMessage.getId());

                    // 5. BROADCAST AI MESSAGE TOI TAT CA PARTICIPANTS
                    broadcastAiMessage(savedAiMessage, conversation.getId());

                } else {
                    log.error("AI service returned error: {}", aiRes.getError());
                }

            } catch (Exception e) {
                log.error("Error calling AI service: {}", e.getMessage(), e);
            }
        }

        return toChatMessageResponse(chatMessage, userId);
    }

    private void broadcastAiMessage(ChatMessage aiMessage, String conversationId) {
        try {
            log.info("Broadcasting AI message to conversation: {}", conversationId);

            // Lay danh sach participants de gui STOMP cho tung user
            var conversation = conversationRepository.findById(conversationId).orElse(null);
            if (conversation == null) return;

            ChatMessageResponse aiResponse = toChatMessageResponse(aiMessage, null);

            for (ParticipantInfo participant : conversation.getParticipants()) {
                webSocketNotificationService.sendChatMessage(participant.getUserId(), aiResponse);
            }

            log.info("AI message broadcasted via STOMP to conversation: {}", conversationId);

        } catch (Exception e) {
            log.error("Error broadcasting AI message: {}", e.getMessage(), e);
        }
    }

    // ==================== EXISTING METHODS (khong thay doi) ====================

    public List<ChatMessageResponse> getMessages(String conversationId) {
        String userId = null;
        try {
            userId = SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        ApiResponse<UserProfileResponse> userProfileResponse = null;
        try {
            userProfileResponse = profileClient.getProfile(userId);
        } catch (Exception e) {
            throw new AppException(ErrorCode.USER_PROFILE_NOT_FOUND);
        }

        if (userProfileResponse == null || userProfileResponse.getResult() == null) {
            throw new AppException(ErrorCode.USER_PROFILE_NOT_FOUND);
        }

        final ApiResponse<UserProfileResponse> userResponse = userProfileResponse;

        var conversation = conversationRepository
                .findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> userResponse.getResult().getUserId().equals(p.getUserId()));

        if (!isParticipant) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        var messages = chatMessageRepository.findAllByConversationIdOrderByCreatedDateDesc(conversationId);

        final String finalUserId = userId;
        return messages.stream()
                .map(msg -> toChatMessageResponse(msg, finalUserId))
                .toList();
    }

    public Integer unreadCount(String conversationId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth != null ? auth.getName() : null;

        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        Integer cachedCount = redisCacheService.getUnreadCount(conversationId, userId);
        if (cachedCount != null) {
            return cachedCount;
        }

        ApiResponse<UserProfileResponse> userResponse = null;
        try {
            userResponse = profileClient.getProfile(userId);
        } catch (Exception e) {
            throw new AppException(ErrorCode.USER_PROFILE_NOT_FOUND);
        }

        if (userResponse == null || userResponse.getResult() == null) {
            throw new AppException(ErrorCode.USER_PROFILE_NOT_FOUND);
        }

        var conversation = conversationRepository
                .findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(p -> p.getUserId().equals(userId));

        if (!isParticipant) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        long count = chatMessageRepository.countByConversationIdAndReadByNotContains(conversationId, userId);
        int intCount = (int) count;

        redisCacheService.cacheUnreadCount(conversationId, userId, intCount);

        return intCount;
    }

    @Transactional
    public String markAsRead(String conversationId, String userId) {
        List<ChatMessage> messages =
                chatMessageRepository.findAllByConversationIdOrderByCreatedDateDesc(conversationId);

        for (ChatMessage msg : messages) {
            if (!msg.getReadBy().contains(userId)) {
                msg.getReadBy().add(userId);
                msg.setIsRead(true);
            }
        }

        chatMessageRepository.saveAll(messages);

        return "mark as read";
    }

    private MessageType determineMessageType(ChatMessageRequest request) {
        boolean hasMessage =
                request.getMessage() != null && !request.getMessage().trim().isEmpty();
        boolean hasAttachments =
                request.getAttachments() != null && !request.getAttachments().isEmpty();

        if (!hasAttachments) {
            return MessageType.TEXT;
        }
        if (hasMessage) {
            return MessageType.MIXED;
        }

        String fileType = request.getAttachments().get(0).getFileType();
        if (fileType == null) return MessageType.FILE;
        if (fileType.startsWith("image/")) return MessageType.IMAGE;
        if (fileType.startsWith("video/")) return MessageType.VIDEO;

        return MessageType.FILE;
    }

    private ChatMessageResponse toChatMessageResponse(ChatMessage msg, String currentUserId) {
        ChatMessageResponse response = ChatMessageResponse.builder()
                .id(msg.getId())
                .conversationId(msg.getConversationId())
                .message(msg.getMessage())
                .messageType(msg.getMessageType())
                .attachments(msg.getAttachments())
                .sender(msg.getSender())
                .createdDate(msg.getCreatedDate())
                .readBy(msg.getReadBy())
                .build();

        if (currentUserId != null && msg.getSender() != null) {
            response.setMe(currentUserId.equals(msg.getSender().getUserId()));
        }

        return response;
    }

    private String orEmpty(String value) {
        return value != null ? value : "";
    }

    public String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
