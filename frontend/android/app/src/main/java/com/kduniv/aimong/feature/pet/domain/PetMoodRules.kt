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
        if (todaySetCount > 0 || todayCompletedCount > 0) return HomeState.HAPPY

        val inferred = inferHomeStateFromStreak(lastCompletedDate, serverDate)
        val fromMood = homeStateFromMood(mood) ?: return inferred

        // lastCompletedDate 미전달·신규 등으로 추정이 IDLE인데 서버만 SAD_* 인 경우 회색 과적용 방지
        if (fromMood == HomeState.SAD_DEEP && inferred == HomeState.IDLE) return HomeState.IDLE
        if (fromMood == HomeState.SAD_LIGHT && inferred == HomeState.IDLE) return HomeState.IDLE

        return fromMood
    }

    private fun inferHomeStateFromStreak(
        lastCompletedDate: String?,
        serverDate: String?,
    ): HomeState = when (daysSinceLastMission(lastCompletedDate, serverDate)) {
        null -> HomeState.IDLE
        0 -> HomeState.HAPPY
        1 -> HomeState.SAD_LIGHT
        else -> HomeState.SAD_DEEP
    }

    fun homeStateFromMood(mood: String?): HomeState? =
        when (mood?.trim()?.uppercase()) {
            "HAPPY" -> HomeState.HAPPY
            "IDLE", "NORMAL", "NEUTRAL" -> HomeState.IDLE
            "SAD_LIGHT", "SAD" -> HomeState.SAD_LIGHT
            "SAD_DEEP" -> HomeState.SAD_DEEP
            else -> null
        }

    /** KST 기준: serverDate − lastCompletedDate (일). 이력 없음·날짜 파싱 실패 시 null. */
    fun daysSinceLastMission(lastCompletedDate: String?, serverDate: String?): Int? {
        val today = parseKstLocalDate(serverDate) ?: return null
        val last = parseKstLocalDate(lastCompletedDate) ?: return null
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
