package com.kduniv.aimong.feature.home.domain

import com.kduniv.aimong.feature.home.data.model.HomeScreenData
import com.kduniv.aimong.feature.home.presentation.HomePathItem
import com.kduniv.aimong.feature.home.presentation.HomeQuizNavigation
import com.kduniv.aimong.feature.mission.domain.model.Mission

/**
 * GET /home 의 missionSummary + GET /missions(v2.3) 목록으로 학습 경로 노드를 구성합니다.
 */
object HomePathBuilder {

    private val STAGE_TITLES = mapOf(
        1 to "AI가 뭐예요?",
        2 to "AI 잘 쓰기",
        3 to "비판적으로 생각하기"
    )

    fun build(data: HomeScreenData, missions: List<Mission>): List<HomePathItem> {
        val rec = data.missionSummary.recommendedMission
        val canStart = data.missionSummary.canStartMission
        val items = mutableListOf<HomePathItem>()

        val recSetIdStr = rec?.setId?.toString()?.takeIf { it != "0" && it.isNotBlank() }

        val groupedByStage = missions
            .groupBy { it.stage }
            .toSortedMap()

        for (stage in 1..3) {
            val stageMissions = groupedByStage[stage] ?: emptyList()
            val sortedMissions = stageMissions
                .sortedBy { missionOrderKey(it.missionCode, it.title) }
                .take(10)

            val stageTitle = STAGE_TITLES[stage] ?: "단계 $stage"
            items.add(HomePathItem.SectionHeader(stage = stage, title = stageTitle))

            var nodeCount = 0

            sortedMissions.forEachIndexed { index, m ->
                if (rec != null && m.missionId == rec.id) {
                    val todayNav = HomeQuizNavigation(
                        entrySetId = recSetIdStr.orEmpty(),
                        missionId = rec.id,
                        starLevel = if (!recSetIdStr.isNullOrBlank()) -1 else 1
                    )
                    items.add(
                        HomePathItem.TodayStart(
                            quizNav = todayNav,
                            missionTitle = rec.title,
                            enabled = canStart
                        )
                    )
                } else if (m.starLevels.any { it.isCompleted }) {
                    val sl = m.starLevels.firstOrNull { it.isReviewable && it.isPlayable }
                        ?: m.starLevels.firstOrNull { it.isPlayable }
                        ?: m.starLevels.firstOrNull()
                    val star = sl?.starLevel?.takeIf { it in 1..3 } ?: 1
                    items.add(
                        HomePathItem.Completed(
                            order = index + 1,
                            title = m.title,
                            missionId = m.missionId,
                            quizNav = HomeQuizNavigation("", m.missionId, star),
                            icon = "⭐"
                        )
                    )
                } else if (m.starLevels.any { it.isReviewable }) {
                    val sl = m.starLevels.first { it.isReviewable }
                    val star = sl.starLevel.takeIf { it in 1..3 } ?: 1
                    items.add(
                        HomePathItem.Review(
                            quizNav = HomeQuizNavigation("", m.missionId, star),
                            subtitle = m.title
                        )
                    )
                } else if (m.isUnlocked && m.starLevels.any { it.isPlayable }) {
                    val star = m.starLevels.firstOrNull { it.isPlayable }?.starLevel?.takeIf { it in 1..3 } ?: 1
                    items.add(
                        HomePathItem.Start(
                            quizNav = HomeQuizNavigation("", m.missionId, star),
                            missionTitle = m.title,
                            enabled = true,
                            icon = "▶"
                        )
                    )
                } else if (!m.isUnlocked) {
                    items.add(HomePathItem.Locked(hint = "잠김"))
                } else {
                    items.add(HomePathItem.Locked(hint = "대기 중"))
                }
                nodeCount++
            }

            while (nodeCount < 10) {
                items.add(HomePathItem.Locked(hint = "준비 중"))
                nodeCount++
            }

            if (stage < 3) {
                items.add(HomePathItem.InterStageDivider)
            }
        }

        return items
    }

    private fun missionOrderKey(missionCode: String, title: String): Int {
        // 예: "S1-M10" -> 10. 파싱 실패 시 0.
        val idx = missionCode.indexOf("-M")
        if (idx >= 0) {
            val n = missionCode.substring(idx + 2).toIntOrNull()
            if (n != null) return n
        }
        // 일부 서버는 "S1M10" 같은 형식일 수 있어 숫자만 추출 시도
        val digits = missionCode.filter { it.isDigit() }
        return digits.toIntOrNull() ?: title.filter { it.isDigit() }.toIntOrNull() ?: 0
    }
}
