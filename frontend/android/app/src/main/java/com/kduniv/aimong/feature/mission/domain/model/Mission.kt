package com.kduniv.aimong.feature.mission.domain.model

data class Mission(
    val id: String,
    val stage: Int,
    val title: String,
    val description: String,
    val isUnlocked: Boolean,
    val isCompleted: Boolean,
    val completedAt: String?,
    val isReviewable: Boolean
)

data class MissionProgress(
    val stage1Completed: Int,
    val stage2Completed: Int,
    val stage3Completed: Int
)
