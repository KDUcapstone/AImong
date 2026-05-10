package com.aimong.backend.domain.parent.dto;

import java.util.UUID;

public record ParentWeakPointResponse(
        UUID missionId,
        String missionTitle,
        short stage,
        double incorrectRate,
        long attemptCount,
        String setId,
        Integer starLevel
) {
    public ParentWeakPointResponse(
            UUID missionId,
            String missionTitle,
            short stage,
            double incorrectRate,
            long attemptCount
    ) {
        this(missionId, missionTitle, stage, incorrectRate, attemptCount, null, null);
    }
}
