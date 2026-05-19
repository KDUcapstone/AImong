package com.kduniv.aimong.feature.home.presentation.quest

import androidx.annotation.DrawableRes

data class QuestRewardCelebrationUi(
    val questTitle: String,
    val lines: List<QuestRewardCelebrationLine>,
)

data class QuestRewardCelebrationLine(
    @DrawableRes val iconRes: Int,
    val amountText: String,
    val labelText: String,
)
