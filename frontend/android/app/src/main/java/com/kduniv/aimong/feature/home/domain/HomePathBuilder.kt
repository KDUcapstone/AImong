package com.kduniv.aimong.feature.home.domain

import com.kduniv.aimong.feature.home.data.model.HomeScreenData
import com.kduniv.aimong.feature.home.presentation.HomePathItem
import com.kduniv.aimong.feature.mission.domain.model.Mission

/**
 * GET /home 의 missionSummary + GET /missions 목록으로 학습 경로 노드를 구성합니다.
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

        val groupedByStage = missions.groupBy { it.stage }.toSortedMap()

        // 1단계부터 3단계까지 순차적으로 렌더링
        for (stage in 1..3) {
            val stageMissions = groupedByStage[stage] ?: emptyList()
            // id 내 숫자를 추출하여 오름차순(낮은 난이도 순) 정렬
            val sortedMissions = stageMissions.sortedBy { m ->
                m.id.filter { it.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE
            }
            
            val stageTitle = STAGE_TITLES[stage] ?: "단계 $stage"
            items.add(HomePathItem.SectionHeader(stage = stage, title = stageTitle))
            
            var nodeCount = 0
            
            sortedMissions.forEachIndexed { index, m ->
                if (rec != null && m.id == rec.id) {
                    items.add(
                        HomePathItem.TodayStart(
                            missionId = rec.id,
                            missionTitle = rec.title,
                            enabled = canStart
                        )
                    )
                } else if (m.isCompleted) {
                    items.add(HomePathItem.Completed(order = index + 1, title = m.title))
                } else if (m.isReviewable) {
                    items.add(HomePathItem.Review(missionId = m.id, subtitle = "복습 미션"))
                } else if (!m.isUnlocked) {
                    items.add(HomePathItem.Locked(hint = "잠김"))
                } else {
                    items.add(HomePathItem.Locked(hint = "대기 중"))
                }
                nodeCount++
            }
            
            // 한 스테이지 당 총 10개의 노드가 되도록 더미 Locked 노드로 패딩
            while (nodeCount < 10) {
                items.add(HomePathItem.Locked(hint = "준비 중"))
                nodeCount++
            }
        }

        return items
    }
}
