package com.kduniv.aimong.feature.settings.data

import com.google.firebase.auth.FirebaseAuth
import com.kduniv.aimong.core.local.SessionManager
import com.kduniv.aimong.core.network.AimongApiService
import com.kduniv.aimong.core.network.ApiErrorMapper
import com.kduniv.aimong.core.network.model.NotificationSettingsRequest
import com.kduniv.aimong.core.network.model.NotificationSettingsResponseData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationSettingsRepositoryImpl @Inject constructor(
    private val apiService: AimongApiService,
    private val sessionManager: SessionManager
) : NotificationSettingsRepository {

    private suspend fun requireParentIdToken(): String {
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("Firebase 로그인이 필요합니다.")
        return user.getIdToken(false).await().token
            ?: throw IllegalStateException("Firebase 토큰을 가져오지 못했습니다.")
    }

    private suspend fun isParentSession(): Boolean =
        sessionManager.userRole.first()?.uppercase() == "PARENT"

    override suspend fun getSettings(): Result<NotificationSettingsResponseData> = try {
        val response = if (isParentSession()) {
            val idToken = requireParentIdToken()
            apiService.getNotificationSettingsParent("Bearer $idToken")
        } else {
            apiService.getNotificationSettings()
        }
        if (response.success) Result.success(response.data)
        else Result.failure(Exception(ApiErrorMapper.userMessageForApiError(response.error)))
    } catch (e: HttpException) {
        Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
    } catch (e: IOException) {
        Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun patchSettings(request: NotificationSettingsRequest): Result<NotificationSettingsResponseData> = try {
        val response = if (isParentSession()) {
            val idToken = requireParentIdToken()
            apiService.patchNotificationSettingsParent("Bearer $idToken", request)
        } else {
            apiService.patchNotificationSettings(request)
        }
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

