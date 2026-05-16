package com.kduniv.aimong.feature.quiz.data.model

import com.google.gson.annotations.SerializedName

/** v2.4: GET /mission-attempts/{attemptId} */
data class MissionAttemptResponseData(
    @SerializedName("attemptId") val attemptId: String,
    /** v2.4: 서버가 문자열 setId를 줄 수 있어 String으로 수용 */
    @SerializedName("setId") val setId: String,
    /** v2.4: missionId가 UUID/String일 수 있어 String으로 수용 */
    @SerializedName("missionId") val missionId: String? = null,
    @SerializedName("starLevel") val starLevel: Int? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("isReview") val isReview: Boolean = false,
    @SerializedName("expiresAt") val expiresAt: String? = null,
    @SerializedName("remainingSeconds") val remainingSeconds: Int? = null,
    @SerializedName("answeredQuestionIds") val answeredQuestionIds: List<String> = emptyList(),
    @SerializedName("remainingLives") val remainingLives: Int? = null,
    @SerializedName("wrongCountInSession") val wrongCountInSession: Int? = null,
    @SerializedName("reviveCount") val reviveCount: Int? = null,
    @SerializedName("canRevive") val canRevive: Boolean? = null,
    @SerializedName("questionCount") val questionCount: Int? = null
)

/** v2.4: POST /mission-attempts/{attemptId}/abandon */
data class MissionAttemptAbandonRequest(
    @SerializedName("reason") val reason: String
)

data class MissionAttemptAbandonResponseData(
    @SerializedName("abandoned") val abandoned: Boolean = false,
    @SerializedName("attemptId") val attemptId: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("energyRefunded") val energyRefunded: Boolean = false
)

/** v2.7: POST /mission-attempts/{attemptId}/revive */
data class MissionAttemptReviveRequest(
    @SerializedName("useCurrency") val useCurrency: Boolean = true
)

data class MissionAttemptReviveResponseData(
    @SerializedName("attemptId") val attemptId: String? = null,
    @SerializedName("remainingLives") val remainingLives: Int = 3,
    @SerializedName("reviveCount") val reviveCount: Int? = null,
    @SerializedName("reviveCost") val reviveCost: Int? = null,
    @SerializedName("gearBalance") val gearBalance: Int? = null
)

/** v2.4: POST /mission-sets/{setId}/check */
data class MissionSetCheckRequest(
    @SerializedName("questionId") val questionId: String,
    @SerializedName("answer") val answer: String
)

data class MissionSetCheckResponseData(
    @SerializedName("questionId") val questionId: String,
    @SerializedName("isCorrect") val isCorrect: Boolean = false,
    @SerializedName("correctAnswer") val correctAnswer: String? = null,
    @SerializedName("explanation") val explanation: String? = null,
    @SerializedName("remainingLives") val remainingLives: Int? = null,
    @SerializedName("canRevive") val canRevive: Boolean? = null,
    @SerializedName("reviveCost") val reviveCost: Int? = null,
    @SerializedName("gearBalance") val gearBalance: Int? = null,
    @SerializedName("nextActions") val nextActions: List<String>? = null
)
