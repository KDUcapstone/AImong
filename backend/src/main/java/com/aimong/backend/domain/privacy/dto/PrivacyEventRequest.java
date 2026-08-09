package com.aimong.backend.domain.privacy.dto;

import com.aimong.backend.domain.privacy.entity.PrivacyDetectedType;
import jakarta.validation.constraints.NotNull;

public record PrivacyEventRequest(
        @NotNull(message = "detectedType is required")
        PrivacyDetectedType detectedType,
        boolean masked
) {
}
