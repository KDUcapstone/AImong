package com.kduniv.aimong.feature.home.presentation

/** 홈에서 퀴즈로 전달할 인자 — entrySetId가 있으면 mission-sets 조회, 없으면 missionId+starLevel */
data class HomeQuizNavigation(
    val entrySetId: String = "",
    val missionId: String = "",
    val starLevel: Int = -1
) {
    fun canNavigate(): Boolean =
        entrySetId.isNotBlank() || (missionId.isNotBlank() && starLevel in 1..3)
}

/** PM 시안: 세로 미션 경로 위 노드 */
sealed class HomePathItem {
    data class SectionHeader(val stage: Int, val title: String) : HomePathItem()

    data class Completed(
        val order: Int,
        val title: String,
        val missionId: String,
        val quizNav: HomeQuizNavigation,
        val icon: String = "⭐"
    ) : HomePathItem()

    /** 스테이지(10노드) 블록 사이 가로 구분선 1줄 */
    object InterStageDivider : HomePathItem()

    data class TodayStart(
        val quizNav: HomeQuizNavigation,
        val missionTitle: String,
        val enabled: Boolean,
        val icon: String = "🌟"
    ) : HomePathItem()

    data class Review(
        val quizNav: HomeQuizNavigation,
        val subtitle: String
    ) : HomePathItem()

    data class Locked(val hint: String) : HomePathItem()
}
