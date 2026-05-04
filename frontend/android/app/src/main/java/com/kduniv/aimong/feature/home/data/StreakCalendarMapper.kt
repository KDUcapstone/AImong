package com.kduniv.aimong.feature.home.data

import com.kduniv.aimong.feature.home.data.model.StreakCalendarData
import com.kduniv.aimong.feature.home.domain.model.StreakCalendarResult
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

internal object StreakCalendarMapper {

    private val kst: ZoneId = ZoneId.of("Asia/Seoul")

    fun defaultYearMonthKst(): String =
        YearMonth.now(kst).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM", Locale.US))

    fun normalize(
        requestedYm: String?,
        body: StreakCalendarData
    ): StreakCalendarResult {
        val ym = body.yearMonth?.takeIf { it.matches(Regex("\\d{4}-\\d{2}")) }
            ?: requestedYm?.takeIf { it.matches(Regex("\\d{4}-\\d{2}")) }
            ?: defaultYearMonthKst()
        val dates = (body.completedDates ?: emptyList())
            .map { it.trim() }
            .filter { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
            .distinct()
            .sorted()
        val continuous = body.continuousDays?.coerceAtLeast(0) ?: 0
        val today = body.today?.trim()?.takeIf { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }
        return StreakCalendarResult(
            yearMonth = ym,
            continuousDays = continuous,
            completedDates = dates,
            today = today
        )
    }
}
