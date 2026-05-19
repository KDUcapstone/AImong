package com.kduniv.aimong.feature.mission.domain.model

/** 소단원(미션) + 별 난이도 3단 — v2.3 */
data class Mission(
    val missionId: String,
    val missionCode: String,
    val stage: Int,
    val title: String,
    val description: String,
    val isUnlocked: Boolean,
    val starLevels: List<MissionStarLevel>
)

data class MissionStarLevel(
    val starLevel: Int,
    val label: String,
    val totalSetCount: Int,
    val completedSetCount: Int,
    val isPlayable: Boolean,
    val isReviewable: Boolean
) {
    val isCompleted: Boolean
        get() = totalSetCount > 0 && completedSetCount >= totalSetCount

    /** 신규 세트 진행 가능 — 복습 전용과 구분 */
    val isReviewOnly: Boolean
        get() = isReviewable && !isPlayable
}

fun List<MissionStarLevel>.hasNewPlayable(): Boolean =
    any { it.isPlayable }

/** 홈 경로 되감기 노드: 신규(isPlayable) 없이 복습만 가능할 때 */
fun Mission.showsReviewPathNode(): Boolean {
    val stars = starLevels.normalizeToThreeLevels()
    return !stars.hasNewPlayable() && stars.any { it.isReviewOnly }
}

/**
 * 홈 미션 노드 아래 ★ 채움(0~3): [isPlayable]인 최고 난이도 번호까지 채움.
 * 예) ★2 playable → 2개 → `★★☆` (보통까지 해금·피커에서도 1~2 동일 기준)
 */
fun List<MissionStarLevel>.openDifficultyCount(): Int =
    normalizeToThreeLevels()
        .filter { it.isPlayable }
        .maxOfOrNull { it.starLevel }
        ?.coerceIn(0, 3) ?: 0

fun Mission.openDifficultyCount(): Int = starLevels.openDifficultyCount()

/** GET /missions·status 응답이 1~2개만 올 때도 피커·★ 표시를 맞추기 위해 3단 보정 */
fun List<MissionStarLevel>.normalizeToThreeLevels(): List<MissionStarLevel> {
    val byLevel = associateBy { it.starLevel }
    return (1..3).map { level ->
        byLevel[level] ?: defaultStarLevel(level)
    }
}

fun List<MissionStarLevel>.mergePreservingHigherUnlock(other: List<MissionStarLevel>): List<MissionStarLevel> {
    val left = normalizeToThreeLevels()
    val right = other.normalizeToThreeLevels()
    return (1..3).map { level ->
        val a = left.first { it.starLevel == level }
        val b = right.first { it.starLevel == level }
        a.copy(
            label = b.label.ifBlank { a.label },
            totalSetCount = maxOf(a.totalSetCount, b.totalSetCount),
            completedSetCount = maxOf(a.completedSetCount, b.completedSetCount),
            isPlayable = a.isPlayable || b.isPlayable,
            isReviewable = a.isReviewable || b.isReviewable,
        )
    }
}

private fun defaultStarLevel(level: Int): MissionStarLevel = MissionStarLevel(
    starLevel = level,
    label = when (level) {
        1 -> "쉬움"
        2 -> "보통"
        else -> "어려움"
    },
    totalSetCount = 0,
    completedSetCount = 0,
    isPlayable = false,
    isReviewable = false,
)

/** 스테이지 진행·완료 배지 등: 세트까지 전부 깬 난이도 개수 */
fun List<MissionStarLevel>.completedDifficultyCount(): Int =
    count { it.isCompleted }.coerceIn(0, 3)

fun Mission.completedDifficultyCount(): Int = starLevels.completedDifficultyCount()

/** v2.11: 스테이지 해금·섬 진행률은 별 1(쉬움) 세트 완료 기준 */
fun Mission.star1Level(): MissionStarLevel? =
    starLevels.firstOrNull { it.starLevel == 1 }

fun Mission.isStar1Completed(): Boolean =
    star1Level()?.isCompleted == true

fun Mission.hasActiveStar1(): Boolean =
    star1Level()?.totalSetCount?.let { it > 0 } == true

/** API title에 붙은 missionCode·S1-M10 등 접미사 제거 */
fun Mission.displayTitle(): String = title.toDisplayMissionTitle(missionCode)

fun String.toDisplayMissionTitle(missionCode: String = ""): String {
    var t = trim()
    val code = missionCode.trim()
    if (code.isNotBlank() && t.endsWith(code, ignoreCase = true)) {
        t = t.removeSuffix(code).trim()
    }
    t = MISSION_CODE_SUFFIX_REGEX.replace(t, "").trim()
    return t.ifBlank { code.ifBlank { trim() } }
}

private val MISSION_CODE_SUFFIX_REGEX =
    Regex("""\s*S\d+-?M\d+\s*$""", RegexOption.IGNORE_CASE)

data class MissionProgress(
    val completedSetCount: Int,
    val totalSetCount: Int
)
