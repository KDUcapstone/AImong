package com.aimong.backend.global.exception;

import lombok.Getter;

@Getter
public class AimongException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String customMessage;

    public AimongException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.customMessage = null;
    }

    public AimongException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.customMessage = null;
    }

    public AimongException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
        this.customMessage = customMessage;
    }

    public String getResolvedMessage() {
        return customMessage != null ? customMessage : errorCode.getMessage();
    }
}
