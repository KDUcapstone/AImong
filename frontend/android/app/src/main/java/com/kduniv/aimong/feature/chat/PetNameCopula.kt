package com.kduniv.aimong.feature.chat

/**
 * 「나는 {이름}야 / {이름}이야」 — 받침 유무에 따른 구어체 연결.
 * 도감 [GachaPetCatalog.Entry.copulaYaOverride]로 펫별 예외 가능.
 */
object PetNameCopula {

    /** 이름 뒤에 붙는 「야」/「이야」(이름 본체는 제외) */
    fun suffixFor(displayName: String): String {
        val name = displayName.trim()
        if (name.isEmpty()) return "야"
        return if (name.last().hasHangulBatchim()) "이야" else "야"
    }

    /** 인사말용 전체 호칭 — 예) 폭풍매야, 밤송이햄이야 */
    fun introName(displayName: String, overrideSuffix: String? = null): String {
        val name = displayName.trim().ifBlank { return "에이몽이야" }
        val suffix = overrideSuffix?.takeIf { it == "야" || it == "이야" } ?: suffixFor(name)
        return name + suffix
    }

    private fun Char.hasHangulBatchim(): Boolean {
        if (this !in '\uAC00'..'\uD7A3') return false
        return (code - 0xAC00) % 28 != 0
    }
}
