package com.kduniv.aimong.feature.dev.mock.repository

import com.kduniv.aimong.feature.quest.data.model.AchievementItemDto
import com.kduniv.aimong.feature.quest.data.model.AchievementProgressDto
import com.kduniv.aimong.feature.quest.data.model.AchievementsResponseData
import com.kduniv.aimong.feature.quest.data.model.DailyQuestsResponseData
import com.kduniv.aimong.feature.quest.data.model.QuestApiItemDto
import com.kduniv.aimong.feature.quest.data.model.QuestApiProgressDto
import com.kduniv.aimong.feature.quest.data.model.QuestClaimResponseData
import com.kduniv.aimong.feature.quest.data.model.QuestRemainingTicketsDto
import com.kduniv.aimong.feature.quest.data.model.QuestRewardItemDto
import com.kduniv.aimong.feature.quest.data.model.WeeklyQuestsResponseData
import com.kduniv.aimong.feature.quest.domain.repository.QuestRepository
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [UiMode.useStubNav] 전용 — 네트워크 없이 퀘스트 시트·도전과제 UI가 동작하도록 고정 데이터를 돌려준다.
 */
@Singleton
class QuestRepositoryStub @Inject constructor() : QuestRepository {

    override suspend fun getDailyQuests(): Result<DailyQuestsResponseData> {
        val today = LocalDate.now().toString()
        return Result.success(
            DailyQuestsResponseData(
                date = today,
                todayXp = 120,
                quests = listOf(
                    sampleQuest("DAILY_MISSION", "오늘 미션 1회", "AUTO", current = 0, required = 1),
                    sampleQuest("DAILY_CHAT", "챗봇과 대화하기", "AUTO", current = 1, required = 1, completed = true),
                    sampleQuest("DAILY_XP", "XP 50 모으기", "AUTO", current = 35, required = 50)
                )
            )
        )
    }

    override suspend fun getWeeklyQuests(): Result<WeeklyQuestsResponseData> {
        val monday = LocalDate.now().with(DayOfWeek.MONDAY).toString()
        return Result.success(
            WeeklyQuestsResponseData(
                weekStart = monday,
                weeklyXp = 340,
                quests = listOf(
                    sampleQuest("WEEKLY_MISSION", "이번 주 미션 5회", "AUTO", current = 2, required = 5),
                    sampleQuest("WEEKLY_XP", "주간 XP 200", "AUTO", current = 200, required = 200, completed = true)
                )
            )
        )
    }

    override suspend fun claimQuest(questType: String, period: String): Result<QuestClaimResponseData> {
        return Result.success(
            QuestClaimResponseData(
                rewards = listOf(
                    QuestRewardItemDto(type = "EXP", ticketType = null, count = 30, reason = "MOCK_CLAIM")
                ),
                remainingTickets = QuestRemainingTicketsDto(normal = 3, rare = 1, epic = 0)
            )
        )
    }

    override suspend fun getAchievements(): Result<AchievementsResponseData> {
        return Result.success(
            AchievementsResponseData(
                achievements = listOf(
                    AchievementItemDto(
                        achievementType = "FIRST_CLEAR",
                        label = "첫 미션 클리어",
                        isCompleted = true,
                        completedAt = "2026-01-01T00:00:00Z",
                        progress = null
                    ),
                    AchievementItemDto(
                        achievementType = "STREAK_7",
                        label = "7일 연속 학습",
                        isCompleted = false,
                        completedAt = null,
                        progress = AchievementProgressDto(current = 3, required = 7)
                    )
                )
            )
        )
    }

    private fun sampleQuest(
        questType: String,
        label: String,
        claimType: String,
        current: Int,
        required: Int,
        completed: Boolean = false,
        rewardClaimed: Boolean = false
    ) = QuestApiItemDto(
        questType = questType,
        label = label,
        reward = "XP",
        claimType = claimType,
        completed = completed,
        rewardClaimed = rewardClaimed,
        progress = QuestApiProgressDto(current = current, required = required)
    )
}
