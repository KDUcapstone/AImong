package com.kduniv.aimong.feature.quiz.domain.model

/** 개별 문항 난이도 (API v1.5 QuestionDifficulty). */
enum class QuestionDifficulty {
    LOW,
    MEDIUM,
    HIGH;

    companion object {
        fun parse(raw: String?): QuestionDifficulty? {
            if (raw.isNullOrBlank()) return null
            return entries.firstOrNull { it.name == raw.trim().uppercase() }
        }
    }
}

/** 시도 상태 (API v1.5 AttemptStatus). */
enum class AttemptStatus {
    IN_PROGRESS,
    SUBMITTED,
    EXPIRED,
    ABANDONED;

    companion object {
        fun parse(raw: String?): AttemptStatus? {
            if (raw.isNullOrBlank()) return null
            val n = raw.trim().uppercase().replace('-', '_')
            return entries.firstOrNull { it.name == n }
        }
    }
}

/** 보상 유형 (API v1.5 RewardType). 레거시 `TICKET`, `XP` 등은 [normalizeRewardType]에서 처리. */
enum class RewardType {
    COIN,
    EXP,
    PET_FRAGMENT,
    ITEM;

    companion object {
        fun parse(raw: String?): RewardType? {
            if (raw.isNullOrBlank()) return null
            val n = raw.trim().uppercase()
            if (n == "XP") return EXP
            return entries.firstOrNull { it.name == n }
        }
    }
}

/** 서버 문자열을 명세 형태(대문자+언더스코어)로 맞춘다. */
fun normalizeAttemptStatus(raw: String?): String {
    if (raw.isNullOrBlank()) return AttemptStatus.SUBMITTED.name
    val n = raw.trim().uppercase().replace('-', '_')
    AttemptStatus.parse(n)?.let { return it.name }
    return n
}

/** 보상 `type` 문자열 정규화 (예: XP → EXP). */
fun normalizeRewardType(raw: String?): String {
    if (raw.isNullOrBlank()) return RewardType.COIN.name
    val n = raw.trim().uppercase()
    RewardType.parse(n)?.let { return it.name }
    return n
}
