package com.kduniv.aimong.feature.quiz.domain.model

import com.kduniv.aimong.feature.pet.domain.PetGrowthRules

/** submit 응답 + 세션 플래그를 결과 화면·펫 XP 바에 맞게 정리 */
object QuizResultMapper {

    fun enrich(submit: QuizResult, sessionIsReview: Boolean): QuizResult {
        val review = submit.mode == "review" || sessionIsReview
        val xp = MissionXpRules.resolveDisplayedXpEarned(submit, review)
        val petXp = resolvePetProgressXp(submit, xp)
        val petStage = PetGrowthRules.resolveEffectiveStageString(
            submit.petStage ?: "EGG",
            petXp,
        )
        val petMaxXp = (
            PetGrowthRules.progressMaxXp(submit.equippedPetGrade ?: "NORMAL", petStage, petXp)
                ?: PetGrowthRules.EGG_EVOLUTION_XP
            ).coerceAtLeast(1)
        val petLevel = PetGrowthRules.displayStageLevel(petStage, petXp)
        return submit.copy(
            mode = if (review) "review" else submit.mode,
            progressApplied = if (review) false else submit.progressApplied,
            xpEarned = xp,
            bonusXp = if (review || !submit.isPassed) 0 else submit.bonusXp,
            petStage = petStage,
            currentXp = petXp,
            nextLevelXp = petMaxXp,
            currentLevel = petLevel,
        )
    }

    /** 장착 펫 누적 XP — 서버 equippedPetXp 우선 */
    private fun resolvePetProgressXp(result: QuizResult, gained: Int): Int {
        if (result.currentXp > 0) return result.currentXp
        if (gained > 0) return gained
        return 0
    }
}
