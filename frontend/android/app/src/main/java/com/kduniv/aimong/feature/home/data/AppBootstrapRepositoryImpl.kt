package com.kduniv.aimong.feature.home.data

import com.kduniv.aimong.core.network.AimongApiService
import com.kduniv.aimong.core.network.runApiCall
import com.kduniv.aimong.core.network.toResult
import com.kduniv.aimong.feature.home.data.model.BootstrapResponseData
import com.kduniv.aimong.feature.home.domain.repository.AppBootstrapRepository
import javax.inject.Inject

class AppBootstrapRepositoryImpl @Inject constructor(
    private val apiService: AimongApiService
) : AppBootstrapRepository {

    @Volatile
    private var cached: BootstrapResponseData? = null

    override fun lastBootstrap(): BootstrapResponseData? = cached

    override suspend fun getBootstrap(): Result<BootstrapResponseData> =
        runApiCall {
            apiService.getBootstrap().toResult().getOrThrow()
        }.also { result ->
            result.onSuccess { cached = it }
        }
}
