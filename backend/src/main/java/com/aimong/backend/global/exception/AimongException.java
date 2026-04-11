package com.aimong.backend.global.exception;

import lombok.Getter;

@Getter
public class AimongException extends RuntimeException {

    private final ErrorCode errorCode;

    public AimongException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public AimongException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
