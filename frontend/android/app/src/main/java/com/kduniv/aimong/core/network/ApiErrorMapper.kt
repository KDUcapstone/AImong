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

    fun userMessageForChatHttpException(e: HttpException, imageRequested: Boolean): String {
        val base = userMessageForHttpException(e)
        if (!imageRequested) return base
        val body = try {
            e.response()?.errorBody()?.string()
        } catch (_: Exception) {
            null
        }
        val parsed = body?.let { parseErrorEnvelope(it) }?.error
        val code = parsed?.code
        val serverMsg = parsed?.message?.trim().orEmpty()
        return when {
            e.code() == 429 && isImageLimitMessage(serverMsg) ->
                chatImageLimitMessage()
            e.code() == 504 && imageRequested ->
                chatImageTimeoutMessage()
            e.code() in 500..599 && imageRequested && isImageFailureMessage(serverMsg) ->
                chatImageFailedMessage()
            code == "GATEWAY_TIMEOUT" && imageRequested -> chatImageTimeoutMessage()
            code == "INTERNAL_ERROR" && imageRequested && isImageFailureMessage(serverMsg) ->
                chatImageFailedMessage()
            else -> base
        }
    }

    private fun isImageLimitMessage(message: String): Boolean =
        message.contains("image", ignoreCase = true) &&
            (message.contains("limit", ignoreCase = true) || message.contains("generation", ignoreCase = true))

    private fun isImageFailureMessage(message: String): Boolean =
        message.contains("image", ignoreCase = true) &&
            (message.contains("generation", ignoreCase = true) || message.contains("failed", ignoreCase = true))

    private fun chatImageLimitMessage(): String =
        "오늘 그림 그리기 횟수를 다 썼어요. 내일 또 만나요!"

    private fun chatImageTimeoutMessage(): String =
        "그림을 만드는 데 시간이 너무 걸렸어요. 다시 시도해 주세요."

    private fun chatImageFailedMessage(): String =
        "그림을 만들지 못했어요. 잠시 후 다시 시도해 주세요."

    fun userMessageForHttpException(e: HttpException): String {
        val code = e.code()
        val body = try {
            e.response()?.errorBody()?.string()
        } catch (_: Exception) {
            null
        }
        val parsed = body?.let { parseErrorEnvelope(it) }?.error
        // 바디에 v1.5 `error` 객체가 있으면 HTTP 상태와 무관하게 code 기반 메시지 우선
        if (parsed != null) {
            return userMessageForCode(parsed.code, parsed.message)
        }
        return when {
            code == 400 -> userMessageForCode("BAD_REQUEST", null)
            code == 401 -> userMessageForCode("UNAUTHORIZED", null)
            code == 403 -> userMessageForCode("FORBIDDEN", null)
            code == 404 -> userMessageForCode("NOT_FOUND", null)
            code == 429 -> userMessageForCode("TOO_MANY_REQUESTS", null)
            code == 504 -> userMessageForCode("GATEWAY_TIMEOUT", null)
            code == 409 -> userMessageForCode("CONFLICT", null)
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
            "FORBIDDEN" -> "접근이 제한되었거나 아직 이용할 수 없는 단계예요."
            "VALIDATION_ERROR" -> "입력 값을 다시 확인해 주세요."
            "MISSION_SET_NOT_READY" -> "문제 세트를 준비하는 데 실패했습니다. 잠시 후 다시 시도해주세요."
            "MISSION_SET_NOT_FOUND" -> "문제 세트를 찾을 수 없어요."
            "INVALID_STAR_LEVEL" -> "난이도(별) 선택이 올바르지 않아요."
            "INVALID_ANSWER_FORMAT" -> "답안 형식이 문제 유형과 맞지 않아요."
            "MISSION_NOT_FOUND" -> "미션을 찾을 수 없어요."
            "MISSION_LOCKED" -> "아직 열리지 않은 미션이에요."
            "INSUFFICIENT_ENERGY" -> "에너지가 부족해요. 잠시 후 다시 도전해 주세요."
            "GEAR_NOT_ENOUGH" -> "톱니바퀴가 부족해요."
            "ATTEMPT_NOT_REVIVABLE" -> "지금은 하트를 회복할 수 없어요."
            "UNAUTHORIZED" -> "로그인이 필요합니다."
            "ATTEMPT_EXPIRED" -> "문제 세션이 만료되었어요. 다시 문제를 불러와주세요."
            "ATTEMPT_NOT_FOUND" -> "문제 세션을 찾을 수 없어요."
            "ATTEMPT_ALREADY_SUBMITTED" -> "이미 제출한 문제 세트예요."
            "ATTEMPT_ALREADY_CLOSED" -> "이미 종료된 문제 세션이에요."
            "QUESTION_NOT_FOUND" -> "문항을 찾을 수 없습니다."
            "REPORT_NOT_FOUND" -> "결과 정보를 찾을 수 없어요."
            "INTERNAL_ERROR", "INTERNAL_SERVER_ERROR" ->
                "서버 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
            "GATEWAY_TIMEOUT" -> "AI 친구가 생각 중이에요. 다시 시도해볼까요?"
            "BAD_REQUEST" ->
                "요청을 확인해 주세요. (티켓·조각 보유량 또는 입력값)"
            // 복귀 보상 409, 가챠 교환 중복 펫 등 — 서버 message 없을 때만 사용
            "CONFLICT" -> "이미 보유한 펫이에요"
            "CHILD_LIMIT_EXCEEDED" -> "등록 가능한 자녀 수를 초과했습니다."
            "CHILD_NOT_FOUND" -> "자녀를 찾을 수 없습니다."
            "MAX_QUEST_LIMIT" -> "먼저 기존 퀘스트를 완료하거나 취소해주세요."
            "QUEST_NOT_FOUND" -> "퀘스트를 찾을 수 없습니다."
            "QUEST_NOT_PENDING" -> "확인 대기 중인 퀘스트가 아닙니다."
            "QUEST_NOT_CANCELLABLE" -> "이미 완료 요청됐거나 종료된 퀘스트예요."
            "QUEST_EXPIRED" -> "기간이 지난 퀘스트예요."
            "QUEST_NOT_ACTIVE" -> "이미 완료 요청했거나 종료된 퀘스트예요."
            "ALREADY_TRIGGERED" -> "이미 지급된 보상은 수정할 수 없습니다."
            else -> null
        }
        return base ?: "문제를 불러오지 못했습니다."
    }
}
