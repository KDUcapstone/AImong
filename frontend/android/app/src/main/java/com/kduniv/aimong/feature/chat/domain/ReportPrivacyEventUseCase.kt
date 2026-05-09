package com.kduniv.aimong.feature.chat.domain

import com.kduniv.aimong.core.privacy.PrivacyRepository
import javax.inject.Inject

class ReportPrivacyEventUseCase @Inject constructor(
    private val privacyRepository: PrivacyRepository
) {
    suspend operator fun invoke(detectedType: String, masked: Boolean) {
        privacyRepository.reportEvent(detectedType, masked)
    }
}
