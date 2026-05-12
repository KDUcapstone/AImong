package com.kduniv.aimong.feature.auth.data

import com.kduniv.aimong.core.network.AimongApiService
import com.kduniv.aimong.core.network.ApiErrorMapper
import com.kduniv.aimong.core.network.toResult
import com.kduniv.aimong.core.network.model.ChildLoginRequest
import com.kduniv.aimong.core.network.model.ChildLoginResponse
import com.kduniv.aimong.core.network.model.ChildMeData
import com.kduniv.aimong.core.network.model.ParentFcmTokenRequest
import com.kduniv.aimong.core.network.model.ParentRegisterRequest
import com.kduniv.aimong.core.network.model.ParentRegisterResponse
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: AimongApiService
) : AuthRepository {

    override suspend fun registerParentChild(
        nickname: String,
        firebaseIdToken: String
    ): Result<ParentRegisterResponse> {
        return try {
            apiService.parentRegister(
                authorization = "Bearer $firebaseIdToken",
                body = ParentRegisterRequest(nickname = nickname.trim())
            ).toResult()
        } catch (e: HttpException) {
            Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e), e))
        } catch (e: IOException) {
            Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loginChild(code: String): Result<ChildLoginResponse> {
        return try {
            apiService.childLogin(ChildLoginRequest(code = code.trim())).toResult()
        } catch (e: HttpException) {
            Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e), e))
        } catch (e: IOException) {
            Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun registerParentFcmToken(firebaseIdToken: String, fcmToken: String): Result<Unit> {
        return try {
            val response = apiService.registerParentFcmToken(
                authorization = "Bearer ${firebaseIdToken.trim()}",
                body = ParentFcmTokenRequest(fcmToken = fcmToken.trim())
            )
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.success(Unit)
            }
        } catch (_: HttpException) {
            Result.success(Unit)
        } catch (_: IOException) {
            Result.success(Unit)
        } catch (_: Exception) {
            Result.success(Unit)
        }
    }

    override suspend fun registerChildFcmToken(fcmToken: String): Result<Unit> {
        return try {
            apiService.registerChildFcmToken(
                body = ParentFcmTokenRequest(fcmToken = fcmToken.trim())
            )
            Result.success(Unit)
        } catch (_: HttpException) {
            Result.success(Unit)
        } catch (_: IOException) {
            Result.success(Unit)
        } catch (_: Exception) {
            Result.success(Unit)
        }
    }

    override suspend fun getChildMe(): Result<ChildMeData> {
        return try {
            apiService.getChildMe().toResult()
        } catch (e: HttpException) {
            Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e), e))
        } catch (e: IOException) {
            Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun childLogout(): Result<Unit> {
        return try {
            apiService.childLogout().toResult()
            Result.success(Unit)
        } catch (e: HttpException) {
            Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e), e))
        } catch (e: IOException) {
            Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteChildFcmToken(): Result<Unit> {
        return try {
            apiService.deleteChildFcmToken().toResult()
            Result.success(Unit)
        } catch (e: HttpException) {
            Result.failure(Exception(ApiErrorMapper.userMessageForHttpException(e), e))
        } catch (e: IOException) {
            Result.failure(Exception("연결을 확인한 뒤 다시 시도해주세요."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
