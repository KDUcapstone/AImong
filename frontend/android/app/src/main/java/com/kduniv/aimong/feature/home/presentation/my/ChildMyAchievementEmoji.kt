package com.kduniv.aimong.feature.home.presentation.my

internal object ChildMyAchievementEmoji {

    fun forType(achievementType: String): String {
        val t = achievementType.uppercase()
        return when {
            t.contains("STREAK") -> "🔥"
            t.contains("FIRST") || t.contains("MISSION") || t.contains("CLEAR") -> "🎯"
            t.contains("QUIZ") || t.contains("PERFECT") || t.contains("SCORE") -> "🏆"
            t.contains("STAR") || t.contains("COLLECT") -> "⭐"
            t.contains("CREAT") || t.contains("ART") || t.contains("PALETTE") -> "🎨"
            t.contains("AI") || t.contains("EXPERT") || t.contains("ROCKET") -> "🚀"
            t.contains("PRIVACY") || t.contains("GUARD") || t.contains("LOCK") -> "🔒"
            t.contains("PROMPT") || t.contains("SPARK") -> "✨"
            t == "SPROUT" -> "🌱"
            t == "EXPLORER" -> "🧭"
            t == "CRITIC" -> "💬"
            t == "GUARDIAN" -> "🛡️"
            else -> "🏅"
        }
    }
}
