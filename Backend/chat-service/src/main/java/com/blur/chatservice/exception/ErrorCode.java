package com.blur.chatservice.exception;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    MESSAGE_NOT_FOUND(1029, "Message not found", HttpStatus.NOT_FOUND),
    FILE_NOT_FOUND(1030, "File not found", HttpStatus.NOT_FOUND),
    USER_NOT_AUTHENTICATED(1031, "User not authenticated", HttpStatus.NOT_FOUND),
    USER_PROFILE_NOT_FOUND(1032, "User profile not found", HttpStatus.NOT_FOUND),
    CONVERSATION_NOT_FOUND(1033, "Conversation not found", HttpStatus.NOT_FOUND),
    INVALID_FILE(400, "Invalid file", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE(400, "File size exceeds limit", HttpStatus.BAD_REQUEST),
    INVALID_FILE_TYPE(400, "File type not allowed", HttpStatus.BAD_REQUEST),
    FILE_UPLOAD_FAILED(500, "File upload failed", HttpStatus.INTERNAL_SERVER_ERROR),

    TOKEN_REQUIRED(2001, "Authentication token is required", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN(2002, "Invalid or expired authentication token", HttpStatus.UNAUTHORIZED),
    AUTH_FAILED(2003, "Authentication failed", HttpStatus.UNAUTHORIZED),
    SESSION_EXPIRED(2004, "WebSocket session has expired", HttpStatus.UNAUTHORIZED),

    INVALID_DATA(3001, "Invalid request data", HttpStatus.BAD_REQUEST),
    CONVERSATION_ID_REQUIRED(3002, "Conversation ID is required", HttpStatus.BAD_REQUEST),
    EMPTY_MESSAGE(3003, "Message content or attachments required", HttpStatus.BAD_REQUEST),
    DUPLICATE_MESSAGE(3004, "Duplicate message detected", HttpStatus.CONFLICT),
    MESSAGE_SEND_FAILED(3005, "Failed to send message", HttpStatus.INTERNAL_SERVER_ERROR),
    RECEIVER_NOT_FOUND(3006, "Message receiver not found", HttpStatus.NOT_FOUND),
    INVALID_CONVERSATION(3007, "Invalid conversation configuration", HttpStatus.BAD_REQUEST),

    CALL_NOT_FOUND(4001, "Call session not found", HttpStatus.NOT_FOUND),
    CALL_INITIATE_FAILED(4002, "Failed to initiate call", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_NOT_AVAILABLE(4003, "User is not available for calls", HttpStatus.SERVICE_UNAVAILABLE),
    USER_OFFLINE(4004, "User is offline", HttpStatus.SERVICE_UNAVAILABLE),
    CALL_ALREADY_EXISTS(4005, "Call already in progress", HttpStatus.CONFLICT),
    INVALID_CALL_TYPE(4006, "Invalid call type", HttpStatus.BAD_REQUEST),
    CALL_ANSWER_FAILED(4007, "Failed to answer call", HttpStatus.INTERNAL_SERVER_ERROR),
    CALL_REJECT_FAILED(4008, "Failed to reject call", HttpStatus.INTERNAL_SERVER_ERROR),
    CALL_END_FAILED(4009, "Failed to end call", HttpStatus.INTERNAL_SERVER_ERROR),
    CALL_STATUS_UPDATE_FAILED(4010, "Failed to update call status", HttpStatus.INTERNAL_SERVER_ERROR),

    WEBRTC_OFFER_FAILED(5001, "Failed to send WebRTC offer", HttpStatus.INTERNAL_SERVER_ERROR),
    WEBRTC_ANSWER_FAILED(5002, "Failed to send WebRTC answer", HttpStatus.INTERNAL_SERVER_ERROR),
    ICE_CANDIDATE_FAILED(5003, "Failed to exchange ICE candidate", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_SDP(5004, "Invalid SDP format", HttpStatus.BAD_REQUEST),
    PEER_NOT_FOUND(5005, "Peer connection not found", HttpStatus.NOT_FOUND),

    DISCONNECT_FAILED(6001, "Failed to disconnect session", HttpStatus.INTERNAL_SERVER_ERROR),
    SESSION_CLEANUP_FAILED(6002, "Failed to cleanup session data", HttpStatus.INTERNAL_SERVER_ERROR),
    SEND_EVENT_FAILED(6003, "Failed to send event to client", HttpStatus.INTERNAL_SERVER_ERROR),

    THREAD_INTERRUPTED(7001, "Thread operation was interrupted", HttpStatus.INTERNAL_SERVER_ERROR),
    ASYNC_OPERATION_FAILED(7002, "Asynchronous operation failed", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
