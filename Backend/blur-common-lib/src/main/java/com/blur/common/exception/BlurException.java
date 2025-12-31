package com.blur.common.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class BlurException extends RuntimeException{
    private final ErrorCode errorCode;
    public BlurException(ErrorCode errorCode){

        this.errorCode = errorCode;
    }
}
