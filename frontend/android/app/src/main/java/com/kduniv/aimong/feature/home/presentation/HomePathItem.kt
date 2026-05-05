package com.kduniv.aimong.feature.home.presentation

/** PM 시안: 세로 미션 경로 위 노드 */
sealed class HomePathItem {
    data class SectionHeader(val stage: Int, val title: String) : HomePathItem()

    data class Completed(val order: Int, val title: String, val icon: String = "⭐") : HomePathItem()

    data class TodayStart(
        val missionId: String,
        val missionTitle: String,
        val enabled: Boolean,
        val icon: String = "🌟"
    ) : HomePathItem()

    data class Review(val missionId: String, val subtitle: String) : HomePathItem()

    data class Locked(val hint: String) : HomePathItem()
}
