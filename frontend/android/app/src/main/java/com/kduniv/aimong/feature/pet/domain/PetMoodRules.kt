package com.kduniv.aimong.feature.pet.domain

import com.kduniv.aimong.core.util.DateUtils
import com.kduniv.aimong.feature.home.presentation.HomeState
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * 기능 명세 5-4 — 펫 슬픔(mood) → 홈 [HomeState].
 * 서버 `pets.mood`가 있으면 우선, 없으면 streak·미션 요약으로 추정.
 */
object PetMoodRules {

    private val kst = ZoneId.of("Asia/Seoul")

    fun resolveHomeState(
        mood: String?,
        todaySetCount: Int,
        todayCompletedCount: Int,
        lastCompletedDate: String?,
        serverDate: String?,
    ): HomeState {
        homeStateFromMood(mood)?.let { return it }
        if (todaySetCount > 0 || todayCompletedCount > 0) return HomeState.HAPPY
        return when (daysSinceLastMission(lastCompletedDate, serverDate)) {
            null -> HomeState.IDLE
            0 -> HomeState.HAPPY
            1 -> HomeState.SAD_LIGHT
            else -> HomeState.SAD_DEEP
        }
    }

    fun homeStateFromMood(mood: String?): HomeState? =
        when (mood?.trim()?.uppercase()) {
            "HAPPY" -> HomeState.HAPPY
            "IDLE" -> HomeState.IDLE
            "SAD_LIGHT" -> HomeState.SAD_LIGHT
            "SAD_DEEP" -> HomeState.SAD_DEEP
            else -> null
        }

    /** KST 기준: serverDate − lastCompletedDate (일). 미완료 이력 없으면 null이 아닌 큰 값으로 SAD_DEEP. */
    fun daysSinceLastMission(lastCompletedDate: String?, serverDate: String?): Int? {
        val today = parseKstLocalDate(serverDate) ?: return null
        val last = parseKstLocalDate(lastCompletedDate) ?: return Int.MAX_VALUE
        return ChronoUnit.DAYS.between(last, today).toInt().coerceAtLeast(0)
    }

    private fun parseKstLocalDate(raw: String?): LocalDate? {
        val s = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (s.length >= 10) {
            runCatching { return LocalDate.parse(s.substring(0, 10)) }
                .onFailure { /* ISO fallback */ }
        }
        runCatching { return LocalDate.parse(s) }.onFailure { /* continue */ }
        val instant = DateUtils.parseIsoUtc(s) ?: return null
        return instant.atZone(kst).toLocalDate()
    }
}
