package com.aimong.backend.domain.mission.dto;

import jakarta.validation.constraints.NotBlank;

public record QuestionReportRequest(
        @NotBlank String reasonCode,
        String detail
) {
}
