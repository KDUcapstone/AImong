package com.aimong.backend.domain.stagereward.dto;

import jakarta.validation.constraints.Size;

public record UpdateStageRewardRequest(
        @Size(max = 100, message = "rewardText must be 100 characters or less")
        String rewardText
) {
}
