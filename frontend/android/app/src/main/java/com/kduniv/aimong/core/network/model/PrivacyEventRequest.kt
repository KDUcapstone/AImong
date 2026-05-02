package com.kduniv.aimong.core.network.model

import com.kduniv.aimong.core.privacy.PrivacyType

data class PrivacyEventRequest(
    val detectedType: PrivacyType,
    val isMasked: Boolean,
    val timestamp: Long = System.currentTimeMillis()
    // 주의: 보안 원칙에 따라 원문 텍스트는 절대 포함하지 않음
)
