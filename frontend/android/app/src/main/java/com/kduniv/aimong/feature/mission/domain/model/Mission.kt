package com.kduniv.aimong.feature.mission.domain.model

data class Mission(
    /** 학습 세트 ID (예: S0101-L1) */
    val setId: String,
    /** 퀴즈 API에서 사용하는 mission UUID */
    val missionId: String,
    val missionCode: String,
    val levelNo: Int,
    val stage: Int,
    val difficulty: String,
    val title: String,
    val description: String,
    val isUnlocked: Boolean,
    val isCompleted: Boolean,
    val completedAt: String?,
    val isReviewable: Boolean
)

data class MissionProgress(
    val completedSetCount: Int,
    val totalSetCount: Int,
    val currentLevelNo: Int
)
