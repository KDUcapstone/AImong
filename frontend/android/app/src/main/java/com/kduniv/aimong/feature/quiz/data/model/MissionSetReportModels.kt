package com.kduniv.aimong.feature.quiz.data.model

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.kduniv.aimong.feature.quiz.data.gson.AttemptIdStringAdapter
import com.kduniv.aimong.feature.quiz.data.gson.SubmitRewardsListAdapter

/** v2.5 GET /mission-sets/{setId}/report */
data class MissionSetReportResponseData(
    @SerializedName("attemptId")
    @JsonAdapter(AttemptIdStringAdapter::class)
    val attemptId: String? = null,
    @SerializedName("setId")
    @JsonAdapter(AttemptIdStringAdapter::class)
    val setId: String? = null,
    @SerializedName("missionId")
    @JsonAdapter(AttemptIdStringAdapter::class)
    val missionId: String? = null,
    @SerializedName("missionCode") val missionCode: String? = null,
    @SerializedName("starLevel") val starLevel: Int? = null,
    @SerializedName("variantNo") val variantNo: Int? = null,
    @SerializedName("score") val score: Int? = null,
    @SerializedName("correctCount") val correctCount: Int? = null,
    @SerializedName("wrongCount") val wrongCount: Int? = null,
    @SerializedName("questionCount") val questionCount: Int? = null,
    @SerializedName("isPassed") val isPassed: Boolean? = null,
    @SerializedName("isPerfect") val isPerfect: Boolean? = null,
    @SerializedName("isFirstClear") val isFirstClear: Boolean? = null,
    @SerializedName("isReview") val isReview: Boolean = false,
    @SerializedName("submittedAt") val submittedAt: String? = null,
    @SerializedName("results") val results: List<MissionSetReportQuestionDto>? = null,
    @SerializedName("rewards")
    @JsonAdapter(SubmitRewardsListAdapter::class)
    val rewards: List<RewardResponse>? = null
)

data class MissionSetReportQuestionDto(
    @SerializedName("questionId")
    @JsonAdapter(AttemptIdStringAdapter::class)
    val questionId: String? = null,
    @SerializedName("questionNo") val questionNo: Int? = null,
    @SerializedName("isCorrect") val isCorrect: Boolean = false,
    @SerializedName("correctAnswer") val correctAnswer: String? = null,
    @SerializedName("submittedAnswer") val submittedAnswer: String? = null,
    @SerializedName("explanation") val explanation: String? = null
)
