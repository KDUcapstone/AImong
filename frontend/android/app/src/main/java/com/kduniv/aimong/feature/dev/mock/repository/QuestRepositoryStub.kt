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
import com.kduniv.aimong.feature.quest.domain.QuestPolicy
import com.kduniv.aimong.feature.quest.domain.repository.QuestRepository
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [UiMode.useStubNav] 전용 — GET/POST quests·achievements 목업 (단일 기본 티켓).
 */
@Singleton
class QuestRepositoryStub @Inject constructor() : QuestRepository {

    override suspend fun getDailyQuests(): Result<DailyQuestsResponseData> {
        val today = LocalDate.now().toString()
        return Result.success(
            DailyQuestsResponseData(
                date = today,
                todayXp = 10,
                quests = listOf(
                    sampleQuest(
                        "MISSION_1",
                        "미션 1개 완료하기",
                        "자동 적용(별도 수령 없음)",
                        "AUTO",
                        current = 1,
                        required = 1,
                        completed = true,
                        rewardClaimed = true,
                    ),
                    sampleQuest(
                        "XP_20",
                        "오늘 XP 20 획득하기",
                        "기본 티켓 1장",
                        "MANUAL",
                        current = 10,
                        required = 20,
                    ),
                    sampleQuest(
                        "ALL_3",
                        "데일리 3개 모두 완료",
                        "기본 티켓 1장",
                        "MANUAL",
                        current = 1,
                        required = 3,
                    ),
                ),
            ),
        )
    }

    override suspend fun getWeeklyQuests(): Result<WeeklyQuestsResponseData> {
        val monday = LocalDate.now().with(DayOfWeek.MONDAY).toString()
        return Result.success(
            WeeklyQuestsResponseData(
                weekStart = monday,
                weeklyXp = 30,
                quests = listOf(
                    sampleQuest(
                        "XP_100",
                        "이번 주 XP 100 획득하기",
                        "기본 티켓 2장",
                        "MANUAL",
                        current = 30,
                        required = 100,
                    ),
                    sampleQuest(
                        "MISSION_5",
                        "미션 5개 완료하기",
                        "기본 티켓 2장",
                        "MANUAL",
                        current = 3,
                        required = 5,
                    ),
                    sampleQuest(
                        "CHAT_3",
                        "GPT 챗봇 3번 사용하기",
                        "기본 티켓 1장",
                        "MANUAL",
                        current = 1,
                        required = 3,
                    ),
                ),
            ),
        )
    }

    override suspend fun claimQuest(questType: String, period: String): Result<QuestClaimResponseData> {
        if (QuestPolicy.isAutoClaimQuest(questType)) {
            return Result.failure(Exception("자동 지급 퀘스트는 수령 API를 호출할 수 없어요"))
        }
        return Result.success(
            QuestClaimResponseData(
                rewards = listOf(
                    QuestRewardItemDto(
                        type = "TICKET",
                        ticketType = "NORMAL",
                        count = 1,
                        reason = "MOCK_QUEST_$questType",
                    ),
                ),
                remainingTickets = QuestRemainingTicketsDto(normal = 3),
            ),
        )
    }

    override suspend fun getAchievements(): Result<AchievementsResponseData> {
        return Result.success(
            AchievementsResponseData(
                achievements = listOf(
                    AchievementItemDto(
                        achievementType = "MISSION_10",
                        label = "미션 10개 완료",
                        isCompleted = true,
                        completedAt = "2026-03-25",
                        progress = AchievementProgressDto(current = 10, required = 10),
                    ),
                    AchievementItemDto(
                        achievementType = "XP_500",
                        label = "XP 500 달성",
                        isCompleted = false,
                        completedAt = null,
                        progress = AchievementProgressDto(current = 240, required = 500),
                    ),
                ),
            ),
        )
    }

    private fun sampleQuest(
        questType: String,
        label: String,
        reward: String,
        claimType: String,
        current: Int,
        required: Int,
        completed: Boolean = false,
        rewardClaimed: Boolean = false,
    ) = QuestApiItemDto(
        questType = questType,
        label = label,
        reward = reward,
        claimType = claimType,
        completed = completed,
        rewardClaimed = rewardClaimed,
        progress = QuestApiProgressDto(current = current, required = required),
    )
}
