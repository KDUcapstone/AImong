package com.kduniv.aimong.feature.quiz.presentation

/**
 * 미션 클리어 등급 — 세션 동안 소모한 하트 수 기준.
 * - 0개: PERFECT
 * - 1~2개: SUCCESS
 * - 3개 이상: FAIL
 */
enum class MissionHeartGrade {
    PERFECT,
    SUCCESS,
    FAIL;

    companion object {
        fun fromHeartsLost(heartsLost: Int): MissionHeartGrade = when {
            heartsLost >= 3 -> FAIL
            heartsLost >= 1 -> SUCCESS
            else -> PERFECT
        }
    }
}
