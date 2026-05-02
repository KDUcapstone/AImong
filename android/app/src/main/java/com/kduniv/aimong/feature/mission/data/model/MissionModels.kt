package com.kduniv.aimong.feature.mission.data.model

import com.google.gson.annotations.SerializedName

data class MissionListResponse(
    @SerializedName("missions") val missions: List<MissionResponse>,
    @SerializedName("stageProgress") val stageProgress: StageProgress
)

data class MissionResponse(
    @SerializedName("id") val id: String,
    @SerializedName("stage") val stage: Int,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("isUnlocked") val isUnlocked: Boolean,
    @SerializedName("isCompleted") val isCompleted: Boolean,
    @SerializedName("completedAt") val completedAt: String?,
    @SerializedName("isReviewable") val isReviewable: Boolean
)

data class StageProgress(
    @SerializedName("stage1Completed") val stage1Completed: Int,
    @SerializedName("stage2Completed") val stage2Completed: Int,
    @SerializedName("stage3Completed") val stage3Completed: Int
)
