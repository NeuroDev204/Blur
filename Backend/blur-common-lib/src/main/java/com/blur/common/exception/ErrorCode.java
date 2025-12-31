package com.blur.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Invalid message key", HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1002, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1003, "You do not have permission", HttpStatus.FORBIDDEN),
    USER_EXISTED(1019, "User already exists", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(1021, "User not found", HttpStatus.NOT_FOUND),
    POST_NOT_FOUND(1020, "Post not found", HttpStatus.NOT_FOUND),
    CANNOT_SAVE_YOUR_POST(1022, "Cannot save your own post", HttpStatus.BAD_REQUEST),
    CANNOT_LIKE_YOUR_POST(1023, "Cannot like your own post", HttpStatus.BAD_REQUEST),
    ALREADY_LIKED(1024, "You have already liked this post", HttpStatus.BAD_REQUEST),
    POST_NOT_LIKED(1025, "You have not liked this post", HttpStatus.BAD_REQUEST),
    ALREADY_SAVED(1026, "You have already saved this post", HttpStatus.BAD_REQUEST),
    POST_NOT_SAVED(1027, "You have not saved this post", HttpStatus.NOT_FOUND),
    COMMENT_NOT_FOUND(1030, "Comment not found", HttpStatus.NOT_FOUND),
    COMMENT_REPLY_NOT_FOUND(1031, "Comment reply not found", HttpStatus.NOT_FOUND),
    USERNAME_INVALID(1003, "User name must be at least {min} characters", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1004, "Password must be at least {min} characters", HttpStatus.BAD_REQUEST),
    INVALID_USERNAME_OR_PASSSWORD(1009, "Invalid username or passsword", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1005, "User not exists", HttpStatus.NOT_FOUND),
    INVALID_DOB(1008, "Your age must be at least {min}", HttpStatus.BAD_REQUEST),
    PASSWORD_EXISTED(1010, "Password existed", HttpStatus.BAD_REQUEST),
    INVALID_SECRET_KEY(1011, "Invalid secret ket", HttpStatus.BAD_REQUEST),
    CANNOT_FOLLOW_YOURSELF(1013, "Can't follow yourself", HttpStatus.FORBIDDEN),
    USER_NOT_EXIST(1012, "User does not exist", HttpStatus.NOT_FOUND),
    USER_PROFILE_NOT_FOUND(1014, "User profile not found", HttpStatus.NOT_FOUND);

    private final int code;
    private final String message;
    private final HttpStatus httpStatusCode;
}
