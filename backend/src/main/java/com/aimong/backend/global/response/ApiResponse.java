package com.aimong.backend.global.response;

import com.aimong.backend.global.exception.ErrorCode;

public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorBody error
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return new ApiResponse<>(false, null, new ErrorBody(errorCode.getCode(), errorCode.getMessage()));
    }

    public record ErrorBody(
            String code,
            String message
    ) {
    }
}
