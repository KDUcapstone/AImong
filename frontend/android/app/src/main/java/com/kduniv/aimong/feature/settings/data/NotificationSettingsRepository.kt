package com.kduniv.aimong.feature.settings.data

import com.kduniv.aimong.core.network.model.NotificationSettingsRequest
import com.kduniv.aimong.core.network.model.NotificationSettingsResponseData

interface NotificationSettingsRepository {
    suspend fun getSettings(): Result<NotificationSettingsResponseData>
    suspend fun patchSettings(request: NotificationSettingsRequest): Result<NotificationSettingsResponseData>
}

