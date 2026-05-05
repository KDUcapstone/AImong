package com.kduniv.aimong.feature.home.data.model

import com.google.gson.annotations.SerializedName

data class StreakCalendarData(
    @SerializedName("yearMonth") val yearMonth: String? = null,
    @SerializedName("continuousDays") val continuousDays: Int? = null,
    @SerializedName("completedDates") val completedDates: List<String>? = null,
    @SerializedName("today") val today: String? = null
)
