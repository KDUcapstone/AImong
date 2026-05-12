package com.kduniv.aimong.feature.parent.data

import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kduniv.aimong.core.local.SessionManager
import com.kduniv.aimong.core.network.AimongApiService
import com.kduniv.aimong.core.network.ApiErrorMapper
import com.kduniv.aimong.core.network.toResult
import com.kduniv.aimong.core.network.model.ParentChildItem
import com.kduniv.aimong.core.network.model.ParentAccountDeleteRequest
import com.kduniv.aimong.core.network.model.ParentChildDetailData
import com.kduniv.aimong.core.network.model.ParentChildPatchResponseData
import com.kduniv.aimong.core.network.model.ParentMeData
import com.kduniv.aimong.core.network.model.ParentRegisterRequest
import com.kduniv.aimong.core.network.model.ParentRegisterResponse
import com.kduniv.aimong.core.network.model.PatchParentChildRequest
import com.kduniv.aimong.feature.parent.data.model.ParentChildSummaryResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentPrivacyLogResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentWeakPointsResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentWeeklyStatsResponseData
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
            apiService.getParentChildren("Bearer $idToken").toResult().map { data ->
                val list = data.children
                sessionManager.saveParentChildrenJson(gson.toJson(list))
                sessionManager.saveParentNickname(data.parentNickname)
                list
            }
        } catch (e: HttpException) {
            Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: IOException) {
            Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun regenerateChildCode(childId: String): Result<String> {
        val user = FirebaseAuth.getInstance().currentUser
            ?: return Result.failure(IllegalStateException("Firebase 로그인이 필요합니다."))
        return try {
            val idToken = user.getIdToken(false).await().token
                ?: return Result.failure(IllegalStateException("Firebase 토큰을 가져오지 못했습니다."))
            apiService.regenerateChildCode("Bearer $idToken", childId).toResult().map { data ->
                val newCode = data.newCode
                // 재발급된 새 코드로 로컬 캐시(JSON) 업데이트
                val currentList = observeCachedParentChildren().first()
                val updatedList = currentList.map {
                    if (it.childId == childId) it.copy(code = newCode) else it
                }
                sessionManager.saveParentChildrenJson(gson.toJson(updatedList))

                newCode
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

    override suspend fun getChildSummary(childId: String): Result<ParentChildSummaryResponseData> = try {
        val idToken = requireParentIdToken()
        apiService.getParentChildSummary("Bearer $idToken", childId).toResult()
    } catch (e: HttpException) {
        Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
    } catch (e: IOException) {
        Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getWeeklyStats(childId: String): Result<ParentWeeklyStatsResponseData> = try {
        val idToken = requireParentIdToken()
        apiService.getParentChildWeeklyStats("Bearer $idToken", childId).toResult()
    } catch (e: HttpException) {
        Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
    } catch (e: IOException) {
        Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getPrivacyLog(childId: String, page: Int, size: Int): Result<ParentPrivacyLogResponseData> = try {
        val idToken = requireParentIdToken()
        apiService.getParentChildPrivacyLog("Bearer $idToken", childId, page = page, size = size).toResult()
    } catch (e: HttpException) {
        Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
    } catch (e: IOException) {
        Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getWeakPoints(childId: String, page: Int, size: Int): Result<ParentWeakPointsResponseData> = try {
        val idToken = requireParentIdToken()
        apiService.getParentChildWeakPoints("Bearer $idToken", childId, page = page, size = size).toResult()
    } catch (e: HttpException) {
        Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
    } catch (e: IOException) {
        Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteParentFcmToken(firebaseIdToken: String): Result<Unit> {
        return try {
            apiService.deleteParentFcmToken("Bearer ${firebaseIdToken.trim()}").toResult()
            Result.success(Unit)
        } catch (_: HttpException) {
            Result.success(Unit)
        } catch (_: IOException) {
            Result.success(Unit)
        } catch (_: Exception) {
            Result.success(Unit)
        }
    }

    override suspend fun getParentMe(): Result<ParentMeData> = try {
        val idToken = requireParentIdToken()
        apiService.getParentMe("Bearer $idToken").toResult()
    } catch (e: HttpException) {
        Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
    } catch (e: IOException) {
        Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun addParentChild(nickname: String): Result<ParentRegisterResponse> {
        return try {
            val idToken = requireParentIdToken()
            val result = apiService.addParentChild(
                "Bearer $idToken",
                ParentRegisterRequest(nickname = nickname.trim())
            ).toResult()
            if (result.isSuccess) {
                syncParentChildren()
            }
            result
        } catch (e: HttpException) {
            Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: IOException) {
            Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getParentChildDetail(childId: String): Result<ParentChildDetailData> = try {
        val idToken = requireParentIdToken()
        apiService.getParentChildDetail("Bearer $idToken", childId).toResult()
    } catch (e: HttpException) {
        Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
    } catch (e: IOException) {
        Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun patchParentChild(
        childId: String,
        body: PatchParentChildRequest
    ): Result<ParentChildPatchResponseData> = try {
        val idToken = requireParentIdToken()
        val result = apiService.patchParentChild("Bearer $idToken", childId, body).toResult()
        if (result.isSuccess) {
            syncParentChildren()
        }
        result
    } catch (e: HttpException) {
        Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
    } catch (e: IOException) {
        Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteParentChild(childId: String): Result<Unit> {
        return try {
            val idToken = requireParentIdToken()
            apiService.deleteParentChild("Bearer $idToken", childId).toResult()
            syncParentChildren()
            Result.success(Unit)
        } catch (e: HttpException) {
            Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: IOException) {
            Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteParentAccount(firebaseIdToken: String): Result<Unit> {
        return try {
            apiService.deleteParentAccount(
                "Bearer ${firebaseIdToken.trim()}",
                ParentAccountDeleteRequest(confirm = true)
            ).toResult()
            Result.success(Unit)
        } catch (e: HttpException) {
            Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e)))
        } catch (e: IOException) {
            Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
