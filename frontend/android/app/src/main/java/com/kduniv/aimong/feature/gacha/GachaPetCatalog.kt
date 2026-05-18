package com.kduniv.aimong.feature.gacha

/**
 * 도감 전체 22종 (서버 pet_type 코드 기준).
 * API 전용 목록이 생기면 이 정적 목록을 교체한다.
 */
object GachaPetCatalog {

    const val TOTAL = 22

    data class Entry(
        val petType: String,
        val displayName: String,
        val grade: String,
        val emoji: String,
    )

    /** GET /pet 의 petType 과 도감 행 매칭 (대소문자·공백 무시) */
    fun entryFor(petType: String?): Entry? {
        if (petType.isNullOrBlank()) return null
        val key = normalizePetTypeKey(petType)
        return entries.firstOrNull { normalizePetTypeKey(it.petType) == key }
    }

    fun displayNameFor(petType: String, grade: String = "NORMAL"): String =
        entryFor(petType)?.displayName ?: fallbackDisplayName(petType, grade)

    fun emojiFor(petType: String, grade: String = "NORMAL"): String =
        entryFor(petType)?.emoji ?: fallbackEmoji(grade)

    private fun normalizePetTypeKey(petType: String): String =
        petType.trim().lowercase().replace('-', '_')

    private fun fallbackDisplayName(petType: String, grade: String): String {
        val tail = petType.substringAfterLast('_', "").filter { it.isDigit() }
        val gradeLabel = when (grade.uppercase()) {
            "RARE" -> "레어"
            "EPIC" -> "에픽"
            "LEGEND", "LEGENDARY" -> "레전드"
            else -> "커먼"
        }
        return if (tail.isNotBlank()) "$gradeLabel $tail" else petType
    }

    private fun fallbackEmoji(grade: String): String = when (grade.uppercase()) {
        "RARE" -> "💎"
        "EPIC" -> "👑"
        "LEGEND", "LEGENDARY" -> "🌟"
        else -> "⭐"
    }

    val entries: List<Entry> = listOf(
        Entry("pet_normal_001", "반짝이", "NORMAL", "⭐"),
        Entry("pet_normal_002", "구름이", "NORMAL", "☁️"),
        Entry("pet_normal_003", "태양이", "NORMAL", "☀️"),
        Entry("pet_normal_004", "달이", "NORMAL", "🌙"),
        Entry("pet_normal_005", "로봇이", "NORMAL", "🤖"),
        Entry("pet_normal_006", "번개", "NORMAL", "⚡"),
        Entry("pet_normal_007", "불꽃이", "NORMAL", "🔥"),
        Entry("pet_normal_008", "바람이", "NORMAL", "💨"),
        Entry("pet_normal_009", "물방울", "NORMAL", "💧"),
        Entry("pet_normal_010", "풀잎이", "NORMAL", "🌿"),
        Entry("pet_rare_001", "수정이", "RARE", "💎"),
        Entry("pet_rare_002", "파도", "RARE", "🌊"),
        Entry("pet_rare_003", "반짝봇", "RARE", "🛸"),
        Entry("pet_rare_004", "무지개", "RARE", "🌈"),
        Entry("pet_rare_005", "별빛", "RARE", "✨"),
        Entry("pet_rare_006", "천둥", "RARE", "⛈️"),
        Entry("pet_epic_001", "불꽃왕", "EPIC", "👑"),
        Entry("pet_epic_002", "우주냥", "EPIC", "🪐"),
        Entry("pet_epic_003", "번개신", "EPIC", "🌩️"),
        Entry("pet_epic_004", "심해", "EPIC", "🐙"),
        Entry("pet_legend_001", "전설이", "LEGEND", "🌟"),
        Entry("pet_legend_002", "아이몽", "LEGEND", "💫"),
    )
}
