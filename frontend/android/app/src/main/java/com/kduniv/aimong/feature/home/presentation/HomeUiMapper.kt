package com.kduniv.aimong.feature.home.presentation

import com.kduniv.aimong.feature.home.data.model.DailyQuestItemDto
import com.kduniv.aimong.feature.home.data.model.HomeScreenData

internal object HomeUiMapper {

    fun toUiState(data: HomeScreenData): HomeUiState {
        val profile = data.profile
        val top = data.topStatus
        val mission = data.missionSummary
        val streak = data.streak
        val quests = data.dailyQuestSummary
        val tickets = data.tickets
        val pet = data.equippedPet

        val userLevel = userLevelFromXp(profile.totalXp)
        val petStage = pet?.stage ?: "EGG"
        val petLv = stageToDisplayLevel(petStage)
        val petMax = maxXpForStage(petStage)
        val petXp = pet?.xp?.coerceIn(0, petMax) ?: 0

        val todayDone = "${quests.completedCount}/${quests.totalCount}"

        val homeState = when {
            mission.todayCompletedCount > 0 || streak.todayMissionCount > 0 -> HomeState.HAPPY
            else -> HomeState.IDLE
        }

        return HomeUiState(
            nickname = profile.nickname,
            totalXp = profile.totalXp,
            /** 스펙: topStatus.streakDays ≡ streak.continuousDays — streak 객체 우선 */
            streakDays = streak.continuousDays,
            profileType = profile.profileImageType,
            userLevel = userLevel,
            petName = pet?.let { petDisplayName(it.petType, it.grade) } ?: "",
            petXp = petXp,
            petMaxXp = petMax,
            petLevel = petLv,
            petStage = petStage,
            homeState = homeState,
            petMessage = petMessage(data),
            normalTickets = tickets.normal,
            shieldCount = streak.shieldCount,
            heartCount = top.heartCount,
            /** 스펙: topStatus.xp ≡ profile.totalXp — 프로필을 단일 표시 소스로 */
            topStatusXp = profile.totalXp,
            srBonus = tickets.rare + tickets.epic,
            gachaDescription = gachaDescription(tickets),
            todayQuestProgress = todayDone,
            quests = quests.quests.map { mapQuest(it, mission.canStartMission) },
            isLoading = false,
            errorMessage = null,
            serverDate = data.serverDate,
            topTicketCount = top.ticketCount,
            canStartMission = mission.canStartMission,
            returnRewardPending = data.returnReward.hasReward,
            dailyQuestClaimableCount = quests.claimableCount
        )
    }

    private fun userLevelFromXp(totalXp: Int): Int =
        1 + (totalXp / 80).coerceIn(0, 99)

    private fun petDisplayName(petType: String, grade: String): String {
        val tail = petType.substringAfterLast('_', "")
        val short = tail.filter { it.isDigit() }.takeIf { it.isNotBlank() }
        return buildString {
            append(when (grade.uppercase()) {
                "COMMON" -> "커먼 "
                "RARE" -> "레어 "
                "EPIC" -> "에픽 "
                else -> ""
            })
            append(short ?: tail.takeIf { it.isNotBlank() } ?: "펫")
        }.trim()
    }

    private fun stageToDisplayLevel(stage: String): Int =
        when (stage.uppercase()) {
            "EGG" -> 1
            "HATCH", "BABY" -> 2
            "GROWTH" -> 3
            "ADULT", "MATURE" -> 4
            else -> 2
        }

    private fun maxXpForStage(stage: String): Int =
        when (stage.uppercase()) {
            "EGG" -> 100
            "GROWTH" -> 300
            "HATCH", "BABY" -> 200
            else -> 500
        }

    private fun petMessage(data: HomeScreenData): String {
        if (data.returnReward.hasReward) {
            return "다시 만나서 반가워요! 보상을 확인해 보세요."
        }
        val m = data.missionSummary
        if (!m.canStartMission && m.todayCompletedCount >= m.todayTargetCount && m.todayTargetCount > 0) {
            return "오늘 미션 목표를 달성했어요!"
        }
        val rec = m.recommendedMission
        if (rec != null && m.canStartMission) {
            return "${rec.title}\n지금 도전해 볼까요?"
        }
        if (m.canStartMission) {
            return "학습 탭에서 오늘의 미션을 시작해 보세요."
        }
        return ""
    }

    private fun gachaDescription(t: com.kduniv.aimong.feature.home.data.model.TicketsDto): String {
        if (t.normal == 0 && t.rare == 0 && t.epic == 0) return ""
        return "일반 ${t.normal} · 레어 ${t.rare} · 에픽 ${t.epic}"
    }

    private fun mapQuest(q: DailyQuestItemDto, canStartMission: Boolean): QuestItemUiState {
        val lineComplete = when (q.claimType.uppercase()) {
            "MANUAL" -> q.completed && q.rewardClaimed
            else -> q.completed
        }
        val missionLike = q.questType.contains("MISSION", ignoreCase = true)
        val canStart = !lineComplete &&
            if (missionLike) canStartMission else true
        return QuestItemUiState(
            id = q.questType,
            title = q.label,
            rewardSummary = "${q.progress.current} / ${q.progress.required}",
            iconRes = null,
            isCompleted = lineComplete,
            canStart = canStart
        )
    }
}
