package com.kduniv.aimong.core.privacy

import android.util.Log
import com.kduniv.aimong.core.network.AimongApiService
import com.kduniv.aimong.core.network.model.PrivacyEventRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrivacyRepositoryImpl @Inject constructor(
    private val api: AimongApiService
) : PrivacyRepository {

    override suspend fun reportEvent(detectedType: String, masked: Boolean) {
        try {
            val res = api.reportPrivacyEvent(
                PrivacyEventRequest(detectedType = detectedType, masked = masked)
            )
            if (!res.success) {
                Log.w(TAG, "privacy/event: ${res.error?.message ?: res.error?.code ?: "unknown"}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "privacy/event failed", e)
        }
    }

    private companion object {
        private const val TAG = "PrivacyRepository"
    }
}
