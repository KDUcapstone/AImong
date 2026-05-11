package com.kduniv.aimong.feature.quiz.data.model

import com.google.gson.annotations.SerializedName

/** v2.4: GET /mission-attempts/{attemptId} */
data class MissionAttemptResponseData(
    @SerializedName("attemptId") val attemptId: String,
    @SerializedName("setId") val setId: Long,
    @SerializedName("missionId") val missionId: Long? = null,
    @SerializedName("starLevel") val starLevel: Int? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("isReview") val isReview: Boolean = false,
    @SerializedName("expiresAt") val expiresAt: String? = null,
    @SerializedName("remainingSeconds") val remainingSeconds: Int? = null,
    @SerializedName("answeredQuestionIds") val answeredQuestionIds: List<Long> = emptyList(),
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

/** v2.4: POST /mission-sets/{setId}/check */
data class MissionSetCheckRequest(
    @SerializedName("questionId") val questionId: Long,
    @SerializedName("answer") val answer: String
)

data class MissionSetCheckResponseData(
    @SerializedName("questionId") val questionId: Long,
    @SerializedName("isCorrect") val isCorrect: Boolean = false,
    @SerializedName("correctAnswer") val correctAnswer: String? = null,
    @SerializedName("explanation") val explanation: String? = null
)

