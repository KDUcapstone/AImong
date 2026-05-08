package com.kduniv.aimong.feature.mission.data.model

import com.google.gson.annotations.SerializedName

/**
 * GET /missions (v2) — 레벨(6) × 단계(3) × 세트(총 96) 학습맵.
 */
data class MissionsMapResponseData(
    @SerializedName("levels") val levels: List<MissionLevelDto> = emptyList(),
    @SerializedName("progress") val progress: MissionsProgressDto? = null
)

data class MissionLevelDto(
    @SerializedName("levelNo") val levelNo: Int,
    @SerializedName("difficulty") val difficulty: String,
    @SerializedName("isUnlocked") val isUnlocked: Boolean = false,
    @SerializedName("completedSetCount") val completedSetCount: Int = 0,
    @SerializedName("totalSetCount") val totalSetCount: Int = 0,
    @SerializedName("stages") val stages: List<MissionStageDto> = emptyList()
)

data class MissionStageDto(
    @SerializedName("stage") val stage: Int,
    @SerializedName("completedSetCount") val completedSetCount: Int = 0,
    @SerializedName("totalSetCount") val totalSetCount: Int = 0,
    @SerializedName("sets") val sets: List<MissionSetDto> = emptyList()
)

data class MissionSetDto(
    @SerializedName("setId") val setId: String,
    @SerializedName("missionId") val missionId: String,
    @SerializedName("missionCode") val missionCode: String,
    @SerializedName("levelNo") val levelNo: Int,
    @SerializedName("stage") val stage: Int,
    @SerializedName("difficulty") val difficulty: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("isUnlocked") val isUnlocked: Boolean = false,
    @SerializedName("isCompleted") val isCompleted: Boolean = false,
    @SerializedName("completedAt") val completedAt: String? = null,
    @SerializedName("isReviewable") val isReviewable: Boolean = false
)

data class MissionsProgressDto(
    @SerializedName("completedSetCount") val completedSetCount: Int = 0,
    @SerializedName("totalSetCount") val totalSetCount: Int = 0,
    @SerializedName("currentLevelNo") val currentLevelNo: Int = 1
)

