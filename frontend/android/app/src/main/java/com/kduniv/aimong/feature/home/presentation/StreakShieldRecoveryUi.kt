package com.kduniv.aimong.feature.home.presentation

data class StreakShieldRecoveryUi(
    val status: String,
    val shieldCount: Int,
    val continuousDays: Int,
    val recoveryDeadlineDate: String?,
) {
    val isRecoverable: Boolean
        get() = status.equals("RECOVERABLE", ignoreCase = true)
}
