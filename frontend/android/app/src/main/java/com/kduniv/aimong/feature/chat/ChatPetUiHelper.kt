package com.kduniv.aimong.feature.chat

import com.kduniv.aimong.feature.gacha.GachaPetCatalog

/** 홈·챗봇에서 공통으로 쓰는 장착 펫 표시 이름·아바타 */
object ChatPetUiHelper {

    fun catalogEntry(petType: String): GachaPetCatalog.Entry? =
        GachaPetCatalog.entryFor(petType)

    /** 장착 펫 말풍선·헤더 아바타 — EGG는 단계 이모지, 그 외는 도감 펫 이모지 */
    fun avatarEmoji(stage: String, petType: String, grade: String = "NORMAL"): String {
        if (stage.equals("EGG", ignoreCase = true)) return stageEmoji(stage)
        return GachaPetCatalog.emojiFor(petType, grade)
    }

    fun resolveDisplayName(petType: String, grade: String): String =
        GachaPetCatalog.displayNameFor(petType, grade)

    fun displayName(petType: String, grade: String): String {
        val tail = petType.substringAfterLast('_', "")
        val short = tail.filter { it.isDigit() }.takeIf { it.isNotBlank() }
        return buildString {
            append(
                when (grade.uppercase()) {
                    "COMMON", "NORMAL" -> "커먼 "
                    "RARE" -> "레어 "
                    "EPIC" -> "에픽 "
                    "LEGEND" -> "레전드 "
                    else -> ""
                }
            )
            append(short ?: tail.takeIf { it.isNotBlank() } ?: "펫")
        }.trim().ifBlank { "에이몽" }
    }

    fun stageEmoji(stage: String): String = when (stage.uppercase()) {
        "EGG" -> "🥚"
        "HATCH", "BABY" -> "🐣"
        "GROWTH" -> "🐣"
        "AIMONG" -> "💫"
        else -> "✨"
    }
}
