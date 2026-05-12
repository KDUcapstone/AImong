package com.kduniv.aimong.feature.home.domain.repository

import com.kduniv.aimong.feature.home.data.model.BootstrapResponseData

interface AppBootstrapRepository {
    suspend fun getBootstrap(): Result<BootstrapResponseData>
}
