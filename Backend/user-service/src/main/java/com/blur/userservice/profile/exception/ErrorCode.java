package com.blur.userservice.profile.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    USER_EXISTED(1001, "User already exists", HttpStatus.BAD_REQUEST),
    INVALID_KEY(1001, "Invalid message key", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1003, "User name must be at least {min} characters", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1004, "Password must be at least {min} characters", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1005, "User not exists", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "you do not have permission", HttpStatus.FORBIDDEN),
    INVALID_DOB(1008, "Your age must be at least {min}", HttpStatus.BAD_REQUEST),
    USER_PROFILE_NOT_FOUND(1010, "User profile not found", HttpStatus.NOT_FOUND),
    PASSWORD_EXISTED(1010, "Password existed", HttpStatus.BAD_REQUEST),
    INVALID_SECRET_KEY(1011, "Invalid secret ket", HttpStatus.BAD_REQUEST),
    USER_NOT_EXIST(1012, "User does not exist", HttpStatus.NOT_FOUND),
    CANNOT_FOLLOW_YOURSELF(1013, "Can't follow yourself", HttpStatus.FORBIDDEN),
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR);
    private int code;
    private String message;
    private HttpStatusCode httpStatusCode;
}
