package com.kduniv.aimong.feature.home.data

import com.kduniv.aimong.core.network.AimongApiService
import com.kduniv.aimong.core.network.ApiErrorMapper
import com.kduniv.aimong.core.network.toResult
import com.kduniv.aimong.feature.home.data.model.EnergyAddRequest
import com.kduniv.aimong.feature.home.data.model.EnergyAddResponseData
import com.kduniv.aimong.feature.home.data.model.EnergyStateData
import com.kduniv.aimong.feature.home.data.model.HomeScreenData
import com.kduniv.aimong.feature.home.data.model.ReturnRewardCheckResponseData
import com.kduniv.aimong.feature.home.data.model.ReturnRewardClaimResponseData
import com.kduniv.aimong.feature.home.domain.model.StreakCalendarResult
import com.kduniv.aimong.feature.home.domain.repository.HomeRepository
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class HomeRepositoryImpl @Inject constructor(
    private val apiService: AimongApiService
) : HomeRepository {

    override suspend fun getHome(): Result<HomeScreenData> {
        return try {
            apiService.getHome().toResult()
        } catch (e: HttpException) {
            Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: IOException) {
            Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getEnergy(): Result<EnergyStateData> {
        return try {
            apiService.getEnergy().toResult()
        } catch (e: HttpException) {
            Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: IOException) {
            Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addEnergy(amount: Int): Result<EnergyAddResponseData> {
        return try {
            apiService.addEnergy(EnergyAddRequest(amount)).toResult()
        } catch (e: HttpException) {
            Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: IOException) {
            Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getStreakCalendar(yearMonth: String?): Result<StreakCalendarResult> {
        return try {
            apiService.getStreakCalendar(yearMonth).toResult().map { body ->
                StreakCalendarMapper.normalize(yearMonth, body)
            }
        } catch (e: HttpException) {
            Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: IOException) {
            Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getReturnReward(): Result<ReturnRewardCheckResponseData> {
        return try {
            apiService.getReturnReward().toResult()
        } catch (e: HttpException) {
            Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: IOException) {
            Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun claimReturnReward(): Result<ReturnRewardClaimResponseData> {
        return try {
            apiService.claimReturnReward().toResult()
        } catch (e: HttpException) {
            Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: IOException) {
            Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
