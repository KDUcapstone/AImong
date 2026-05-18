package com.kduniv.aimong.feature.home.domain

import com.kduniv.aimong.core.dev.UiMode
import com.kduniv.aimong.feature.mission.domain.model.Mission
import com.kduniv.aimong.feature.mission.domain.model.MissionStarLevel

/**
 * 목업/개발 시 스테이지마다 탭 가능한 미션이 없으면 홈 경로가 전부 「잠김·준비 중」이 됩니다.
 * [UiMode.useStubNav] 일 때 스테이지별 첫 미션에 쉬움(★1) 플레이를 보장합니다.
 */
object MissionPathDevHelper {

    fun ensureOnePlayablePerStage(missions: List<Mission>): List<Mission> {
        if (!UiMode.useStubNav || missions.isEmpty()) return missions

        val updated = missions.toMutableList()
        val indexById = updated.mapIndexed { index, m -> m.missionId to index }.toMap()

        for (stage in 1..3) {
            val stageMissions = updated
                .filter { it.stage == stage }
                .sortedBy { missionOrderKey(it.missionCode, it.title) }

            val hasPlayableNode = stageMissions.any { it.hasPlayablePathNode() }
            if (hasPlayableNode) continue

            val candidate = stageMissions.firstOrNull { it.isUnlocked }
                ?: stageMissions.firstOrNull()
                ?: continue
            val idx = indexById[candidate.missionId] ?: continue
            updated[idx] = candidate.withGuaranteedEasyPlayable()
        }
        return updated
    }

    private fun Mission.hasPlayablePathNode(): Boolean =
        isUnlocked && starLevels.any { it.isPlayable || it.isReviewable || it.isCompleted }

    private fun Mission.withGuaranteedEasyPlayable(): Mission {
        val baseStars = starLevels.ifEmpty { defaultStarLevels() }
        val patched = baseStars.map { level ->
            if (level.starLevel == 1) level.copy(isPlayable = true) else level
        }
        val withEasy = if (patched.any { it.starLevel == 1 }) {
            patched
        } else {
            listOf(MissionStarLevel(1, "쉬움", 2, 0, isPlayable = true, isReviewable = false)) + patched
        }
        return copy(isUnlocked = true, starLevels = withEasy)
    }

    private fun defaultStarLevels(): List<MissionStarLevel> = listOf(
        MissionStarLevel(1, "쉬움", 2, 0, isPlayable = true, isReviewable = false),
        MissionStarLevel(2, "보통", 2, 0, isPlayable = false, isReviewable = false),
        MissionStarLevel(3, "어려움", 2, 0, isPlayable = false, isReviewable = false),
    )

    private fun missionOrderKey(missionCode: String, title: String): Int {
        val idx = missionCode.indexOf("-M")
        if (idx >= 0) {
            missionCode.substring(idx + 2).toIntOrNull()?.let { return it }
        }
        return missionCode.filter { it.isDigit() }.toIntOrNull()
            ?: title.filter { it.isDigit() }.toIntOrNull()
            ?: 0
    }
}
