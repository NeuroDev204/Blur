package com.blur.communicationservice.chat.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blur.communicationservice.chat.repository.CallSessionRepository;
import com.blur.communicationservice.chat.repository.ChatMessageRepository;
import com.blur.communicationservice.chat.repository.ConversationRepository;
import com.blur.communicationservice.dto.response.ChatMessageResponse;
import com.blur.communicationservice.entity.CallSession;
import com.blur.communicationservice.entity.ChatMessage;
import com.blur.communicationservice.entity.Conversation;
import com.blur.communicationservice.entity.ParticipantInfo;
import com.blur.communicationservice.enums.CallStatus;
import com.blur.communicationservice.enums.CallType;
import com.blur.communicationservice.enums.MessageType;
import com.blur.communicationservice.notification.service.NotificationService;
import com.blur.communicationservice.service.RedisCacheService;
import com.blur.communicationservice.websocket.service.WebSocketNotificationService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CallService {

    private static final int RING_TIMEOUT = 60;
    private static final int ACTIVATE_CALL_TIMEOUT = 3600;
    // ✅ Temporary storage for last created call message to broadcast
    private static final ThreadLocal<ChatMessage> lastCreatedCallMessage = new ThreadLocal<>();
    CallSessionRepository callSessionRepository;
    ChatMessageRepository chatMessageRepository; // ✅ ADD THIS
    ConversationRepository conversationRepository;
    RedisCacheService redisCacheService;
    NotificationService notificationService;
    CacheManager cacheManager;
    WebSocketNotificationService webSocketNotificationService;

    @Transactional
    public CallSession initiateCall(
            String callerId,
            String callerName,
            String callerAvatar,
            String receiverId,
            String receiverName,
            String receiverAvatar,
            CallType callType,
            String callerSocketId,
            String conversationId) {
        // 1. Check if users are in call
        if (redisCacheService.isUserInCall(receiverId)) {
            throw new IllegalStateException("User is currently in another call");
        }

        if (redisCacheService.isUserInCall(callerId)) {
            throw new IllegalStateException("You are currently in another call");
        }

        // 2. Create call session
        CallSession session = CallSession.builder()
                .id(UUID.randomUUID().toString())
                .callerId(callerId)
                .callerName(callerName)
                .callerAvatar(callerAvatar)
                .receiverId(receiverId)
                .receiverName(receiverName)
                .receiverAvatar(receiverAvatar)
                .callType(callType)
                .callStatus(CallStatus.INITIATING)
                .createdAt(LocalDateTime.now())
                .callerSockerId(callerSocketId)
                .conversationId(conversationId)
                .build();

        // 3. Save to MongoDB
        CallSession saved = callSessionRepository.save(session);

        // 4. Cache to Redis
        redisCacheService.cacheCallSession(saved.getId(), saved, RING_TIMEOUT);
        redisCacheService.markUserInCall(callerId, saved.getId(), RING_TIMEOUT);
        redisCacheService.markUserInCall(receiverId, saved.getId(), RING_TIMEOUT);

        return saved;
    }

    /**
     * ✅ UPDATE: Save call message when call ends
     */
    @Transactional
    @CacheEvict(value = "callSessions", key = "#callId")
    public CallSession updateCallStatus(String callId, CallStatus newStatus, String receiverSocketId) {
        CallSession session = redisCacheService.getCallSession(callId, CallSession.class);
        if (session == null) {
            session = callSessionRepository
                    .findById(callId)
                    .orElseThrow(() -> new IllegalStateException("CallSession not found"));
        }

        CallStatus oldStatus = session.getCallStatus();
        session.setCallStatus(newStatus);

        if (receiverSocketId != null) {
            session.setReceiverSockerId(receiverSocketId);
        }

        // Handle status transitions
        switch (newStatus) {
            case RINGING:
                redisCacheService.cacheCallSession(session.getId(), session, RING_TIMEOUT);
                webSocketNotificationService.sendCallEvent(session.getReceiverId(), session.getCallType());
                break;

            case ANSWERED:
                session.setStartTime(LocalDateTime.now());
                redisCacheService.cacheCallSession(session.getId(), session, ACTIVATE_CALL_TIMEOUT);
                redisCacheService.markUserInCall(session.getCallerId(), session.getId(), ACTIVATE_CALL_TIMEOUT);
                redisCacheService.markUserInCall(session.getReceiverId(), session.getId(), ACTIVATE_CALL_TIMEOUT);
                break;

            case ENDED:
            case REJECTED:
            case MISSED:
            case FAILED:
            case BUSY:
                session.setEndTime(LocalDateTime.now());
                session.setEndReason(newStatus.name());

                if (session.getStartTime() != null) {
                    Duration duration = Duration.between(session.getStartTime(), LocalDateTime.now());
                    session.setDuration(duration.toSeconds());
                }

                // ✅ CREATE CALL MESSAGE (Only once, prevent duplicates)
                if (session.getConversationId() != null && oldStatus != newStatus) {
                    createCallMessage(session);
                }

                // Clean Redis
                redisCacheService.cleanupCallCaches(callId, session.getCallerId(), session.getReceiverId());

                // Increment missed calls
                if (newStatus == CallStatus.MISSED) {
                    redisCacheService.incrementMissedCalls(session.getReceiverId());
                    webSocketNotificationService.sendCallEvent(session.getReceiverId(), session.getCallType());
                }
                break;
        }

        // Save to DB
        return callSessionRepository.save(session);
    }

    /**
     * ✅ NEW METHOD: Create call message in chat history
     */
    private void createCallMessage(CallSession session) {
        try {
            String messageContent = formatCallMessage(session);

            ChatMessage callMessage = ChatMessage.builder()
                    .conversationId(session.getConversationId())
                    .message(messageContent)
                    .messageType(
                            session.getCallType() == CallType.VOICE ? MessageType.VOICE_CALL : MessageType.VIDEO_CALL)
                    .sender(ParticipantInfo.builder()
                            .userId(session.getCallerId())
                            .firstName(session.getCallerName())
                            .avatar(session.getCallerAvatar())
                            .build())
                    .createdDate(Instant.now())
                    .isRead(false)
                    .readBy(List.of(session.getCallerId()))
                    .attachments(null)
                    .build();

            ChatMessage savedMessage = chatMessageRepository.save(callMessage);

            // ✅ STORE IN THREADLOCAL to retrieve in SocketHandler for broadcasting
            lastCreatedCallMessage.set(savedMessage);

            Conversation conversation =
                    conversationRepository.findById(session.getConversationId()).orElse(null);
            if (conversation != null) {
                touchConversation(conversation, savedMessage);
                evictUnreadCachesForParticipants(conversation, session.getCallerId());
                broadcastCallMessage(savedMessage, conversation);
            }

            // ✅ EVICT CACHES to refresh conversation list
            redisCacheService.evictLastMessage(session.getConversationId());
            redisCacheService.invalidateConversationMessages(session.getConversationId());

        } catch (Exception e) {
            // Silent fail - don't break the call flow
        }
    }

    /**
     * ✅ Format call message based on status and duration
     */
    private String formatCallMessage(CallSession session) {
        String callType = session.getCallType() == CallType.VOICE ? "📞 Cuộc gọi thoại" : "📹 Cuộc gọi video";

        switch (session.getCallStatus()) {
            case ENDED:
                if (session.getDuration() != null && session.getDuration() > 0) {
                    return String.format("%s • %s", callType, formatDuration(session.getDuration()));
                }
                return String.format("%s • Đã kết thúc", callType);

            case MISSED:
                return String.format("%s • Nhớ cuộc gọi", callType);

            case REJECTED:
                return String.format("%s • Đã từ chối", callType);

            case FAILED:
            case BUSY:
                return String.format("%s • Không thành công", callType);

            default:
                return callType;
        }
    }

    /**
     * Format call duration (e.g., "2:35" for 2 minutes 35 seconds)
     */
    private String formatDuration(Long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }

        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;

        if (minutes < 60) {
            return String.format("%d:%02d", minutes, remainingSeconds);
        }

        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        return String.format("%d:%02d:%02d", hours, remainingMinutes, remainingSeconds);
    }

    public CallSession endCall(String callId, String userId) {
        return updateCallStatus(callId, CallStatus.ENDED, null);
    }

    public CallSession rejectCall(String callId, String userId) {
        return updateCallStatus(callId, CallStatus.REJECTED, null);
    }

    /**
     * ✅ Get and clear the last created call message for broadcasting
     */
    public ChatMessage getAndClearLastCreatedCallMessage() {
        ChatMessage message = lastCreatedCallMessage.get();
        lastCreatedCallMessage.remove();
        return message;
    }

    private void touchConversation(Conversation conversation, ChatMessage chatMessage) {
        conversation.setModifiedDate(chatMessage.getCreatedDate());
        conversation.setLastMessage(chatMessage.getMessage());
        conversation.setLastMessageTime(chatMessage.getCreatedDate());
        conversation.setLastMessageSender(buildSenderName(chatMessage.getSender()));
        conversationRepository.save(conversation);
    }

    private void evictUnreadCachesForParticipants(Conversation conversation, String senderId) {
        if (conversation.getParticipants() == null) {
            return;
        }

        for (ParticipantInfo participant : conversation.getParticipants()) {
            if (participant.getUserId() == null || participant.getUserId().equals(senderId)) {
                continue;
            }
            redisCacheService.evictUnreadCount(conversation.getId(), participant.getUserId());
        }
    }

    private void broadcastCallMessage(ChatMessage chatMessage, Conversation conversation) {
        if (conversation.getParticipants() == null) {
            return;
        }

        for (ParticipantInfo participant : conversation.getParticipants()) {
            if (participant.getUserId() == null) {
                continue;
            }
            webSocketNotificationService.sendChatMessage(
                    participant.getUserId(), toChatMessageResponse(chatMessage, participant.getUserId()));
        }
    }

    private ChatMessageResponse toChatMessageResponse(ChatMessage message, String currentUserId) {
        ChatMessageResponse response = ChatMessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .message(message.getMessage())
                .messageType(message.getMessageType())
                .attachments(message.getAttachments())
                .sender(message.getSender())
                .createdDate(message.getCreatedDate())
                .isRead(Boolean.TRUE.equals(message.getIsRead()))
                .readBy(message.getReadBy())
                .build();

        if (currentUserId != null && message.getSender() != null) {
            response.setMe(currentUserId.equals(message.getSender().getUserId()));
        }

        return response;
    }

    private String buildSenderName(ParticipantInfo sender) {
        if (sender == null) {
            return "";
        }

        String fullName = (orEmpty(sender.getFirstName()) + " " + orEmpty(sender.getLastName())).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        return orEmpty(sender.getUsername());
    }

    private String orEmpty(String value) {
        return value != null ? value : "";
    }

    public boolean isUserInCall(String userId) {
        return redisCacheService.isUserInCall(userId);
    }

    public Optional<CallSession> getUserCurrentCall(String userId) {
        String callId = redisCacheService.getUserCurrentCallId(userId);
        if (callId == null) {
            return Optional.empty();
        }
        return getCallSessionById(callId);
    }

    public String getUserCurrentCallId(String userId) {
        return redisCacheService.getUserCurrentCallId(userId);
    }

    @Cacheable(value = "callSessions", key = "#callId", unless = "#result == null")
    public Optional<CallSession> getCallSessionById(String callId) {
        CallSession cached = redisCacheService.getCallSession(callId, CallSession.class);
        if (cached != null) {
            return Optional.of(cached);
        }
        return callSessionRepository.findById(callId);
    }

    @Cacheable(value = "callHistory", key = "#userId + '_' + #page + '_' + #size")
    public Page<CallSession> getCallHistory(String userId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return callSessionRepository.findByCallerIdOrReceiverId(userId, pageRequest);
    }

    public List<CallSession> getMissedCalls(String userId) {
        return callSessionRepository.findMissedCallsByReceiverId(userId);
    }

    public long countMissedCalls(String userId) {
        long cachedCount = redisCacheService.getMissedCallCount(userId);
        if (cachedCount > 0) {
            return cachedCount;
        }
        long dbCount = callSessionRepository.countMissedCallsByReceiverId(userId);
        if (dbCount > 0) {
            for (int i = 0; i < dbCount; i++) {
                redisCacheService.incrementMissedCalls(userId);
            }
        }
        return dbCount;
    }

    @CacheEvict(value = "callHistory", key = "#userId + '_*'", allEntries = true)
    public void markMissedCallsAsRead(String userId) {
        redisCacheService.resetMissedCalls(userId);
    }
}
