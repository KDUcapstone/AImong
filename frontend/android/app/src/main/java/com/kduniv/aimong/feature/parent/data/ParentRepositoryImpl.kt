package com.kduniv.aimong.feature.parent.data

import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kduniv.aimong.core.local.SessionManager
import com.kduniv.aimong.core.network.AimongApiService
import com.kduniv.aimong.core.network.ApiErrorMapper
import com.kduniv.aimong.core.network.model.ParentAddChildRequest
import com.kduniv.aimong.core.network.model.ParentChildDetailResponseData
import com.kduniv.aimong.core.network.model.ParentChildItem
import com.kduniv.aimong.core.network.model.ParentMeResponseData
import com.kduniv.aimong.core.network.model.ParentRegisterResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParentRepositoryImpl @Inject constructor(
    private val apiService: AimongApiService,
    private val sessionManager: SessionManager,
    private val gson: Gson
) : ParentRepository {

    private val childListType = object : TypeToken<List<ParentChildItem>>() {}.type

    override suspend fun syncParentChildren(): Result<List<ParentChildItem>> {
        val user = FirebaseAuth.getInstance().currentUser
            ?: return Result.failure(IllegalStateException("Firebase 로그인이 필요합니다."))
        return try {
            val idToken = user.getIdToken(false).await().token
                ?: return Result.failure(IllegalStateException("Firebase 토큰을 가져오지 못했습니다."))
            val response = apiService.getParentChildren("Bearer $idToken")
            if (response.success) {
                val list = response.data.children
                sessionManager.saveParentChildrenJson(gson.toJson(list))
                sessionManager.saveParentNickname(response.data.parentNickname)
                Result.success(list)
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

    override suspend fun getParentChildDetail(childId: String): Result<ParentChildDetailResponseData> = try {
        val idToken = requireParentIdToken()
        val response = apiService.getParentChildDetail("Bearer $idToken", childId)
        if (response.success) Result.success(response.data)
        else Result.failure(Exception(ApiErrorMapper.userMessageForApiError(response.error)))
    } catch (e: HttpException) {
        Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
    } catch (e: IOException) {
        Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getParentMe(): Result<ParentMeResponseData> = try {
        val idToken = requireParentIdToken()
        val response = apiService.getParentMe("Bearer $idToken")
        if (response.success) Result.success(response.data)
        else Result.failure(Exception(ApiErrorMapper.userMessageForApiError(response.error)))
    } catch (e: HttpException) {
        Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
    } catch (e: IOException) {
        Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun addChild(nickname: String): Result<ParentRegisterResponse> = try {
        val idToken = requireParentIdToken()
        val response = apiService.addParentChild(
            authorization = "Bearer $idToken",
            body = ParentAddChildRequest(nickname = nickname.trim())
        )
        if (response.success) Result.success(response.data)
        else Result.failure(Exception(ApiErrorMapper.userMessageForApiError(response.error)))
    } catch (e: HttpException) {
        Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
    } catch (e: IOException) {
        Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun regenerateChildCode(childId: String): Result<String> {
        val user = FirebaseAuth.getInstance().currentUser
            ?: return Result.failure(IllegalStateException("Firebase 로그인이 필요합니다."))
        return try {
            val idToken = user.getIdToken(false).await().token
                ?: return Result.failure(IllegalStateException("Firebase 토큰을 가져오지 못했습니다."))
            val response = apiService.regenerateChildCode("Bearer $idToken", childId)
            if (response.success) {
                val newCode = response.data.newCode
                // 재발급된 새 코드로 로컬 캐시(JSON) 업데이트
                val currentList = observeCachedParentChildren().first()
                val updatedList = currentList.map {
                    if (it.childId == childId) it.copy(code = newCode) else it
                }
                sessionManager.saveParentChildrenJson(gson.toJson(updatedList))
                
                Result.success(newCode)
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

    override fun observeCachedParentChildren(): Flow<List<ParentChildItem>> =
        sessionManager.parentChildrenJson.map { json ->
            if (json.isNullOrBlank()) emptyList()
            else runCatching { gson.fromJson<List<ParentChildItem>>(json, childListType) }.getOrElse { emptyList() }
        }

    private suspend fun requireParentIdToken(): String {
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("Firebase 로그인이 필요합니다.")
        return user.getIdToken(false).await().token
            ?: throw IllegalStateException("Firebase 토큰을 가져오지 못했습니다.")
    }
}
