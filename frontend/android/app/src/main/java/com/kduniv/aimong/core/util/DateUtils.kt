package com.kduniv.aimong.core.util

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * API v1.5: 저장 시각은 UTC(ISO-8601), 화면 표시는 사용자 로컬 타임존 권장.
 */
object DateUtils {

    private val displayFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.KOREA)

    /** ISO-8601 UTC 문자열(예: `2026-05-10T09:00:00Z`)을 [Instant]로 파싱. 실패 시 null. */
    fun parseIsoUtc(iso: String?): Instant? {
        val s = iso?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return try {
            Instant.parse(s)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    /** [Instant]를 기본 로컬([ZoneId.systemDefault]) 기준으로 짧게 표시. */
    fun formatForLocalDisplay(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
        ZonedDateTime.ofInstant(instant, zone).format(displayFormatter)

    /** ISO UTC 문자열을 로컬 표시 문자열로 변환. 파싱 실패 시 원문을 그대로 반환. */
    fun formatIsoUtcForLocal(iso: String?, zone: ZoneId = ZoneId.systemDefault()): String {
        val instant = parseIsoUtc(iso) ?: return iso?.trim().orEmpty()
        return formatForLocalDisplay(instant, zone)
    }
}
