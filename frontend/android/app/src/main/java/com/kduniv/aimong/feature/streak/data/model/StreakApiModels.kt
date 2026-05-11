package com.kduniv.aimong.feature.streak.data.model

import com.google.gson.annotations.SerializedName

data class StreakPartnerDto(
    @SerializedName("childId") val childId: String,
    @SerializedName("nickname") val nickname: String,
    @SerializedName("todayCompleted") val todayCompleted: Boolean
)

data class StreakStatusData(
    @SerializedName("continuousDays") val continuousDays: Int,
    @SerializedName("lastCompletedDate") val lastCompletedDate: String?,
    @SerializedName("todayMissionCount") val todayMissionCount: Int,
    @SerializedName("shieldCount") val shieldCount: Int,
    @SerializedName("partner") val partner: StreakPartnerDto?
)

