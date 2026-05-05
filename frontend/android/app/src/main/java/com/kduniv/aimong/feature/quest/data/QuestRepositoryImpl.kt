package com.kduniv.aimong.feature.quest.data

import com.kduniv.aimong.core.network.AimongApiService
import com.kduniv.aimong.core.network.ApiErrorMapper
import com.kduniv.aimong.feature.quest.data.model.DailyQuestsResponseData
import com.kduniv.aimong.feature.quest.data.model.QuestClaimRequest
import com.kduniv.aimong.feature.quest.data.model.QuestClaimResponseData
import com.kduniv.aimong.feature.quest.data.model.WeeklyQuestsResponseData
import com.kduniv.aimong.feature.quest.domain.repository.QuestRepository
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class QuestRepositoryImpl @Inject constructor(
    private val apiService: AimongApiService
) : QuestRepository {

    override suspend fun getDailyQuests(): Result<DailyQuestsResponseData> = try {
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

    override suspend fun getWeeklyQuests(): Result<WeeklyQuestsResponseData> = try {
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

    override suspend fun claimQuest(questType: String, period: String): Result<QuestClaimResponseData> = try {
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
