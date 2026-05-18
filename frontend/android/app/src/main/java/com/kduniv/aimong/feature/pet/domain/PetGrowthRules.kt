package com.kduniv.aimong.feature.pet.domain

/**
 * BE PetService.getEvolutionThreshold / addXpAndEvolve 기준.
 * EGG→GROWTH, GROWTH→AIMONG 진화 시 XP는 0으로 리셋되며, 홈 바 분모는 현재 단계 임계값이다.
 */
object PetGrowthRules {

    enum class PetGrade { NORMAL, RARE, EPIC, LEGEND }

    enum class PetStage { EGG, GROWTH, AIMONG }

    fun normalizeGrade(grade: String): PetGrade =
        when (grade.uppercase()) {
            "RARE" -> PetGrade.RARE
            "EPIC" -> PetGrade.EPIC
            "LEGEND", "LEGENDARY" -> PetGrade.LEGEND
            "COMMON", "NORMAL" -> PetGrade.NORMAL
            else -> PetGrade.NORMAL
        }

    fun normalizeStage(stage: String): PetStage =
        when (stage.uppercase()) {
            "EGG" -> PetStage.EGG
            "GROWTH", "HATCH", "BABY" -> PetStage.GROWTH
            "AIMONG", "ADULT", "MATURE" -> PetStage.AIMONG
            else -> PetStage.EGG
        }

    /** 다음 진화에 필요한 누적 XP. AIMONG(최종)이면 null */
    fun evolutionThresholdXp(grade: String, stage: String): Int? =
        when (normalizeStage(stage)) {
            PetStage.EGG -> when (normalizeGrade(grade)) {
                PetGrade.NORMAL -> 10
                PetGrade.RARE -> 12
                PetGrade.EPIC -> 15
                PetGrade.LEGEND -> 20
            }
            PetStage.GROWTH -> when (normalizeGrade(grade)) {
                PetGrade.NORMAL -> 30
                PetGrade.RARE -> 36
                PetGrade.EPIC -> 45
                PetGrade.LEGEND -> 60
            }
            PetStage.AIMONG -> null
        }

    fun displayStageLevel(stage: String): Int =
        when (normalizeStage(stage)) {
            PetStage.EGG -> 1
            PetStage.GROWTH -> 2
            PetStage.AIMONG -> 3
        }

    /** 홈 XP 프로그레스 분모 — 서버 [xp]는 그대로, 상한으로 자르지 않음 */
    fun progressMaxXp(grade: String, stage: String, currentXp: Int): Int =
        evolutionThresholdXp(grade, stage) ?: currentXp.coerceAtLeast(1)
}
