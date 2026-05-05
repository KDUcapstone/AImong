package com.kduniv.aimong.feature.quiz.data.model

import com.google.gson.annotations.SerializedName

data class QuestionReportRequest(
    @SerializedName("reasonCode") val reasonCode: String,
    @SerializedName("detail") val detail: String? = null
)

data class QuestionReportResponseData(
    @SerializedName("questionId") val questionId: String,
    @SerializedName("issueId") val issueId: String,
    @SerializedName("issueStatus") val issueStatus: String,
    @SerializedName("quarantined") val quarantined: Boolean
)
