package com.aimong.backend.domain.mission.dto;

import java.util.UUID;

public record QuestionReportResponse(
        UUID questionId,
        UUID issueId,
        String issueStatus,
        boolean quarantined
) {
}
