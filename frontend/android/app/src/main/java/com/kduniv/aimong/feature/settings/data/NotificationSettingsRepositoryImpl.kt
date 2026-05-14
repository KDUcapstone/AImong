package com.kduniv.aimong.feature.settings.data

import com.kduniv.aimong.core.network.AimongApiService
import com.kduniv.aimong.core.network.ApiErrorMapper
import com.kduniv.aimong.core.network.model.NotificationSettingsData
import com.kduniv.aimong.core.network.model.NotificationSettingsPatchRequest
import com.kduniv.aimong.core.network.toResult
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationSettingsRepositoryImpl @Inject constructor(
    private val apiService: AimongApiService
) : NotificationSettingsRepository {

    override suspend fun getSettings(): Result<NotificationSettingsData> {
        return try {
            apiService.getNotificationSettings().toResult()
        } catch (e: HttpException) {
            Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: IOException) {
            Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun patchSettings(patch: NotificationSettingsPatchRequest): Result<NotificationSettingsData> {
        return try {
            apiService.patchNotificationSettings(patch).toResult()
        } catch (e: HttpException) {
            Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: IOException) {
            Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
