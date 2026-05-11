package com.kduniv.aimong.feature.mission.data.model

import com.google.gson.annotations.SerializedName

/** GET /missions — v2.3 (소단원 × starLevel 1~3) */
data class MissionsMapResponseData(
    @SerializedName("stages") val stages: List<MissionStageV23Dto> = emptyList(),
    @SerializedName("progress") val progress: MissionsProgressV23Dto? = null
)

data class MissionStageV23Dto(
    @SerializedName("stage") val stage: Int = 0,
    @SerializedName("title") val title: String? = null,
    @SerializedName("missions") val missions: List<MissionV23Dto> = emptyList()
)

data class MissionV23Dto(
    @SerializedName("missionId") val missionId: Long = 0,
    @SerializedName("missionCode") val missionCode: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("isUnlocked") val isUnlocked: Boolean = false,
    @SerializedName("starLevels") val starLevels: List<MissionStarLevelV23Dto> = emptyList()
)

data class MissionStarLevelV23Dto(
    @SerializedName("starLevel") val starLevel: Int = 0,
    @SerializedName("label") val label: String = "",
    @SerializedName("totalSetCount") val totalSetCount: Int = 0,
    @SerializedName("completedSetCount") val completedSetCount: Int = 0,
    @SerializedName("isPlayable") val isPlayable: Boolean = false,
    @SerializedName("isReviewable") val isReviewable: Boolean = false
)

data class MissionsProgressV23Dto(
    @SerializedName("completedSetCount") val completedSetCount: Int = 0,
    @SerializedName("totalSetCount") val totalSetCount: Int = 0
)
