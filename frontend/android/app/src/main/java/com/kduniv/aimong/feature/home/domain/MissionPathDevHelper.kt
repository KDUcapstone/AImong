package com.kduniv.aimong.feature.home.domain

import com.kduniv.aimong.core.dev.UiMode
import com.kduniv.aimong.feature.mission.domain.model.Mission
import com.kduniv.aimong.feature.mission.domain.model.MissionStarLevel

/**
 * 홈 미션 경로에 탭 가능한 노드가 없을 때 보정.
 * - 목업: 스테이지마다 첫 미션에 ★1 플레이 보장
 * - 실서버: 1스테이지 첫 해금 미션에 ★1 플레이 보장 (GET /missions·status 불일치 대비)
 */
object MissionPathDevHelper {

    fun applyPathUnlockGuarantees(missions: List<Mission>): List<Mission> {
        if (missions.isEmpty()) return missions
        val afterStub = if (UiMode.useStubNav) ensureOnePlayablePerStage(missions) else missions
        return ensureStageOneEntryMissionPlayable(afterStub)
    }

    private fun ensureOnePlayablePerStage(missions: List<Mission>): List<Mission> {
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
            updated[idx] = withGuaranteedEasyPlayable(candidate)
        }
        return updated
    }

    /** 실서버: 1스테이지에 플레이 가능한 미션이 하나도 없으면 첫 해금 미션 ★1 오픈 */
    private fun ensureStageOneEntryMissionPlayable(missions: List<Mission>): List<Mission> {
        if (UiMode.useStubNav) return missions
        val stage1 = missions
            .filter { it.stage == 1 && it.isUnlocked }
            .sortedBy { missionOrderKey(it.missionCode, it.title) }
        if (stage1.isEmpty()) return missions
        if (stage1.any { it.starLevels.any { s -> s.isPlayable || s.isReviewable } }) {
            return missions
        }
        val target = stage1.first()
        val idx = missions.indexOfFirst { it.missionId == target.missionId }
        if (idx < 0) return missions
        return missions.toMutableList().apply {
            this[idx] = withGuaranteedEasyPlayable(target)
        }
    }

    private fun Mission.hasPlayablePathNode(): Boolean =
        isUnlocked && starLevels.any { it.isPlayable || it.isReviewable || it.isCompleted }

    fun withGuaranteedEasyPlayable(mission: Mission): Mission {
        val baseStars = mission.starLevels.ifEmpty { defaultStarLevels() }
        val patched = baseStars.map { level ->
            if (level.starLevel == 1) level.copy(isPlayable = true) else level
        }
        val withEasy = if (patched.any { it.starLevel == 1 }) {
            patched
        } else {
            listOf(
                MissionStarLevel(1, "쉬움", 2, 0, isPlayable = true, isReviewable = false),
            ) + patched
        }
        return mission.copy(isUnlocked = true, starLevels = withEasy)
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
