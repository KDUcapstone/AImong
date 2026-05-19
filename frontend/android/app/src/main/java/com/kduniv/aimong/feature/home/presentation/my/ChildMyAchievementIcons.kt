package com.kduniv.aimong.feature.home.presentation.my

import androidx.annotation.DrawableRes
import com.kduniv.aimong.R

internal object ChildMyAchievementIcons {

    @DrawableRes
    fun iconFor(achievementType: String): Int {
        val t = achievementType.uppercase()
        return when {
            t.contains("STREAK") -> R.drawable.ic_flame
            t.contains("FIRST") || t.contains("MISSION") || t.contains("CLEAR") -> R.drawable.ic_check_circle_mint
            t.contains("QUIZ") || t.contains("PERFECT") || t.contains("SCORE") -> R.drawable.ic_chip_trophy
            t.contains("STAR") || t.contains("COLLECT") -> R.drawable.ic_star_filled
            t.contains("LEVEL") || t.contains("XP") -> R.drawable.ic_trending_up
            t.contains("PET") || t.contains("GACHA") -> R.drawable.ic_nav_collection_color
            t.contains("CHAT") -> R.drawable.ic_child_quest_speech_bubble
            else -> R.drawable.ic_nav_study_color
        }
    }
}
