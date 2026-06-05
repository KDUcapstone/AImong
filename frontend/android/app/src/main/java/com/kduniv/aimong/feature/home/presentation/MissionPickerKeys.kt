package com.kduniv.aimong.feature.home.presentation

/** 난이도 피커 토글·중복 방지용 미션 식별 키 */
fun HomeQuizNavigation.difficultyPickerMissionKey(): String = when {
    missionId.isNotBlank() -> "mission:$missionId"
    entrySetId.isNotBlank() -> "set:$entrySetId"
    else -> ""
}
