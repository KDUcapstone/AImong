package com.kduniv.aimong.feature.home.presentation

import com.kduniv.aimong.feature.mission.domain.model.MissionStarLevel

/** 난이도 피커에서 별 단계를 열어 둘 때 기준 — 신규 학습 vs 복습 */
enum class DifficultyUnlockMode {
    NEW_PLAY,
    REVIEW,
    /** 별마다 isPlayable(신규) / isReviewable(복습)을 따로 판단 — 완료(⭐) 노드용 */
    PER_STAR,
}

/** 난이도 선택 시 퀴즈 진입 검증 모드 — 미완료 세트 우선 신규, 없으면 복습 */
fun MissionStarLevel.resolveUnlockModeForPick(): DifficultyUnlockMode =
    if (isPlayable) DifficultyUnlockMode.NEW_PLAY else DifficultyUnlockMode.REVIEW
