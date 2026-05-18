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

    /** 플레이·복습·클리어 중 하나라도 가능한 난이도면 열린 것으로 본다 */
    val isOpen: Boolean
        get() = isPlayable || isReviewable || isCompleted
}

/** 홈 경로 노드 아래 ★ 표시(0~3): 열린 최고 난이도 단계 (난이도 피커 등) */
fun List<MissionStarLevel>.openDifficultyCount(): Int =
    filter { it.isOpen }
        .maxOfOrNull { it.starLevel }
        ?.coerceIn(0, 3) ?: 0

fun Mission.openDifficultyCount(): Int = starLevels.openDifficultyCount()

/** 홈 경로 노드 아래 ★ 표시(0~3): 클리어한 난이도 개수 */
fun List<MissionStarLevel>.completedDifficultyCount(): Int =
    count { it.isCompleted }.coerceIn(0, 3)

fun Mission.completedDifficultyCount(): Int = starLevels.completedDifficultyCount()

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
