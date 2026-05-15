package com.kduniv.aimong.feature.chat

/** 홈·챗봇에서 공통으로 쓰는 장착 펫 표시 이름·이모지 */
object ChatPetUiHelper {

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
        "GROWTH" -> "✨"
        else -> "🌟"
    }
}
