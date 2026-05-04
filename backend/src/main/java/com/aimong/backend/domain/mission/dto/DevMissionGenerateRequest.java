package com.aimong.backend.domain.mission.dto;

import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.QuestionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DevMissionGenerateRequest(
        @NotNull(message = "missionId is required")
        UUID missionId,
        DifficultyBand difficultyBand,
        QuestionType type,
        @Min(value = 1, message = "count must be at least 1")
        @Max(value = 3, message = "count must be 3 or less")
        Integer count,
        @Min(value = 1, message = "packNo must be at least 1")
        @Max(value = 6, message = "packNo must be 6 or less")
        Integer packNo,
        Boolean persist
) {
}
