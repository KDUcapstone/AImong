package com.aimong.backend.domain.parent.dto;

import java.time.Instant;
import java.util.List;

public record ParentPrivacyLogResponse(
        int page,
        int size,
        long totalCount,
        boolean hasNext,
        long weeklyCount,
        List<PrivacyEventResponse> events
) {

    public record PrivacyEventResponse(
            String detectedType,
            boolean masked,
            Instant detectedAt
    ) {
    }
}
