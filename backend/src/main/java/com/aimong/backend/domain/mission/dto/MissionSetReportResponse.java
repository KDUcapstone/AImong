package com.aimong.backend.domain.mission.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MissionSetReportResponse(
        String setId,
        UUID missionId,
        int starLevel,
        int variantNo,
        boolean completed,
        Integer bestScore,
        Integer total,
        UUID firstAttemptId,
        Instant completedAt,
        List<ResultResponse> results
) {
    public record ResultResponse(
            UUID questionId,
            boolean isCorrect
    ) {
    }
}
