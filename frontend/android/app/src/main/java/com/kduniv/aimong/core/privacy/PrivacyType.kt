package com.kduniv.aimong.core.privacy

enum class PrivacyType {
    NAME,
    SCHOOL,
    AGE,
    GRADE,
    PHONE,
    EMAIL,
    ADDRESS,
    DATE,
    URL,
    ETC;

    /** POST /privacy/event 의 `detectedType` (ETC는 API 미지원 → AGE로 보냄). */
    fun toApiDetectedType(): String = when (this) {
        ETC, GRADE -> "AGE"
        else -> name
    }
}
