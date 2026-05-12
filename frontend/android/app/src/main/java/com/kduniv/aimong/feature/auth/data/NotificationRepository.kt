package com.kduniv.aimong.feature.auth.data

import com.kduniv.aimong.core.network.model.NotificationSettingsData
import com.kduniv.aimong.core.network.model.NotificationSettingsPatchRequest

interface NotificationRepository {
    suspend fun getSettings(): Result<NotificationSettingsData>
    suspend fun patchSettings(patch: NotificationSettingsPatchRequest): Result<NotificationSettingsData>
}
