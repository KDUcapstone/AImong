package com.kduniv.aimong.feature.home.domain

import com.kduniv.aimong.feature.home.data.model.HomeScreenData
import com.kduniv.aimong.feature.home.presentation.HomePathItem
import com.kduniv.aimong.feature.mission.domain.model.Mission

/**
 * GET /home 의 missionSummary + GET /missions 목록으로 학습 경로 노드를 구성합니다.
 */
object HomePathBuilder {

    fun build(data: HomeScreenData, missions: List<Mission>): List<HomePathItem> {
        val rec = data.missionSummary.recommendedMission
        val canStart = data.missionSummary.canStartMission
        val sorted = missions.sortedWith(compareBy({ it.stage }, { it.title }))

        val items = mutableListOf<HomePathItem>()

        val completedForShow = sorted
            .filter { it.isCompleted && (rec == null || it.id != rec.id) }
            .take(2)

        completedForShow.forEachIndexed { index, m ->
            items.add(HomePathItem.Completed(order = index + 1, title = m.title))
        }

        if (rec != null) {
            items.add(
                HomePathItem.TodayStart(
                    missionId = rec.id,
                    missionTitle = rec.title,
                    enabled = canStart
                )
            )
        }

        sorted.firstOrNull { it.isReviewable && (rec == null || it.id != rec.id) }?.let { m ->
            items.add(HomePathItem.Review(missionId = m.id, subtitle = "틀린 문제"))
        }

        sorted.firstOrNull { !it.isUnlocked }?.let {
            items.add(HomePathItem.Locked(hint = "내일 열림"))
        }

        return items
    }
}
