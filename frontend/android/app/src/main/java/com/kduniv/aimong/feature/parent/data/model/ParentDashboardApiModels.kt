package com.kduniv.aimong.feature.parent.data.model

import com.google.gson.annotations.SerializedName

data class ParentChildSummaryResponseData(
    @SerializedName("nickname") val nickname: String,
    @SerializedName("profileImageType") val profileImageType: String,
    @SerializedName("totalXp") val totalXp: Int = 0,
    @SerializedName("continuousDays") val continuousDays: Int = 0,
    @SerializedName("shieldCount") val shieldCount: Int = 0,
    @SerializedName("weeklyCompletedSetCount") val weeklyCompletedSetCount: Int = 0,
    @SerializedName("totalCompletedSetCount") val totalCompletedSetCount: Int = 0,
    @SerializedName("currentLevelNo") val currentLevelNo: Int = 0,
    @SerializedName("lastActiveAt") val lastActiveAt: String? = null
)

data class ParentWeeklyStatsResponseData(
    @SerializedName("weekStart") val weekStart: String? = null,
    @SerializedName("weekEnd") val weekEnd: String? = null,
    @SerializedName("totalWeeklyXp") val totalWeeklyXp: Int = 0,
    @SerializedName("totalWeeklyMissions") val totalWeeklyMissions: Int = 0,
    @SerializedName("dailyStats") val dailyStats: List<ParentDailyStatDto> = emptyList()
)

data class ParentDailyStatDto(
    @SerializedName("date") val date: String,
    @SerializedName("dayOfWeek") val dayOfWeek: String,
    @SerializedName("completedSetCount") val completedSetCount: Int = 0,
    @SerializedName("xpEarned") val xpEarned: Int = 0
)

data class ParentWeakPointsResponseData(
    @SerializedName("page") val page: Int = 0,
    @SerializedName("size") val size: Int = 20,
    @SerializedName(value = "totalCount", alternate = ["totalElements"])
    val totalCount: Int = 0,
    @SerializedName("totalPages") val totalPages: Int = 0,
    @SerializedName("hasNext") val hasNext: Boolean = false,
    @SerializedName("analyzedPeriod") val analyzedPeriod: String? = null,
    @SerializedName(value = "weakPoints", alternate = ["items"])
    val weakPoints: List<ParentWeakPointDto> = emptyList()
)

data class ParentWeakPointDto(
    @SerializedName("missionId") val missionId: String? = null,
    @SerializedName("missionTitle") val missionTitle: String? = null,
    @SerializedName("setId") val setId: String? = null,
    @SerializedName("setTitle") val setTitle: String? = null,
    @SerializedName("starLevel") val starLevel: Int? = null,
    @SerializedName("levelNo") val levelNo: Int? = null,
    @SerializedName("difficulty") val difficulty: String? = null,
    @SerializedName("stage") val stage: Int? = null,
    @SerializedName("incorrectRate") val incorrectRate: Double = 0.0,
    @SerializedName("attemptCount") val attemptCount: Int = 0
)

