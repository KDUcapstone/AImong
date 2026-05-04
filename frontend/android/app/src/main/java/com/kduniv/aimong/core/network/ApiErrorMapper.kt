package com.kduniv.aimong.core.network

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import retrofit2.HttpException

/**
 * HTTP 에러 바디·ApiResponse.error의 code를 사용자용 문구로 매핑.
 */
object ApiErrorMapper {

    private val gson = Gson()

    private data class ErrorEnvelope(
        @SerializedName("success") val success: Boolean? = null,
        @SerializedName("error") val error: ApiError? = null
    )

    fun userMessageForApiError(error: ApiError?): String {
        if (error == null) return "요청을 처리하지 못했습니다."
        return userMessageForCode(error.code, error.message)
    }

    fun userMessageForHttpException(e: HttpException): String {
        val code = e.code()
        val body = try {
            e.response()?.errorBody()?.string()
        } catch (_: Exception) {
            null
        }
        val parsed = body?.let { parseErrorEnvelope(it) }?.error
        return when {
            parsed != null -> userMessageForCode(parsed.code, parsed.message)
            code == 400 -> "요청 형식이 올바르지 않습니다."
            code == 401 -> userMessageForCode("UNAUTHORIZED", null)
            code == 403 -> userMessageForCode("FORBIDDEN", null)
            code == 404 -> userMessageForCode("NOT_FOUND", null)
            code == 429 -> userMessageForCode("TOO_MANY_REQUESTS", null)
            code == 409 -> "요청을 처리할 수 없습니다. 문제를 다시 불러오거나 이전 화면으로 돌아가 주세요."
            code in 500..599 ->
                "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
            else -> "문제를 불러오지 못했습니다 (${code})"
        }
    }

    private fun parseErrorEnvelope(json: String): ErrorEnvelope? =
        try {
            gson.fromJson(json, ErrorEnvelope::class.java)
        } catch (_: Exception) {
            null
        }

    fun userMessageForCode(code: String, fallbackMessage: String?): String {
        val trimmed = fallbackMessage?.trim()?.takeIf { it.isNotEmpty() }
        if (trimmed != null) return trimmed

        val base = when (code) {
            // 미션·자녀 로그인 등 공통 — 구체 문구는 서버 message 우선
            "NOT_FOUND" -> "요청한 정보를 찾을 수 없습니다."
            "TOO_MANY_REQUESTS" -> "잠시 후 다시 시도해주세요."
            "FORBIDDEN" -> "아직 잠긴 미션이에요. 이전 단계를 먼저 완료해주세요."
            "MISSION_SET_NOT_READY" -> "문제 세트를 준비하는 데 실패했습니다. 잠시 후 다시 시도해주세요."
            "UNAUTHORIZED" -> "로그인이 필요합니다."
            "ATTEMPT_EXPIRED" -> "문제 세션이 만료되었어요. 다시 문제를 불러와주세요."
            "ATTEMPT_ALREADY_SUBMITTED" -> "이미 제출한 문제 세트예요."
            "QUESTION_NOT_FOUND" -> "문항을 찾을 수 없습니다."
            "INTERNAL_ERROR" -> "서버 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
            "BAD_REQUEST" -> null
            else -> null
        }
        return base ?: "문제를 불러오지 못했습니다."
    }
}
