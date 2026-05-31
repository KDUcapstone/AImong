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
        /** 챗봇 「나는 {displayName}{copulaYa}」 — 예: 폭풍매야, 밤송이햄이야 */
        val copulaYa: String,
    ) {
        fun introNameForChat(): String = displayName + copulaYa
    }

    data class ExchangeTarget(
        val petType: String,
        val grade: String,
    )

    /** GET /pet 의 petType 과 도감 행 매칭 (대소문자·공백 무시) */
    fun entryFor(petType: String?): Entry? {
        if (petType.isNullOrBlank()) return null
        val key = normalizePetTypeKey(petType)
        return entries.firstOrNull { normalizePetTypeKey(it.petType) == key }
    }

    /** POST /gacha/exchange — 서버 허용 코드·등급과 도감 정의를 맞춘다 */
    fun resolveExchangeTarget(grade: String, petType: String): ExchangeTarget? {
        val entry = entryFor(petType) ?: return null
        val requestGrade = normalizeExchangeGrade(grade)
        val catalogGrade = normalizeExchangeGrade(entry.grade)
        if (requestGrade != catalogGrade) return null
        return ExchangeTarget(petType = entry.petType, grade = catalogGrade)
    }

    fun normalizePetTypeKey(petType: String): String =
        petType.trim().lowercase().replace('-', '_')

    fun displayNameFor(petType: String, grade: String = "NORMAL"): String =
        entryFor(petType)?.displayName ?: fallbackDisplayName(petType, grade)

    /** 챗봇 환영 인사 「나는 폭풍매야!」 등 */
    fun introNameForChat(petType: String, grade: String = "NORMAL"): String {
        val entry = entryFor(petType)
        if (entry != null) return entry.introNameForChat()
        val fallback = fallbackDisplayName(petType, grade)
        return com.kduniv.aimong.feature.chat.PetNameCopula.introName(fallback)
    }

    fun emojiFor(petType: String, grade: String = "NORMAL"): String =
        entryFor(petType)?.emoji ?: fallbackEmoji(grade)

    fun normalizeExchangeGrade(grade: String): String = when (grade.uppercase()) {
        "LEGENDARY" -> "LEGEND"
        else -> grade.uppercase()
    }

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
        Entry("pet_normal_001", "몽실토끼", "NORMAL", "🐰", "야"),
        Entry("pet_normal_002", "방울펭귄", "NORMAL", "🐧", "이야"),
        Entry("pet_normal_003", "잎새여우", "NORMAL", "🦊", "야"),
        Entry("pet_normal_004", "젤리곰", "NORMAL", "🐻", "이야"),
        Entry("pet_normal_005", "별콩새", "NORMAL", "🐤", "야"),
        Entry("pet_normal_006", "조개물개", "NORMAL", "🦭", "야"),
        Entry("pet_normal_007", "밤송이햄", "NORMAL", "🐹", "이야"),
        Entry("pet_normal_008", "바람다람", "NORMAL", "🐿️", "이야"),
        Entry("pet_normal_009", "달빛냥", "NORMAL", "🐱", "이야"),
        Entry("pet_normal_010", "꽃사슴", "NORMAL", "🦌", "이야"),
        Entry("pet_rare_001", "번개람쥐", "RARE", "⚡", "야"),
        Entry("pet_rare_002", "눈꽃여우", "RARE", "❄️", "야"),
        Entry("pet_rare_003", "수정사슴", "RARE", "💎", "이야"),
        Entry("pet_rare_004", "구름양", "RARE", "☁️", "이야"),
        Entry("pet_rare_005", "해초용", "RARE", "🐉", "이야"),
        Entry("pet_rare_006", "그림자냥", "RARE", "🌙", "이야"),
        Entry("pet_epic_001", "화염늑대", "EPIC", "🔥", "야"),
        Entry("pet_epic_002", "폭풍매", "EPIC", "🦅", "야"),
        Entry("pet_epic_003", "흑요호랑", "EPIC", "🐯", "이야"),
        Entry("pet_epic_004", "루미드래곤", "EPIC", "✨", "이야"),
        Entry("pet_legend_001", "태양봉황", "LEGEND", "☀️", "이야"),
        Entry("pet_legend_002", "월광기린", "LEGEND", "🌙", "이야"),
    )
}
