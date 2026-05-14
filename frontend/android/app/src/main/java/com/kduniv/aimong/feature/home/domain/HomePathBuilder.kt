package com.kduniv.aimong.feature.home.domain

import androidx.annotation.DrawableRes
import com.kduniv.aimong.R
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

    private data class IslandMeta(val emoji: String, val name: String, @DrawableRes val banner: Int)

    private val ISLAND_META = listOf(
        IslandMeta("🏝️", "시작의 섬", R.drawable.bg_home_section_banner_stage1),
        IslandMeta("🌋", "탐험의 화산섬", R.drawable.bg_home_section_banner_stage2),
        IslandMeta("⭐", "마스터의 별섬", R.drawable.bg_home_section_banner_stage3),
    )

    private fun Mission.filledStars(): Int =
        starLevels.count { it.isCompleted }.coerceIn(0, 3)

    fun build(data: HomeScreenData, missions: List<Mission>): List<HomePathItem> {
        val rec = data.missionSummary.recommendedMission
        val summary = data.missionSummary
        val canStart = summary.canStartMission
        val dailyQuotaActive = summary.todayTargetCount > 0
        val underDailyQuota = !dailyQuotaActive || summary.todayCompletedCount < summary.todayTargetCount
        val todayStartEnabled = canStart || (rec != null && underDailyQuota)
        val items = mutableListOf<HomePathItem>()

        val recSetIdStr = rec?.setId?.toString()?.takeIf { it != "0" && it.isNotBlank() }

        val groupedByStage = missions
            .groupBy { it.stage }
            .toSortedMap()

        for (stage in 1..3) {
            val stageMissions = groupedByStage[stage] ?: emptyList()
            val sortedMissions = stageMissions
                .sortedBy { it.missionId.toIntOrNull() ?: 0 }
                .take(10)

            val stageTitle = STAGE_TITLES[stage] ?: "단계 $stage"
            val meta = ISLAND_META.getOrNull(stage - 1) ?: ISLAND_META.first()
            val completedInStage = sortedMissions.count { m ->
                m.starLevels.any { it.isCompleted }
            }
            val totalInStage = sortedMissions.size.coerceAtLeast(1)
            items.add(
                HomePathItem.SectionHeader(
                    stage = stage,
                    islandEmoji = meta.emoji,
                    islandName = meta.name,
                    progressCompleted = completedInStage,
                    progressTotal = totalInStage,
                    themeHint = stageTitle,
                    bannerDrawableRes = meta.banner,
                )
            )

            var nodeCount = 0

            sortedMissions.forEachIndexed { index, m ->
                val stars = m.filledStars()
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
                            enabled = todayStartEnabled,
                            starsFilled = stars
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
                            icon = "⭐",
                            starsFilled = stars
                        )
                    )
                } else if (m.starLevels.any { it.isReviewable }) {
                    val sl = m.starLevels.first { it.isReviewable }
                    val star = sl.starLevel.takeIf { it in 1..3 } ?: 1
                    items.add(
                        HomePathItem.Review(
                            quizNav = HomeQuizNavigation("", m.missionId, star),
                            subtitle = m.title,
                            starsFilled = stars
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
}
