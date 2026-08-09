package com.kduniv.aimong.feature.settings.data

import com.kduniv.aimong.core.network.model.NotificationSettingsData
import com.kduniv.aimong.core.network.model.NotificationSettingsPatchRequest

interface NotificationSettingsRepository {
    suspend fun getSettings(): Result<NotificationSettingsData>
    suspend fun patchSettings(patch: NotificationSettingsPatchRequest): Result<NotificationSettingsData>
}

