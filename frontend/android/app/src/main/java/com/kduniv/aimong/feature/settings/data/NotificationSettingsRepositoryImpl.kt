package com.kduniv.aimong.feature.settings.data

import com.kduniv.aimong.core.auth.FirebaseParentTokenProvider
import com.kduniv.aimong.core.local.SessionManager
import com.kduniv.aimong.core.network.AimongApiService
import com.kduniv.aimong.core.network.ApiErrorMapper
import com.kduniv.aimong.core.network.model.NotificationSettingsData
import com.kduniv.aimong.core.network.model.NotificationSettingsPatchRequest
import com.kduniv.aimong.core.network.toResult
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationSettingsRepositoryImpl @Inject constructor(
    private val apiService: AimongApiService,
    private val sessionManager: SessionManager,
    private val firebaseParentTokenProvider: FirebaseParentTokenProvider,
) : NotificationSettingsRepository {

    override suspend fun getSettings(): Result<NotificationSettingsData> {
        return try {
            resolveGetSettings().toResult()
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
            resolvePatchSettings(patch).toResult()
        } catch (e: HttpException) {
            Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: IOException) {
            Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun resolveGetSettings() =
        if (sessionManager.userRole.first() == "PARENT") {
            val token = firebaseParentTokenProvider.getIdTokenOrNull()
                ?: throw IllegalStateException("Firebase 로그인이 필요합니다.")
            apiService.getNotificationSettingsWithAuth("Bearer $token")
        } else {
            apiService.getNotificationSettings()
        }

    private suspend fun resolvePatchSettings(patch: NotificationSettingsPatchRequest) =
        if (sessionManager.userRole.first() == "PARENT") {
            val token = firebaseParentTokenProvider.getIdTokenOrNull()
                ?: throw IllegalStateException("Firebase 로그인이 필요합니다.")
            apiService.patchNotificationSettingsWithAuth("Bearer $token", patch)
        } else {
            apiService.patchNotificationSettings(patch)
        }
}
