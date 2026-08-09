package com.kduniv.aimong.feature.home.domain.repository

import com.kduniv.aimong.feature.home.data.model.BootstrapResponseData

interface AppBootstrapRepository {
    suspend fun getBootstrap(): Result<BootstrapResponseData>

    /** 마지막 성공한 [getBootstrap] 응답 (실패·미호출 시 null) */
    fun lastBootstrap(): BootstrapResponseData?
}
