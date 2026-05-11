package com.kduniv.aimong.feature.quest.data.model

import com.google.gson.annotations.SerializedName

data class AchievementsResponseData(
    @SerializedName("achievements") val achievements: List<AchievementItemDto> = emptyList()
)

data class AchievementItemDto(
    @SerializedName("achievementType") val achievementType: String,
    @SerializedName("label") val label: String,
    @SerializedName("isCompleted") val isCompleted: Boolean = false,
    @SerializedName("completedAt") val completedAt: String? = null,
    @SerializedName("progress") val progress: AchievementProgressDto? = null
)

data class AchievementProgressDto(
    @SerializedName("current") val current: Int = 0,
    @SerializedName("required") val required: Int = 0
)

