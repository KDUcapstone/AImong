package com.aimong.backend.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증에 실패했습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, "CONFLICT", "요청 상태가 충돌했습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "리소스를 찾을 수 없습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "토큰이 만료되었습니다."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS", "잠시 후 다시 시도해 주세요."),
    CHILD_NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "자녀 프로필을 찾을 수 없습니다."),
    CHILD_CODE_NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "코드를 다시 확인해 주세요."),
    MISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "미션을 찾을 수 없습니다."),
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "문제를 찾을 수 없습니다."),
    MISSION_LOCKED(HttpStatus.FORBIDDEN, "FORBIDDEN", "아직 잠긴 미션입니다."),
    MISSION_QUESTIONS_LOCKED(HttpStatus.FORBIDDEN, "FORBIDDEN", "아직 잠긴 미션입니다. 이전 단계를 먼저 완료해 주세요."),
    MISSION_SET_NOT_READY(HttpStatus.INTERNAL_SERVER_ERROR, "MISSION_SET_NOT_READY", "문제 세트를 준비하는 데 실패했습니다."),
    QUIZ_ATTEMPT_INVALID(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "유효하지 않은 문제 시도입니다."),
    QUIZ_ATTEMPT_REQUIRED(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "문제 시도 정보가 필요합니다."),
    QUIZ_ANSWERS_REQUIRED(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "답안은 10개를 모두 제출해 주세요."),
    QUIZ_DUPLICATE_QUESTION(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "같은 문제를 중복 제출할 수 없습니다."),
    QUIZ_ATTEMPT_ALREADY_SUBMITTED(HttpStatus.CONFLICT, "ATTEMPT_ALREADY_SUBMITTED", "이미 제출된 문제 세트입니다."),
    ATTEMPT_EXPIRED(HttpStatus.CONFLICT, "ATTEMPT_EXPIRED", "문제 시도가 만료되었습니다. 다시 문제를 불러와 주세요."),
    CODE_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "코드 생성에 실패했습니다."),
    CODE_GENERATION_FAILED_WITH_RETRY(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "코드 생성에 실패했습니다. 다시 시도해 주세요."),
    NICKNAME_REQUIRED(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "닉네임을 입력해 주세요."),
    NICKNAME_TOO_LONG(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "닉네임은 20자 이하여야 합니다."),
    CHILD_CODE_REQUIRED(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "코드를 입력해 주세요."),
    CHILD_CODE_INVALID_FORMAT(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "올바른 형식의 코드를 입력해 주세요."),
    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 오류가 발생했습니다."),
    SUBMIT_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "결과 저장에 실패했습니다. 다시 시도해 주세요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
