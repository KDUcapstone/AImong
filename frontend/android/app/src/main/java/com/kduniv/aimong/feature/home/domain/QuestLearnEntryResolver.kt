package com.kduniv.aimong.feature.home.domain

import com.kduniv.aimong.feature.home.presentation.DifficultyUnlockMode
import com.kduniv.aimong.feature.home.presentation.HomeQuizNavigation
import com.kduniv.aimong.feature.mission.domain.model.Mission
import com.kduniv.aimong.feature.mission.domain.model.MissionStarLevel
import com.kduniv.aimong.feature.mission.domain.model.hasActiveStar1
import com.kduniv.aimong.feature.mission.domain.model.isStar1Completed

/**
 * 퀘스트 「학습하기」 진입 — 마지막으로 스테이지 별1을 전부 깬 다음 스테이지의
 * 첫 소단원 · 별 난이도 1(쉬움)으로 이동한다.
 */
object QuestLearnEntryResolver {

    fun resolve(missions: List<Mission>): Pair<HomeQuizNavigation, DifficultyUnlockMode>? {
        if (missions.isEmpty()) return null
        val lastCleared = lastStar1FullyClearedStage(missions)
        val targetStage = (lastCleared + 1).coerceIn(1, 3)
        return entryForStageStar1(missions, targetStage)
            ?: if (targetStage > 1) entryForStageStar1(missions, targetStage - 1) else null
    }

    /** 스테이지 내 missionCode 순 첫 해금 미션 · ★1 */
    private fun entryForStageStar1(
        missions: List<Mission>,
        stage: Int,
    ): Pair<HomeQuizNavigation, DifficultyUnlockMode>? {
        val ordered = missions
            .filter { it.stage == stage && it.isUnlocked }
            .sortedBy { missionOrderKey(it.missionCode, it.title) }
        for (mission in ordered) {
            val entry = star1Entry(mission) ?: continue
            return entry
        }
        return null
    }

    private fun star1Entry(mission: Mission): Pair<HomeQuizNavigation, DifficultyUnlockMode>? {
        val sl = mission.starLevels.firstOrNull { it.starLevel == 1 } ?: return null
        val mode = unlockModeForStar1(sl) ?: return null
        val nav = HomeQuizNavigation(
            entrySetId = "",
            missionId = mission.missionId,
            starLevel = 1,
        )
        return nav to mode
    }

    private fun unlockModeForStar1(sl: MissionStarLevel): DifficultyUnlockMode? = when {
        sl.isPlayable -> DifficultyUnlockMode.NEW_PLAY
        sl.isReviewable && !sl.isPlayable -> DifficultyUnlockMode.REVIEW
        else -> null
    }

    private fun lastStar1FullyClearedStage(missions: List<Mission>): Int {
        var last = 0
        for (stage in 1..3) {
            val star1Missions = missions.filter { it.stage == stage && it.hasActiveStar1() }
            if (star1Missions.isNotEmpty() && star1Missions.all { it.isStar1Completed() }) {
                last = stage
            }
        }
        return last
    }

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
