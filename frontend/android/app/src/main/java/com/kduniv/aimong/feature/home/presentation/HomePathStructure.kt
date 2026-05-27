package com.kduniv.aimong.feature.home.presentation

/** 미션 경로 레이아웃 구조만 비교(별 개수 변경은 제외) — 부분 갱신용 */
fun List<HomePathItem>.pathStructureKey(): String =
    joinToString(separator = "|") { it.pathStructureToken() }

private fun HomePathItem.pathStructureToken(): String = when (this) {
    is HomePathItem.SectionHeader -> "H:$stage"
    is HomePathItem.InterStageRewardChest -> "Chest:${afterStageNumber}"
    is HomePathItem.Completed -> "C:$missionId:${quizNav.entrySetId}:${quizNav.starLevel}"
    is HomePathItem.TodayStart -> "T:${quizNav.missionId}:${quizNav.entrySetId}:${quizNav.starLevel}:$enabled"
    is HomePathItem.Start -> "S:${quizNav.missionId}:${quizNav.entrySetId}:${quizNav.starLevel}:$enabled"
    is HomePathItem.Review -> "R:${quizNav.missionId}:${quizNav.entrySetId}:${quizNav.starLevel}"
    is HomePathItem.Locked -> "L:${hint.hashCode()}"
}

fun HomePathItem.missionIdForPath(): String? = when (this) {
    is HomePathItem.Completed -> missionId.takeIf { it.isNotBlank() }
    is HomePathItem.TodayStart -> quizNav.missionId.takeIf { it.isNotBlank() }
    is HomePathItem.Start -> quizNav.missionId.takeIf { it.isNotBlank() }
    is HomePathItem.Review -> quizNav.missionId.takeIf { it.isNotBlank() }
    else -> null
}
