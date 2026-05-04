package com.kduniv.aimong.feature.home.domain.model

/**
 * GET /home/streak-calendar 응답을 UI용으로 정규화한 값.
 */
data class StreakCalendarResult(
    val yearMonth: String,
    val continuousDays: Int,
    val completedDates: List<String>,
    val today: String?
)
