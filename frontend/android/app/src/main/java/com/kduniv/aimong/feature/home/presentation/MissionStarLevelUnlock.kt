package com.kduniv.aimong.feature.home.presentation

import com.kduniv.aimong.feature.mission.domain.model.MissionStarLevel
import com.kduniv.aimong.feature.mission.domain.model.openDifficultyCount

/**
 * 노드 아래 ★([openDifficultyCount])과 난이도 피커 잠금을 동일 기준으로 맞춘다.
 * 최고 [isPlayable] 단계가 N이면 1~N 구간에서 각 모드별 선택 가능 여부를 판단한다.
 */
fun List<MissionStarLevel>.isPickerUnlocked(starLevel: Int, mode: DifficultyUnlockMode): Boolean {
    val maxOpen = openDifficultyCount()
    if (maxOpen <= 0 || starLevel !in 1..maxOpen) return false
    val sl = firstOrNull { it.starLevel == starLevel } ?: return false
    return when (mode) {
        DifficultyUnlockMode.NEW_PLAY -> sl.isPlayable
        DifficultyUnlockMode.REVIEW -> sl.isReviewOnly
        DifficultyUnlockMode.PER_STAR -> sl.isPlayable || sl.isReviewOnly
    }
}
