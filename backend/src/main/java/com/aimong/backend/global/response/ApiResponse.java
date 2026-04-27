package com.aimong.backend.global.response;

import com.aimong.backend.global.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorBody error,
        String requestId
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, RequestIdUtils.getOrCreate());
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return fail(errorCode, errorCode.getMessage());
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message) {
        return new ApiResponse<>(
                false,
                null,
                new ErrorBody(errorCode.getCode(), message),
                RequestIdUtils.getOrCreate()
        );
    }

    public record ErrorBody(
            String code,
            String message
    ) {
    }
}
