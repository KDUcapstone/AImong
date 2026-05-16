package com.kduniv.aimong.feature.home.data

import com.kduniv.aimong.core.network.AimongApiService
import com.kduniv.aimong.core.network.ApiErrorMapper
import com.kduniv.aimong.core.network.toResult
import com.kduniv.aimong.feature.home.data.model.BootstrapResponseData
import com.kduniv.aimong.feature.home.domain.repository.AppBootstrapRepository
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class AppBootstrapRepositoryImpl @Inject constructor(
    private val apiService: AimongApiService
) : AppBootstrapRepository {

    @Volatile
    private var cached: BootstrapResponseData? = null

    override fun lastBootstrap(): BootstrapResponseData? = cached

    override suspend fun getBootstrap(): Result<BootstrapResponseData> {
        return try {
            apiService.getBootstrap().toResult().also { result ->
                result.onSuccess { cached = it }
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
