package com.kduniv.aimong.feature.home.domain

import androidx.annotation.DrawableRes
import com.kduniv.aimong.R
import com.kduniv.aimong.feature.home.data.model.HomeScreenData
import com.kduniv.aimong.feature.home.presentation.DifficultyUnlockMode
import com.kduniv.aimong.feature.home.presentation.HomePathItem
import com.kduniv.aimong.feature.home.presentation.HomeQuizNavigation
import com.kduniv.aimong.feature.mission.domain.model.Mission
import com.kduniv.aimong.feature.mission.domain.model.openDifficultyCount
import com.kduniv.aimong.feature.mission.domain.model.displayTitle
import com.kduniv.aimong.feature.mission.domain.model.toDisplayMissionTitle

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

    fun build(data: HomeScreenData, missions: List<Mission>): List<HomePathItem> {
        val rec = data.missionSummary.recommendedMission
        val summary = data.missionSummary
        val canStart = summary.canStartMission
        val items = mutableListOf<HomePathItem>()

        val recSetIdStr = rec?.setId?.toString()?.takeIf { it != "0" && it.isNotBlank() }

        val groupedByStage = missions
            .groupBy { it.stage }
            .toSortedMap()

        // v2.4: /home 의 recommendedMission.id 가 UUID가 아닐 수 있어(예: "1").
        // 홈에서 퀴즈 진입은 /missions 목록의 UUID missionId를 우선 사용하도록 보정한다.
        val resolvedRecommendedMissionId: String? = rec?.let { r ->
            missions.firstOrNull { it.missionId == r.id }?.missionId
                ?: missions.firstOrNull { it.stage == r.stage && it.title == r.title }?.missionId
                ?: missions.firstOrNull { it.title == r.title }?.missionId
                ?: r.id
        }

        for (stage in 1..3) {
            val stageMissions = groupedByStage[stage] ?: emptyList()
            val sortedMissions = stageMissions
                .sortedBy { missionOrderKey(it.missionCode, it.title) }

            val stageTitle = STAGE_TITLES[stage] ?: "단계 $stage"
            val meta = ISLAND_META.getOrNull(stage - 1) ?: ISLAND_META.first()
            // 다음 스테이지(섬) 해금 조건: 이 스테이지 ★1(쉬움) 클리어 수 (BE isUnlocked 와 별개 축)
            val completedInStage = sortedMissions.count { m ->
                m.isUnlocked &&
                    m.starLevels.any { it.starLevel == 1 && it.isCompleted }
            }
            val unlockedInStage = sortedMissions.count { it.isUnlocked }
            items.add(
                HomePathItem.SectionHeader(
                    stage = stage,
                    islandEmoji = meta.emoji,
                    islandName = meta.name,
                    progressCompleted = completedInStage,
                    progressTotal = unlockedInStage.coerceAtLeast(1),
                    themeHint = stageTitle,
                    bannerDrawableRes = meta.banner,
                )
            )

            sortedMissions.forEachIndexed { index, m ->
                // 노드 아래 ★: starLevels[].isPlayable 만 (미션 isUnlocked 와 별개)
                val stars = m.openDifficultyCount()
                val displayTitle = m.displayTitle()
                if (!m.isUnlocked) {
                    items.add(HomePathItem.Locked(hint = "잠김"))
                } else if (rec != null && resolvedRecommendedMissionId != null && m.missionId == resolvedRecommendedMissionId) {
                    val todayNav = HomeQuizNavigation(
                        entrySetId = recSetIdStr.orEmpty(),
                        missionId = resolvedRecommendedMissionId,
                        starLevel = if (!recSetIdStr.isNullOrBlank()) -1 else 1
                    )
                    val todayTitle = displayTitle.ifBlank {
                        rec.title.toDisplayMissionTitle(rec.missionCode.orEmpty())
                    }
                    val recMissionStars = m.starLevels
                    val anyPlayable = recMissionStars.any { it.isPlayable }
                    val todayUnlock = when {
                        rec.isReviewable && !anyPlayable -> DifficultyUnlockMode.REVIEW
                        rec.isReviewable && anyPlayable -> DifficultyUnlockMode.PER_STAR
                        else -> DifficultyUnlockMode.NEW_PLAY
                    }
                    items.add(
                        HomePathItem.TodayStart(
                            quizNav = todayNav,
                            missionTitle = todayTitle,
                            enabled = canStart,
                            skipEnergyCheck = rec.isReviewable && !anyPlayable,
                            unlockMode = todayUnlock,
                            starsFilled = stars
                        )
                    )
                } else if (m.starLevels.any { it.isCompleted }) {
                    val sl = m.starLevels.firstOrNull { it.isPlayable }
                        ?: m.starLevels.firstOrNull { it.isReviewable }
                        ?: m.starLevels.firstOrNull()
                    val star = sl?.starLevel?.takeIf { it in 1..3 } ?: 1
                    items.add(
                        HomePathItem.Completed(
                            order = index + 1,
                            title = displayTitle,
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
                            subtitle = displayTitle,
                            starsFilled = stars
                        )
                    )
                } else if (m.starLevels.any { it.isPlayable }) {
                    val star = m.starLevels.firstOrNull { it.isPlayable }?.starLevel?.takeIf { it in 1..3 } ?: 1
                    items.add(
                        HomePathItem.Start(
                            quizNav = HomeQuizNavigation("", m.missionId, star),
                            missionTitle = displayTitle,
                            enabled = true,
                            icon = "▶",
                            starsFilled = stars,
                        )
                    )
                } else {
                    items.add(HomePathItem.Locked(hint = "대기 중"))
                }
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
