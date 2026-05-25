package com.aimong.backend.domain.stagereward.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateStageRewardRequest(
        @NotNull(message = "stageNumber is required")
        @Min(value = 1, message = "stageNumber must be 1, 2, or 3")
        @Max(value = 3, message = "stageNumber must be 1, 2, or 3")
        Integer stageNumber,

        @Size(max = 100, message = "rewardText must be 100 characters or less")
        String rewardText
) {
}
