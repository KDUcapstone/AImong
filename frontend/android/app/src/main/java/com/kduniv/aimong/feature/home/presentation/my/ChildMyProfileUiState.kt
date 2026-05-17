package com.kduniv.aimong.feature.home.presentation.my

data class ChildMyProfileUiState(
    val nickname: String = "",
    val profileSubtitle: String = "",
    val completedMissionCount: Int = 0,
    val totalXp: Int = 0,
    val petCount: Int = 0,
    val streakDays: Int = 0,
    val badges: List<ChildMyBadgeUi> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

data class ChildMyBadgeUi(
    val achievementType: String,
    val label: String,
    val emoji: String,
    val isUnlocked: Boolean
)
