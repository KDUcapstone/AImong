package com.kduniv.aimong.feature.quiz.domain.model

/** submit 응답 + 세션 플래그를 결과 화면·XP 바에 맞게 정리 */
object QuizResultMapper {

    fun enrich(submit: QuizResult, sessionIsReview: Boolean): QuizResult {
        val review = submit.mode == "review" || sessionIsReview
        val xp = MissionXpRules.resolveDisplayedXpEarned(submit, review)
        val barXp = resolveProgressXp(submit, xp)
        val barMax = submit.nextLevelXp.takeIf { it > 0 } ?: 100
        return submit.copy(
            mode = if (review) "review" else submit.mode,
            progressApplied = if (review) false else submit.progressApplied,
            xpEarned = xp,
            bonusXp = if (review || !submit.isPassed) 0 else submit.bonusXp,
            currentXp = barXp,
            nextLevelXp = barMax,
        )
    }

    /** 서버가 currentXp를 안 줄 때도 획득 EXP 애니메이션이 보이도록 */
    private fun resolveProgressXp(result: QuizResult, gained: Int): Int {
        if (result.currentXp > 0) return result.currentXp
        if (gained > 0) return gained
        return 0
    }
}
