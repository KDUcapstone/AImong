package com.kduniv.aimong.feature.quiz.domain.model

/** submit 응답 + 세션 플래그를 결과 화면·XP 바에 맞게 정리 */
object QuizResultMapper {

    fun enrich(submit: QuizResult, sessionIsReview: Boolean): QuizResult {
        val review = submit.mode == "review" || sessionIsReview
        val xp = resolveXpEarned(submit)
        val barXp = resolveProgressXp(submit, xp)
        val barMax = submit.nextLevelXp.takeIf { it > 0 } ?: 100
        return submit.copy(
            mode = if (review) "review" else submit.mode,
            xpEarned = xp,
            currentXp = barXp,
            nextLevelXp = barMax,
        )
    }

    private fun resolveXpEarned(result: QuizResult): Int {
        if (result.xpEarned > 0) return result.xpEarned
        val fromRewards = result.rewards
            .filter { it.type.equals("EXP", ignoreCase = true) }
            .sumOf { it.count }
        return fromRewards.coerceAtLeast(0)
    }

    /** 서버가 currentXp를 안 줄 때도 획득 EXP 애니메이션이 보이도록 */
    private fun resolveProgressXp(result: QuizResult, gained: Int): Int {
        if (result.currentXp > 0) return result.currentXp
        if (gained > 0) return gained
        return 0
    }
}
