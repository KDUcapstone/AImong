package com.kduniv.aimong.feature.quiz.domain.model

import kotlin.math.floor

/**
 * 미션 퀴즈 XP 정책 (서버 [SubmitService]·기능 명세 v3.9 기준).
 *
 * - 별 1/2/3 난이도별 XP 차등 없음
 * - 일반 통과: 기본 [BASE_PASS_XP]
 * - 전부 정답(퍼펙트): [PERFECT_BONUS_XP] 추가
 * - 펫 등급 보너스: [petBonusXp] (응답 `bonusXp`와 동일 개념)
 * - 공동 스트릭(파트너 당일 완료): floor(합계 × 1.5)
 * - 복습·실패: 0
 */
object MissionXpRules {

    const val BASE_PASS_XP = 10
    const val PERFECT_BONUS_XP = 10
    private const val STREAK_MULTIPLIER = 1.5

    /** 결과 화면·홈 힌트에 쓸 최종 획득 XP (서버 값 우선, 복습/실패는 0). */
    fun resolveDisplayedXpEarned(result: QuizResult, sessionIsReview: Boolean): Int {
        if (sessionIsReview || result.mode == "review") return 0
        if (!result.isPassed) return 0
        if (!result.progressApplied) return 0
        if (result.xpEarned > 0) return result.xpEarned
        return result.rewards
            .filter { it.type.equals("EXP", ignoreCase = true) }
            .sumOf { it.count }
            .coerceAtLeast(0)
    }

    /** 목업 제출 등 클라이언트 계산이 필요할 때만 사용. */
    fun computeEarnedXp(
        isReview: Boolean,
        isPassed: Boolean,
        wrongCount: Int,
        isPerfect: Boolean,
        equippedPetGrade: String?,
        streakBonusApplied: Boolean,
    ): Int {
        if (isReview || !isPassed) return 0
        val subtotal = BASE_PASS_XP +
            petBonusXp(equippedPetGrade, wrongCount) +
            if (isPerfect) PERFECT_BONUS_XP else 0
        return if (streakBonusApplied) {
            floor(subtotal * STREAK_MULTIPLIER).toInt()
        } else {
            subtotal
        }
    }

    /** 서버 `calculatePetBonusXp`와 동일. */
    fun petBonusXp(equippedPetGrade: String?, wrongCount: Int): Int {
        return when (equippedPetGrade?.uppercase()) {
            "NORMAL" -> if (wrongCount == 0) 10 else 0
            "RARE" -> if (wrongCount <= 1) 10 else 0
            "EPIC" -> if (wrongCount <= 2) 10 else 0
            "LEGEND" -> if (wrongCount <= 2) 15 else 0
            else -> 0
        }
    }
}
