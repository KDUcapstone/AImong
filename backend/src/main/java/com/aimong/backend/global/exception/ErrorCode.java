package com.aimong.backend.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증에 실패했습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, "CONFLICT", "요청 상태가 충돌합니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "리소스를 찾을 수 없습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "토큰이 만료되었습니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS", "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해 주세요."),
    CHILD_NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "자녀 프로필을 찾을 수 없습니다."),
    CHILD_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "자녀 코드를 다시 확인해 주세요."),
    MISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "미션을 찾을 수 없습니다."),
    MISSION_LOCKED(HttpStatus.FORBIDDEN, "FORBIDDEN", "잠긴 미션입니다."),
    MISSION_SET_NOT_READY(HttpStatus.INTERNAL_SERVER_ERROR, "MISSION_SET_NOT_READY", "승인된 고정 문제 10문항이 준비되지 않았습니다."),
    QUIZ_ATTEMPT_INVALID(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "유효하지 않은 quiz attempt입니다."),
    QUIZ_ATTEMPT_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "ATTEMPT_ALREADY_SUBMITTED", "이미 제출한 attempt입니다."),
    ATTEMPT_EXPIRED(HttpStatus.CONFLICT, "ATTEMPT_EXPIRED", "quiz attempt가 만료되었습니다. 문제를 다시 받아 주세요."),
    CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "코드 생성에 실패했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

}
