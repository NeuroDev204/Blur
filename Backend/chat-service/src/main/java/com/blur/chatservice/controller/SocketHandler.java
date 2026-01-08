package com.blur.chatservice.controller;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.springframework.stereotype.Component;

import com.blur.chatservice.dto.request.ChatMessageRequest;
import com.blur.chatservice.dto.response.ChatMessageResponse;
import com.blur.chatservice.entity.CallSession;
import com.blur.chatservice.entity.ChatMessage;
import com.blur.chatservice.entity.MediaAttachment;
import com.blur.chatservice.entity.ParticipantInfo;
import com.blur.chatservice.enums.CallStatus;
import com.blur.chatservice.enums.CallType;
import com.blur.chatservice.exception.AppException;
import com.blur.chatservice.exception.ErrorCode;
import com.blur.chatservice.repository.ConversationRepository;
import com.blur.chatservice.service.CallService;
import com.blur.chatservice.service.ChatMessageService;
import com.blur.chatservice.service.IdentityService;
import com.blur.chatservice.service.RedisCacheService;
import com.blur.chatservice.service.WebsocketSessionService;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Optimized SocketHandler with Redis Cache
 * - User socket mapping in Redis
 * - Message deduplication in Redis
 * - User sessions in Redis
 * - Better scalability and persistence
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SocketHandler {

    SocketIOServer socketIOServer;
    IdentityService identityService;
    WebsocketSessionService websocketSessionService;
    ChatMessageService chatMessageService;
    ConversationRepository conversationRepository;
    CallService callService;
    RedisCacheService redisCacheService;

    static final long DUPLICATE_THRESHOLD = 3000; // 3 seconds
    static final long SESSION_TTL = 7200; // 2 hours in seconds

    @PostConstruct
    public void startServer() {
        socketIOServer.addConnectListener(this::handleConnect);
        socketIOServer.addDisconnectListener(this::handleDisconnect);

        socketIOServer.addEventListener("send_message", Map.class, this::handleSendMessage);
        socketIOServer.addEventListener("typing", Map.class, this::handleTyping);

        socketIOServer.addEventListener("call:initiate", Map.class, this::onCallInitiate);
        socketIOServer.addEventListener("call:answer", Map.class, this::onCallAnswer);
        socketIOServer.addEventListener("call:reject", Map.class, this::onCallReject);
        socketIOServer.addEventListener("call:end", Map.class, this::onCallEnd);

        socketIOServer.addEventListener("webrtc:offer", Map.class, this::onWebRTCOffer);
        socketIOServer.addEventListener("webrtc:answer", Map.class, this::onWebRTCAnswer);
        socketIOServer.addEventListener("webrtc:ice-candidate", Map.class, this::onICECandidate);

        socketIOServer.start();
    }

    @PreDestroy
    public void stopServer() {
        socketIOServer.stop();
    }

    /**
     * Handle client connection
     * Store mapping in Redis for scalability
     */
    private void handleConnect(SocketIOClient client) {
        try {
            String token = extractToken(client);

            if (token == null || token.isEmpty()) {
                throw new AppException(ErrorCode.TOKEN_REQUIRED);
            }

            var introspectRes = identityService.introspect(com.blur.chatservice.dto.request.IntrospectRequest.builder()
                    .token(token)
                    .build());

            if (!introspectRes.isValid()) {
                throw new AppException(ErrorCode.INVALID_TOKEN);
            }

            String userId = introspectRes.getUserId();
            String sessionId = client.getSessionId().toString();

            client.set("userId", userId);

            // Store in Redis instead of in-memory map
            redisCacheService.cacheUserSocket(userId, sessionId);
            redisCacheService.addUserSession(userId, sessionId);

            // Create websocket session
            websocketSessionService.createSession(sessionId, userId);

            Map<String, Object> connectedData = Map.of(
                    "userId", userId,
                    "sessionId", sessionId,
                    "timestamp", Instant.now().toString());

            client.sendEvent("connected", connectedData);

        } catch (AppException e) {
            sendError(client, "auth_error", e.getErrorCode());
            client.disconnect();
        } catch (Exception e) {
            sendError(client, "auth_error", ErrorCode.AUTH_FAILED);
            client.disconnect();
        }
    }

    /**
     * Handle client disconnection
     * Clean up Redis cache
     */
    private void handleDisconnect(SocketIOClient client) {
        String userId = client.get("userId");
        String sessionId = client.getSessionId().toString();

        try {
            if (userId != null) {
                redisCacheService.removeUserSocket(userId);
                redisCacheService.removeUserSession(userId, sessionId);
            }

            websocketSessionService.deleteSession(sessionId);

        } catch (Exception e) {
            throw new AppException(ErrorCode.DISCONNECT_FAILED);
        }
    }

    /**
     * Handle send message event
     * Use Redis for message deduplication
     */
    private void handleSendMessage(SocketIOClient client, Map<String, Object> data, Object ack) {
        try {
            String senderId = client.get("userId");
            if (senderId == null) {
                throw new AppException(ErrorCode.SESSION_EXPIRED);
            }

            String conversationId = (String) data.get("conversationId");
            String message = (String) data.get("message");
            String tempMessageId = (String) data.get("messageId");

            List<Map<String, Object>> attachmentsData = (List<Map<String, Object>>) data.get("attachments");

            if (conversationId == null || conversationId.isEmpty()) {
                throw new AppException(ErrorCode.CONVERSATION_ID_REQUIRED);
            }

            boolean hasMessage = message != null && !message.trim().isEmpty();
            boolean hasAttachments = attachmentsData != null && !attachmentsData.isEmpty();

            if (!hasMessage && !hasAttachments) {
                throw new AppException(ErrorCode.EMPTY_MESSAGE);
            }

            // Check duplicate in Redis
            String messageKey = conversationId + ":" + tempMessageId;
            if (isDuplicateMessage(messageKey)) {
                throw new AppException(ErrorCode.DUPLICATE_MESSAGE);
            }

            ChatMessageRequest.ChatMessageRequestBuilder builder =
                    ChatMessageRequest.builder().conversationId(conversationId).message(message);

            if (hasAttachments) {
                List<MediaAttachment> attachments =
                        attachmentsData.stream().map(this::mapToAttachment).collect(Collectors.toList());
                builder.attachments(attachments);
            }

            ChatMessageRequest request = builder.build();
            ChatMessageResponse savedMessage = chatMessageService.create(request, senderId);

            var conversation = conversationRepository
                    .findById(conversationId)
                    .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

            if (conversation.getParticipants().size() != 2) {
                throw new AppException(ErrorCode.INVALID_CONVERSATION);
            }

            String receiverId = conversation.getParticipants().stream()
                    .map(ParticipantInfo::getUserId)
                    .filter(id -> !id.equals(senderId))
                    .findFirst()
                    .orElseThrow(() -> new AppException(ErrorCode.RECEIVER_NOT_FOUND));

            Map<String, Object> payload = buildMessagePayload(savedMessage, tempMessageId);

            sendToUser(senderId, "message_sent", payload);
            sendToUser(receiverId, "message_received", payload);

            // Mark as processed in Redis
            markMessageAsProcessed(messageKey);

        } catch (AppException e) {
            sendError(client, "message_error", e.getErrorCode());
        } catch (Exception e) {
            sendError(client, "message_error", ErrorCode.MESSAGE_SEND_FAILED);
        }
    }

    /**
     * Handle typing indicator
     * Use Redis to check user online status
     */
    private void handleTyping(SocketIOClient client, Map<String, Object> data, Object ack) {
        try {
            String senderId = client.get("userId");
            String conversationId = (String) data.get("conversationId");
            Boolean isTyping = (Boolean) data.getOrDefault("isTyping", false);

            if (senderId == null) {
                throw new AppException(ErrorCode.SESSION_EXPIRED);
            }
            if (conversationId == null) {
                throw new AppException(ErrorCode.CONVERSATION_ID_REQUIRED);
            }

            var conversation = conversationRepository
                    .findById(conversationId)
                    .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

            String receiverId = conversation.getParticipants().stream()
                    .map(ParticipantInfo::getUserId)
                    .filter(id -> !id.equals(senderId))
                    .findFirst()
                    .orElseThrow(() -> new AppException(ErrorCode.RECEIVER_NOT_FOUND));

            Map<String, Object> typingData = Map.of(
                    "conversationId", conversationId,
                    "userId", senderId,
                    "isTyping", isTyping);

            sendToUser(receiverId, "user_typing", typingData);

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorCode.SEND_EVENT_FAILED);
        }
    }

    /**
     * Initiate call
     * Use Redis for user availability check
     */
    public void onCallInitiate(SocketIOClient client, Map<String, Object> data, Object ack) {
        String sessionId = null;
        try {
            String callerId = (String) data.get("callerId");
            String callerName = (String) data.get("callerName");
            String callerAvatar = (String) data.get("callerAvatar");
            String receiverId = (String) data.get("receiverId");
            String receiverName = (String) data.get("receiverName");
            String receiverAvatar = (String) data.get("receiverAvatar");
            String callTypeStr = (String) data.get("callType");
            String conversationId = (String) data.get("conversationId");

            if (callerId == null || receiverId == null || callTypeStr == null) {
                throw new AppException(ErrorCode.INVALID_DATA);
            }

            CallType callType;
            try {
                callType = CallType.valueOf(callTypeStr);
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.INVALID_CALL_TYPE);
            }

            CallSession session = callService.initiateCall(
                    callerId,
                    callerName,
                    callerAvatar,
                    receiverId,
                    receiverName,
                    receiverAvatar,
                    callType,
                    client.getSessionId().toString(),
                    conversationId);

            sessionId = session.getId();

            // Send to caller
            client.sendEvent("call:initiated", Map.of("success", true, "callId", session.getId()));

            // Get receiver socket from Redis
            String receiverSocketId = redisCacheService.getUserSocket(receiverId);

            if (receiverSocketId == null) {
                throw new AppException(ErrorCode.USER_NOT_AVAILABLE);
            }

            UUID receiverUUID;
            try {
                receiverUUID = UUID.fromString(receiverSocketId);
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.CALL_INITIATE_FAILED);
            }

            SocketIOClient receiverClient = socketIOServer.getClient(receiverUUID);

            if (receiverClient == null || !receiverClient.isChannelOpen()) {
                throw new AppException(ErrorCode.USER_OFFLINE);
            }

            receiverClient.sendEvent(
                    "call:incoming",
                    Map.of(
                            "callId",
                            session.getId(),
                            "callerId",
                            callerId,
                            "callerName",
                            callerName,
                            "callerAvatar",
                            callerAvatar != null ? callerAvatar : "",
                            "callType",
                            callType.name()));

            callService.updateCallStatus(session.getId(), CallStatus.RINGING, receiverSocketId);

        } catch (AppException e) {
            if (sessionId != null) {
                try {
                    CallStatus failedStatus = (e.getErrorCode() == ErrorCode.USER_NOT_AVAILABLE
                                    || e.getErrorCode() == ErrorCode.USER_OFFLINE)
                            ? CallStatus.MISSED
                            : CallStatus.FAILED;
                    callService.updateCallStatus(sessionId, failedStatus, null);
                } catch (Exception ignored) {
                    throw new AppException(ErrorCode.CALL_STATUS_UPDATE_FAILED);
                }
            }

            client.sendEvent(
                    "call:failed",
                    Map.of(
                            "code", e.getErrorCode().getCode(),
                            "reason", e.getErrorCode().getMessage()));
        } catch (Exception e) {
            if (sessionId != null) {
                try {
                    callService.updateCallStatus(sessionId, CallStatus.FAILED, null);
                } catch (Exception ignored) {
                    throw new AppException(ErrorCode.CALL_STATUS_UPDATE_FAILED);
                }
            }

            client.sendEvent(
                    "call:failed",
                    Map.of(
                            "code", ErrorCode.CALL_INITIATE_FAILED.getCode(),
                            "reason", ErrorCode.CALL_INITIATE_FAILED.getMessage()));
        }
    }

    public void onCallAnswer(SocketIOClient client, Map<String, Object> data, Object ack) {
        try {
            String callId = (String) data.get("callId");

            if (callId == null || callId.isEmpty()) {
                throw new AppException(ErrorCode.INVALID_DATA);
            }

            CallSession session = callService.updateCallStatus(
                    callId, CallStatus.ANSWERED, client.getSessionId().toString());

            if (session == null) {
                throw new AppException(ErrorCode.CALL_NOT_FOUND);
            }

            client.sendEvent("call:answer:success", Map.of("callId", session.getId()));

            new Thread(() -> {
                        try {
                            Thread.sleep(800);

                            String callerId = session.getCallerId();
                            String receiverId = session.getReceiverId();

                            sendToUser(
                                    callerId,
                                    "call:answered",
                                    Map.of(
                                            "callId", callId,
                                            "receiverId", receiverId));

                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new AppException(ErrorCode.THREAD_INTERRUPTED);
                        }
                    })
                    .start();

        } catch (AppException e) {
            client.sendEvent(
                    "call:failed",
                    Map.of(
                            "code", e.getErrorCode().getCode(),
                            "reason", e.getErrorCode().getMessage()));
        } catch (Exception e) {
            client.sendEvent(
                    "call:failed",
                    Map.of(
                            "code", ErrorCode.CALL_ANSWER_FAILED.getCode(),
                            "reason", ErrorCode.CALL_ANSWER_FAILED.getMessage()));
        }
    }

    public void onCallReject(SocketIOClient client, Map<String, Object> data, Object ack) {
        try {
            String callId = (String) data.get("callId");
            if (callId == null || callId.isEmpty()) {
                throw new AppException(ErrorCode.INVALID_DATA);
            }

            CallSession session = callService.updateCallStatus(callId, CallStatus.REJECTED, null);

            if (session == null) {
                throw new AppException(ErrorCode.CALL_NOT_FOUND);
            }

            client.sendEvent("call:reject:success", Map.of("callId", callId));

            sendToUser(session.getCallerId(), "call:rejected", Map.of("callId", callId, "reason", "Call was rejected"));

        } catch (AppException e) {
            client.sendEvent(
                    "call:failed",
                    Map.of(
                            "code", e.getErrorCode().getCode(),
                            "reason", e.getErrorCode().getMessage()));
        } catch (Exception e) {
            client.sendEvent(
                    "call:failed",
                    Map.of(
                            "code", ErrorCode.CALL_REJECT_FAILED.getCode(),
                            "reason", ErrorCode.CALL_REJECT_FAILED.getMessage()));
        }
    }

    public void onCallEnd(SocketIOClient client, Map<String, Object> data, Object ack) {
        try {
            String callId = (String) data.get("callId");
            String userId = client.get("userId");

            if (callId == null || callId.isEmpty()) {
                throw new AppException(ErrorCode.INVALID_DATA);
            }

            CallSession session = callService.endCall(callId, userId);

            if (session == null) {
                throw new AppException(ErrorCode.CALL_NOT_FOUND);
            }

            Map<String, Object> endData =
                    Map.of("callId", callId, "duration", session.getDuration() != null ? session.getDuration() : 0);

            sendToUser(session.getCallerId(), "call:ended", endData);
            sendToUser(session.getReceiverId(), "call:ended", endData);

            // ✅ GET THE CREATED CALL MESSAGE AND BROADCAST IT (Only broadcast once)
            ChatMessage callMessage = callService.getAndClearLastCreatedCallMessage();
            if (callMessage != null) {
                Map<String, Object> messagePayload = buildCallMessagePayload(callMessage);

                // Broadcast to both participants using room-based approach
                socketIOServer
                        .getRoomOperations("conversation:" + session.getConversationId())
                        .sendEvent("message_received", messagePayload);
            }

        } catch (AppException e) {
            client.sendEvent(
                    "call:failed",
                    Map.of(
                            "code", e.getErrorCode().getCode(),
                            "reason", e.getErrorCode().getMessage()));
        } catch (Exception e) {
            client.sendEvent(
                    "call:failed",
                    Map.of(
                            "code", ErrorCode.CALL_END_FAILED.getCode(),
                            "reason", ErrorCode.CALL_END_FAILED.getMessage()));
        }
    }

    private void onWebRTCOffer(SocketIOClient client, Map<String, Object> data, AckRequest ack) {
        try {
            String to = (String) data.get("to");
            String from = client.get("userId");

            if (to == null || to.isEmpty()) {
                throw new AppException(ErrorCode.INVALID_DATA);
            }

            Map<String, Object> forwardData = new HashMap<>(data);
            forwardData.put("from", from);

            int sent = sendToUser(to, "webrtc:offer", forwardData);

            if (sent == 0) {
                throw new AppException(ErrorCode.PEER_NOT_FOUND);
            }

        } catch (AppException e) {
            client.sendEvent(
                    "webrtc:error",
                    Map.of(
                            "code", e.getErrorCode().getCode(),
                            "message", e.getErrorCode().getMessage()));
        } catch (Exception e) {
            client.sendEvent(
                    "webrtc:error",
                    Map.of(
                            "code", ErrorCode.WEBRTC_OFFER_FAILED.getCode(),
                            "message", ErrorCode.WEBRTC_OFFER_FAILED.getMessage()));
        }
    }

    private void onWebRTCAnswer(SocketIOClient client, Map<String, Object> data, AckRequest ack) {
        try {
            String to = (String) data.get("to");
            String from = client.get("userId");

            if (to == null || to.isEmpty()) {
                throw new AppException(ErrorCode.INVALID_DATA);
            }

            Map<String, Object> forwardData = new HashMap<>(data);
            forwardData.put("from", from);

            int sent = sendToUser(to, "webrtc:answer", forwardData);

            if (sent == 0) {
                throw new AppException(ErrorCode.PEER_NOT_FOUND);
            }

        } catch (AppException e) {
            client.sendEvent(
                    "webrtc:error",
                    Map.of(
                            "code", e.getErrorCode().getCode(),
                            "message", e.getErrorCode().getMessage()));
        } catch (Exception e) {
            client.sendEvent(
                    "webrtc:error",
                    Map.of(
                            "code", ErrorCode.WEBRTC_ANSWER_FAILED.getCode(),
                            "message", ErrorCode.WEBRTC_ANSWER_FAILED.getMessage()));
        }
    }

    private void onICECandidate(SocketIOClient client, Map<String, Object> data, AckRequest ack) {
        try {
            String to = (String) data.get("to");
            String from = client.get("userId");

            if (to == null || to.isEmpty()) {
                throw new AppException(ErrorCode.INVALID_DATA);
            }

            Map<String, Object> forwardData = new HashMap<>(data);
            forwardData.put("from", from);

            int sent = sendToUser(to, "webrtc:ice-candidate", forwardData);

            if (sent == 0) {
                throw new AppException(ErrorCode.PEER_NOT_FOUND);
            }

        } catch (AppException e) {
            client.sendEvent(
                    "webrtc:error",
                    Map.of(
                            "code", e.getErrorCode().getCode(),
                            "message", e.getErrorCode().getMessage()));
        } catch (Exception e) {
            client.sendEvent(
                    "webrtc:error",
                    Map.of(
                            "code", ErrorCode.ICE_CANDIDATE_FAILED.getCode(),
                            "message", ErrorCode.ICE_CANDIDATE_FAILED.getMessage()));
        }
    }

    // ==================== HELPER METHODS ====================

    private MediaAttachment mapToAttachment(Map<String, Object> data) {
        if (data == null) return null;

        return MediaAttachment.builder()
                .id((String) data.get("id"))
                .url((String) data.get("url"))
                .fileName((String) data.get("fileName"))
                .fileType((String) data.get("fileType"))
                .fileSize(getNumber(data, "fileSize", Long.class))
                .width(getNumber(data, "width", Integer.class))
                .height(getNumber(data, "height", Integer.class))
                .duration(getNumber(data, "duration", Integer.class))
                .thumbnailUrl((String) data.get("thumbnailUrl"))
                .build();
    }

    @SuppressWarnings("unchecked")
    private <T extends Number> T getNumber(Map<String, Object> data, String key, Class<T> type) {
        Object value = data.get(key);
        if (value == null) return null;

        if (type == Long.class) {
            return (T) Long.valueOf(((Number) value).longValue());
        } else if (type == Integer.class) {
            return (T) Integer.valueOf(((Number) value).intValue());
        }
        return null;
    }

    private Map<String, Object> buildMessagePayload(ChatMessageResponse msg, String tempId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", msg.getId());
        payload.put("messageId", msg.getId());
        payload.put("tempMessageId", tempId);
        payload.put("conversationId", msg.getConversationId());
        payload.put("message", msg.getMessage());
        payload.put(
                "messageType",
                msg.getMessageType() != null ? msg.getMessageType().toString() : "TEXT");
        payload.put("createdDate", msg.getCreatedDate().toString());

        if (msg.getSender() != null) {
            ParticipantInfo sender = msg.getSender();
            payload.put("senderId", sender.getUserId());

            Map<String, Object> senderMap = Map.of(
                    "userId", orEmpty(sender.getUserId()),
                    "username", orEmpty(sender.getUsername()),
                    "firstName", orEmpty(sender.getFirstName()),
                    "lastName", orEmpty(sender.getLastName()),
                    "avatar", orEmpty(sender.getAvatar()));
            payload.put("sender", senderMap);
        }

        if (msg.getAttachments() != null && !msg.getAttachments().isEmpty()) {
            payload.put("attachments", msg.getAttachments());
        }

        return payload;
    }

    /**
     * ✅ Build message payload for ChatMessage (used for call messages)
     */
    private Map<String, Object> buildCallMessagePayload(ChatMessage msg) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", msg.getId());
        payload.put("messageId", msg.getId());
        payload.put("conversationId", msg.getConversationId());
        payload.put("message", msg.getMessage());
        payload.put(
                "messageType",
                msg.getMessageType() != null ? msg.getMessageType().toString() : "TEXT");
        payload.put(
                "createdDate",
                msg.getCreatedDate() != null ? msg.getCreatedDate().toString() : System.currentTimeMillis());

        if (msg.getSender() != null) {
            ParticipantInfo sender = msg.getSender();
            payload.put("senderId", sender.getUserId());

            Map<String, Object> senderMap = Map.of(
                    "userId", orEmpty(sender.getUserId()),
                    "username", orEmpty(sender.getUsername()),
                    "firstName", orEmpty(sender.getFirstName()),
                    "lastName", orEmpty(sender.getLastName()),
                    "avatar", orEmpty(sender.getAvatar()));
            payload.put("sender", senderMap);
        }

        payload.put("isRead", msg.getIsRead() != null ? msg.getIsRead() : false);

        if (msg.getAttachments() != null && !msg.getAttachments().isEmpty()) {
            payload.put("attachments", msg.getAttachments());
        }

        return payload;
    }

    /**
     * Send event to user's all active sessions
     * Use Redis to get user sessions
     */
    private int sendToUser(String userId, String event, Object data) {
        Set<Object> sessions = redisCacheService.getUserSessions(userId);
        if (sessions == null || sessions.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (Object sessionObj : sessions) {
            try {
                UUID sessionId = UUID.fromString(sessionObj.toString());
                SocketIOClient client = socketIOServer.getClient(sessionId);
                if (client != null && client.isChannelOpen()) {
                    client.sendEvent(event, data);
                    count++;
                }
            } catch (Exception e) {
                // Skip invalid session
            }
        }
        return count;
    }

    private String extractToken(SocketIOClient client) {
        String token = client.getHandshakeData().getSingleUrlParam("token");

        if (token == null || token.isEmpty()) {
            Object authObj = client.getHandshakeData().getAuthToken();
            if (authObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> authMap = (Map<String, Object>) authObj;
                token = (String) authMap.get("token");
            }
        }

        return token;
    }

    /**
     * Check duplicate message using Redis
     */
    private boolean isDuplicateMessage(String key) {
        return redisCacheService.isMessageProcessed(key);
    }

    /**
     * Mark message as processed in Redis
     */
    private void markMessageAsProcessed(String key) {
        redisCacheService.markMessageAsProcessed(key, DUPLICATE_THRESHOLD);
    }

    private void sendError(SocketIOClient client, String event, ErrorCode errorCode) {
        Map<String, Object> error = Map.of(
                "code", errorCode.getCode(),
                "message", errorCode.getMessage(),
                "timestamp", Instant.now().toString());
        client.sendEvent(event, error);
    }

    private String orEmpty(String value) {
        return value != null ? value : "";
    }
}
