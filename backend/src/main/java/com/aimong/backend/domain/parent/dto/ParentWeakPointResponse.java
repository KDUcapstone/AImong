package com.aimong.backend.domain.parent.dto;

import java.util.UUID;

public record ParentWeakPointResponse(
        UUID missionId,
        String missionTitle,
        short stage,
        double incorrectRate,
        long attemptCount
) {
}
