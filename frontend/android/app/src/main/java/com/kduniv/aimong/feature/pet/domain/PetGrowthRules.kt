package com.kduniv.aimong.feature.pet.domain

/**
 * BE·기능 명세 5-2 기준 pet.xp 성장 구간 (등급과 무관).
 *
 * - 알(EGG): 0 ≤ xp < 80
 * - 성장(GROWTH): 80 ≤ xp < 250
 * - 아이몽(AIMONG): xp ≥ 250 → 달성 후 서버가 xp=0 리셋·stage=AIMONG 유지
 */
object PetGrowthRules {

    const val EGG_EVOLUTION_XP = 80
    const val GROWTH_EVOLUTION_XP = 250

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
            "AIMONG", "ADULT", "MATURE", "FINAL" -> PetStage.AIMONG
            else -> PetStage.EGG
        }

    /** 현재 단계에서 다음 진화까지 필요한 누적 XP. 아이몽(최종)이면 null */
    fun evolutionThresholdXp(grade: String, stage: String): Int? =
        evolutionThresholdXp(normalizeStage(stage))

    fun evolutionThresholdXp(stage: PetStage): Int? =
        when (stage) {
            PetStage.EGG -> EGG_EVOLUTION_XP
            PetStage.GROWTH -> GROWTH_EVOLUTION_XP
            PetStage.AIMONG -> null
        }

    fun displayStageLevel(stage: String): Int =
        displayStageLevel(normalizeStage(stage))

    fun displayStageLevel(stage: PetStage): Int =
        when (stage) {
            PetStage.EGG -> 1
            PetStage.GROWTH -> 2
            PetStage.AIMONG -> 3
        }

    fun displayStageLevel(stage: String, xp: Int): Int =
        displayStageLevel(resolveEffectiveStage(stage, xp))

    /**
     * UI·도감 표시용 단계.
     * - AIMONG 은 [stage] 정본 (달성 후 xp=0 리셋)
     * - 그 외는 pet.xp 구간(80/250) 기준 — BE stage가 xp보다 앞서 있는 비정상 응답도 보정
     */
    fun resolveEffectiveStage(stage: String, xp: Int): PetStage {
        if (normalizeStage(stage) == PetStage.AIMONG) return PetStage.AIMONG
        val safeXp = xp.coerceAtLeast(0)
        return when {
            safeXp >= GROWTH_EVOLUTION_XP -> PetStage.AIMONG
            safeXp >= EGG_EVOLUTION_XP -> PetStage.GROWTH
            else -> PetStage.EGG
        }
    }

    fun resolveEffectiveStageString(stage: String, xp: Int): String =
        when (resolveEffectiveStage(stage, xp)) {
            PetStage.EGG -> "EGG"
            PetStage.GROWTH -> "GROWTH"
            PetStage.AIMONG -> "AIMONG"
        }

    /** 홈·펫 시트 XP 바 표시 여부 — 아이몽은 성장 종료 */
    fun showsXpProgress(stage: String): Boolean =
        showsXpProgress(stage, xp = 0)

    fun showsXpProgress(stage: String, xp: Int): Boolean =
        resolveEffectiveStage(stage, xp) != PetStage.AIMONG

    /** 홈 XP 프로그레스 분모 (현재 단계 진화 임계값) */
    fun progressMaxXp(grade: String, stage: String, currentXp: Int = 0): Int? =
        evolutionThresholdXp(resolveEffectiveStage(stage, currentXp))
}
