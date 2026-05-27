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

/** 미션 노드별 별 개수만 비교 — 구조는 같고 별만 바뀐 [bind] 스킵용 */
fun List<HomePathItem>.pathStarsKey(): String = buildString {
    for (item in this@pathStarsKey) {
        val missionId = item.missionIdForPath() ?: continue
        val stars = when (item) {
            is HomePathItem.Completed -> item.starsFilled
            is HomePathItem.TodayStart -> item.starsFilled
            is HomePathItem.Start -> item.starsFilled
            is HomePathItem.Review -> item.starsFilled
            else -> continue
        }
        append(missionId)
        append(':')
        append(stars)
        append('|')
    }
}

fun HomePathItem.missionIdForPath(): String? = when (this) {
    is HomePathItem.Completed -> missionId.takeIf { it.isNotBlank() }
    is HomePathItem.TodayStart -> quizNav.missionId.takeIf { it.isNotBlank() }
    is HomePathItem.Start -> quizNav.missionId.takeIf { it.isNotBlank() }
    is HomePathItem.Review -> quizNav.missionId.takeIf { it.isNotBlank() }
    else -> null
}
