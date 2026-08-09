package com.kduniv.aimong.core.privacy

interface PrivacyRepository {
    suspend fun reportEvent(detectedType: String, masked: Boolean)
}
