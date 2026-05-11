package com.kduniv.aimong.feature.mission.domain.model

/** 소단원(미션) + 별 난이도 3단 — v2.3 */
data class Mission(
    val missionId: String,
    val missionCode: String,
    val stage: Int,
    val title: String,
    val description: String,
    val isUnlocked: Boolean,
    val starLevels: List<MissionStarLevel>
)

data class MissionStarLevel(
    val starLevel: Int,
    val label: String,
    val totalSetCount: Int,
    val completedSetCount: Int,
    val isPlayable: Boolean,
    val isReviewable: Boolean
) {
    val isCompleted: Boolean
        get() = totalSetCount > 0 && completedSetCount >= totalSetCount
}

data class MissionProgress(
    val completedSetCount: Int,
    val totalSetCount: Int
)
