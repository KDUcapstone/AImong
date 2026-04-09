package com.aimong.backend.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, "CONFLICT", "요청 상태가 충돌했습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "대상을 찾을 수 없습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "토큰이 만료되었습니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS", "잠시 후 다시 시도해 주세요."),
    CHILD_NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "자녀 프로필을 찾을 수 없습니다."),
    CHILD_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "코드를 다시 확인해 주세요."),
    MISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "미션을 찾을 수 없습니다."),
    MISSION_LOCKED(HttpStatus.FORBIDDEN, "FORBIDDEN", "이전 단계를 먼저 완료해야 합니다."),
    QUIZ_ATTEMPT_INVALID(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "유효하지 않은 문제 세션입니다."),
    QUIZ_ATTEMPT_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "CONFLICT", "이미 제출된 문제 세션입니다."),
    CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "코드 생성에 실패했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
