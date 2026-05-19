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
        Entry("pet_normal_001", "몽실토끼", "NORMAL", "🐰"),
        Entry("pet_normal_002", "방울펭귄", "NORMAL", "🐧"),
        Entry("pet_normal_003", "잎새여우", "NORMAL", "🦊"),
        Entry("pet_normal_004", "젤리곰", "NORMAL", "🐻"),
        Entry("pet_normal_005", "별콩새", "NORMAL", "🐤"),
        Entry("pet_normal_006", "조개물개", "NORMAL", "🦭"),
        Entry("pet_normal_007", "밤송이햄", "NORMAL", "🐹"),
        Entry("pet_normal_008", "바람다람", "NORMAL", "🐿️"),
        Entry("pet_normal_009", "달빛냥", "NORMAL", "🐱"),
        Entry("pet_normal_010", "꽃사슴", "NORMAL", "🦌"),
        Entry("pet_rare_001", "번개람쥐", "RARE", "⚡"),
        Entry("pet_rare_002", "눈꽃여우", "RARE", "❄️"),
        Entry("pet_rare_003", "수정사슴", "RARE", "💎"),
        Entry("pet_rare_004", "구름양", "RARE", "☁️"),
        Entry("pet_rare_005", "해초용", "RARE", "🐉"),
        Entry("pet_rare_006", "그림자냥", "RARE", "🌙"),
        Entry("pet_epic_001", "화염늑대", "EPIC", "🔥"),
        Entry("pet_epic_002", "폭풍매", "EPIC", "🦅"),
        Entry("pet_epic_003", "흑요호랑", "EPIC", "🐯"),
        Entry("pet_epic_004", "루미드래곤", "EPIC", "✨"),
        Entry("pet_legend_001", "태양봉황", "LEGEND", "☀️"),
        Entry("pet_legend_002", "월광기린", "LEGEND", "🌙"),
    )
}
