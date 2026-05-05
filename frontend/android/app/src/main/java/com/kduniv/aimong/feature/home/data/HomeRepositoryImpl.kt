package com.kduniv.aimong.feature.home.data

import com.kduniv.aimong.core.network.AimongApiService
import com.kduniv.aimong.core.network.ApiErrorMapper
import com.kduniv.aimong.feature.home.data.model.HomeScreenData
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
            val response = apiService.getHome()
            if (response.success) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(ApiErrorMapper.userMessageForApiError(response.error)))
            }
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
            val response = apiService.getStreakCalendar(yearMonth)
            if (response.success) {
                Result.success(StreakCalendarMapper.normalize(yearMonth, response.data))
            } else {
                Result.failure(Exception(ApiErrorMapper.userMessageForApiError(response.error)))
            }
        } catch (e: HttpException) {
            Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: IOException) {
            Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
