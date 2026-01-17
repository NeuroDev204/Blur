package com.blur.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
  // ============= COMMON ERRORS (1000-1999) =============
  UNCATEGORIZED_EXCEPTION(1001, "Uncategorized Error", HttpStatus.INTERNAL_SERVER_ERROR),
  INVALID_KEY(1002, "Invalid message key", HttpStatus.BAD_REQUEST),
  UNAUTHENTICATED(1003, "Unauthenticated", HttpStatus.UNAUTHORIZED),
  UNAUTHORIZED(1004, "You do not have permission", HttpStatus.FORBIDDEN),
  INVALID_TOKEN(1005, "Invalid token", HttpStatus.UNAUTHORIZED),
  TOKEN_EXPIRED(1006, "Token has expired", HttpStatus.UNAUTHORIZED),
  ACCESS_DENIED(1007, "Access denied", HttpStatus.FORBIDDEN),

  // ============= USER ERRORS (2000-2999) =============
  USER_EXISTED(2001, "User already exists", HttpStatus.BAD_REQUEST),
  USER_NOT_EXISTED(2002, "User does not exist", HttpStatus.NOT_FOUND),
  INVALID_USERNAME_OR_PASSWORD(2003, "Invalid username or password", HttpStatus.BAD_REQUEST),
  USERNAME_INVALID(2004, "Username must be at least 3 characters", HttpStatus.BAD_REQUEST),
  PASSWORD_INVALID(2005, "Password must be at least 8 characters", HttpStatus.BAD_REQUEST),
  EMAIL_INVALID(2006, "Invalid email format", HttpStatus.BAD_REQUEST),
  PASSWORD_EXISTED(2007, "Password existed", HttpStatus.BAD_REQUEST),
  // ============= CHAT ERRORS (3000-3999) =============
  CONVERSATION_NOT_FOUND(3001, "Conversation not found", HttpStatus.NOT_FOUND),
  MESSAGE_NOT_FOUND(3002, "Message not found", HttpStatus.NOT_FOUND),
  EMPTY_MESSAGE(3003, "Message cannot be empty", HttpStatus.BAD_REQUEST),
  DUPLICATE_MESSAGE(3004, "Duplicate message detected", HttpStatus.BAD_REQUEST),
  CONVERSATION_ID_REQUIRED(3005, "Conversation ID is required", HttpStatus.BAD_REQUEST),
  RECEIVER_NOT_FOUND(3006, "Receiver not found", HttpStatus.NOT_FOUND),
  INVALID_CONVERSATION(3007, "Invalid conversation", HttpStatus.BAD_REQUEST),
  SESSION_EXPIRED(3008, "Session expired", HttpStatus.UNAUTHORIZED),
  INVALID_FILE(3009, "Invalid file ", HttpStatus.BAD_REQUEST),
  FILE_TOO_LARGE(3010, "File to large", HttpStatus.BAD_REQUEST),
  // ============= CALL ERRORS (3500-3599) =============
  CALL_NOT_FOUND(3501, "Call not found", HttpStatus.NOT_FOUND),
  CALL_INITIATE_FAILED(3502, "Failed to initiate call", HttpStatus.INTERNAL_SERVER_ERROR),
  CALL_ANSWER_FAILED(3503, "Failed to answer call", HttpStatus.INTERNAL_SERVER_ERROR),
  CALL_REJECT_FAILED(3504, "Failed to reject call", HttpStatus.INTERNAL_SERVER_ERROR),
  CALL_END_FAILED(3505, "Failed to end call", HttpStatus.INTERNAL_SERVER_ERROR),
  USER_NOT_AVAILABLE(3506, "User is not available", HttpStatus.BAD_REQUEST),
  USER_OFFLINE(3507, "User is offline", HttpStatus.BAD_REQUEST),
  INVALID_CALL_TYPE(3508, "Invalid call type", HttpStatus.BAD_REQUEST),
  PEER_NOT_FOUND(3509, "Peer not found", HttpStatus.NOT_FOUND),
  INVALID_DATA(3510, "Invalid data", HttpStatus.BAD_REQUEST),
  CALL_STATUS_UPDATE_FAILED(3511, "Call status update failed", HttpStatus.BAD_REQUEST),
  THREAD_INTERRUPTED(3512, "Thread interrupted", HttpStatus.INTERNAL_SERVER_ERROR),
  // ============= POST ERRORS (4000-4999) =============
  POST_NOT_FOUND(4001, "Post not found", HttpStatus.NOT_FOUND),
  POST_CREATE_FAILED(4002, "Failed to create post", HttpStatus.INTERNAL_SERVER_ERROR),
  COMMENT_NOT_FOUND(4003, "Comment not found", HttpStatus.NOT_FOUND),
  ALREADY_LIKED(4004, "Already liked this post", HttpStatus.BAD_REQUEST),
  NOT_LIKED_YET(4005, "Not liked this post yet", HttpStatus.BAD_REQUEST),
  CANNOT_LIKE_YOUR_POST(4006, "Can not like your post", HttpStatus.BAD_REQUEST),
  CANNOT_SAVE_YOUR_POST(4007, "Can not save your post", HttpStatus.BAD_REQUEST),

  // ============= STORY ERRORS (5000-5999) =============
  STORY_NOT_FOUND(5001, "Story not found", HttpStatus.NOT_FOUND),
  STORY_EXPIRED(5002, "Story has expired", HttpStatus.GONE),

  // ============= NOTIFICATION ERRORS (6000-6999) =============
  NOTIFICATION_NOT_FOUND(6001, "Notification not found", HttpStatus.NOT_FOUND),
  NOTIFICATION_SEND_FAILED(6002, "Failed to send notification", HttpStatus.INTERNAL_SERVER_ERROR),

  // ============= PROFILE ERRORS (7000-7999) =============
  PROFILE_NOT_FOUND(7001, "Profile not found", HttpStatus.NOT_FOUND),
  ALREADY_FOLLOWING(7002, "Already following this user", HttpStatus.BAD_REQUEST),
  NOT_FOLLOWING(7003, "Not following this user", HttpStatus.BAD_REQUEST),
  CANNOT_FOLLOW_YOURSELF(7004, "Can not follow yourself", HttpStatus.BAD_REQUEST),
  // ============= WEBSOCKET ERRORS (8000-8999) =============
  TOKEN_REQUIRED(8001, "Token is required", HttpStatus.UNAUTHORIZED),
  AUTH_FAILED(8002, "Authentication failed", HttpStatus.UNAUTHORIZED),
  DISCONNECT_FAILED(8003, "Disconnect failed", HttpStatus.INTERNAL_SERVER_ERROR),
  MESSAGE_SEND_FAILED(8004, "Message send failed", HttpStatus.INTERNAL_SERVER_ERROR),
  SEND_EVENT_FAILED(8005, "Send event failed", HttpStatus.INTERNAL_SERVER_ERROR),
  WEBRTC_OFFER_FAILED(8006, "WebRTC offer failed", HttpStatus.INTERNAL_SERVER_ERROR),
  WEBRTC_ANSWER_FAILED(8007, "WebRTC answer failed", HttpStatus.INTERNAL_SERVER_ERROR),
  ICE_CANDIDATE_FAILED(8008, "ICE candidate failed", HttpStatus.INTERNAL_SERVER_ERROR),
  // ============= Kafka ERRORS (9000-9998) =============
  OUTBOX_PUBLISHER_FAILED(9000, "Outbox publisher failed", HttpStatus.INTERNAL_SERVER_ERROR);
  private final int code;
  private final String message;
  private final HttpStatus httpStatusCode;
}
