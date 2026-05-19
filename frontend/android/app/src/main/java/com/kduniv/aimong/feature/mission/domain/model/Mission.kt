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
}

/**
 * 홈 미션 노드 아래 ★ 채움(0~3): [isPlayable]인 최고 난이도까지.
 * 별 단계에는 isUnlocked 없음 — 소단원 [Mission.isUnlocked]·복습 [isReviewable]과 별개.
 * 예) ★1·★2 playable → 2 → `★★☆`
 */
fun List<MissionStarLevel>.openDifficultyCount(): Int =
    filter { it.isPlayable }
        .maxOfOrNull { it.starLevel }
        ?.coerceIn(0, 3) ?: 0

fun Mission.openDifficultyCount(): Int = starLevels.openDifficultyCount()

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
