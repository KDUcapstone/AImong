package com.kduniv.aimong.core.network

import com.google.gson.annotations.SerializedName

/**
 * API v1.5 공통 래퍼. 실패 시 `data`는 null일 수 있어 nullable로 둔다.
 */
data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: T? = null,
    @SerializedName("error") val error: ApiError? = null,
    @SerializedName("requestId") val requestId: String? = null
)

data class ApiError(
    @SerializedName("code") val code: String,
    @SerializedName("message") val message: String
)

/**
 * `success == true`이고 `data`가 있으면 [Result.success], 아니면 API/빈 본문에 맞는 실패.
 */
fun <T> ApiResponse<T>.toResult(): Result<T> {
    if (!success) {
        return Result.failure(Exception(ApiErrorMapper.userMessageForApiError(error)))
    }
    val payload = data
        ?: return Result.failure(Exception("응답 데이터가 비어 있습니다."))
    return Result.success(payload)
}
