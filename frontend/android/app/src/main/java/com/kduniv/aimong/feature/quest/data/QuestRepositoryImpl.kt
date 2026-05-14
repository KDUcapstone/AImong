package com.kduniv.aimong.feature.quest.data

import com.kduniv.aimong.core.dev.UiMode
import com.kduniv.aimong.core.network.AimongApiService
import com.kduniv.aimong.core.network.ApiErrorMapper
import com.kduniv.aimong.feature.quest.data.model.AchievementItemDto
import com.kduniv.aimong.feature.quest.data.model.AchievementsResponseData
import com.kduniv.aimong.feature.quest.data.model.DailyQuestsResponseData
import com.kduniv.aimong.feature.quest.data.model.QuestApiItemDto
import com.kduniv.aimong.feature.quest.data.model.QuestApiProgressDto
import com.kduniv.aimong.feature.quest.data.model.QuestClaimRequest
import com.kduniv.aimong.feature.quest.data.model.QuestClaimResponseData
import com.kduniv.aimong.feature.quest.data.model.QuestRemainingTicketsDto
import com.kduniv.aimong.feature.quest.data.model.WeeklyQuestsResponseData
import com.kduniv.aimong.feature.quest.domain.repository.QuestRepository
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class QuestRepositoryImpl @Inject constructor(
    private val apiService: AimongApiService
) : QuestRepository {

    override suspend fun getDailyQuests(): Result<DailyQuestsResponseData> {
        if (UiMode.useStubNav) return Result.success(stubDailyQuests())
        return try {
            val response = apiService.getDailyQuests()
            if (response.success) Result.success(response.data)
            else Result.failure(Exception(ApiErrorMapper.userMessageForApiError(response.error)))
        } catch (e: HttpException) {
            Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: IOException) {
            Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getWeeklyQuests(): Result<WeeklyQuestsResponseData> {
        if (UiMode.useStubNav) return Result.success(stubWeeklyQuests())
        return try {
            val response = apiService.getWeeklyQuests()
            if (response.success) Result.success(response.data)
            else Result.failure(Exception(ApiErrorMapper.userMessageForApiError(response.error)))
        } catch (e: HttpException) {
            Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: IOException) {
            Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun claimQuest(questType: String, period: String): Result<QuestClaimResponseData> {
        if (UiMode.useStubNav) {
            return Result.success(
                QuestClaimResponseData(
                    rewards = emptyList(),
                    remainingTickets = QuestRemainingTicketsDto(normal = 4, rare = 0, epic = 0)
                )
            )
        }
        return try {
            val response = apiService.claimQuest(QuestClaimRequest(questType, period))
            if (response.success) Result.success(response.data)
            else Result.failure(Exception(ApiErrorMapper.userMessageForApiError(response.error)))
        } catch (e: HttpException) {
            Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: IOException) {
            Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAchievements(): Result<AchievementsResponseData> {
        if (UiMode.useStubNav) {
            return Result.success(
                AchievementsResponseData(
                    achievements = listOf(
                        AchievementItemDto("STUB_1", "첫 AI 탐험", isCompleted = true),
                        AchievementItemDto("STUB_2", "퀘스트 마스터", isCompleted = false)
                    )
                )
            )
        }
        return try {
            val response = apiService.getAchievements()
            if (response.success) Result.success(response.data)
            else Result.failure(Exception(ApiErrorMapper.userMessageForApiError(response.error)))
        } catch (e: HttpException) {
            Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: IOException) {
            Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun stubDailyQuests(): DailyQuestsResponseData {
        val p = QuestApiProgressDto(current = 1, required = 1)
        return DailyQuestsResponseData(
            date = "2026-05-13",
            todayXp = 120,
            quests = listOf(
                QuestApiItemDto(
                    questType = "DAILY_MISSION",
                    label = "미션 1회 완료",
                    reward = "+20 XP",
                    claimType = "AUTO",
                    completed = false,
                    rewardClaimed = false,
                    progress = QuestApiProgressDto(0, 1)
                ),
                QuestApiItemDto(
                    questType = "DAILY_CHAT",
                    label = "챗봇과 대화하기",
                    reward = "+15 XP",
                    claimType = "AUTO",
                    completed = true,
                    rewardClaimed = false,
                    progress = QuestApiProgressDto(1, 1)
                ),
                QuestApiItemDto(
                    questType = "DAILY_ATTEND",
                    label = "출석하기",
                    reward = "+10 XP",
                    claimType = "MANUAL",
                    completed = true,
                    rewardClaimed = false,
                    progress = p
                )
            )
        )
    }

    private fun stubWeeklyQuests(): WeeklyQuestsResponseData {
        return WeeklyQuestsResponseData(
            weekStart = "2026-05-11",
            weeklyXp = 340,
            quests = listOf(
                QuestApiItemDto(
                    questType = "WEEKLY_MISSION",
                    label = "미션 5회 완료",
                    reward = "티켓",
                    claimType = "AUTO",
                    completed = false,
                    rewardClaimed = false,
                    progress = QuestApiProgressDto(2, 5)
                )
            )
        )
    }
}
