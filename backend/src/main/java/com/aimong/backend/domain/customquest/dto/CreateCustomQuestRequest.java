package com.aimong.backend.domain.customquest.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateCustomQuestRequest(
        @NotBlank(message = "title is required")
        @Size(max = 50, message = "title must be 50 characters or less")
        String title,

        @Size(max = 200, message = "description must be 200 characters or less")
        String description,

        @NotBlank(message = "rewardText is required")
        @Size(max = 100, message = "rewardText must be 100 characters or less")
        String rewardText,

        @NotNull(message = "expiresAt is required")
        @Future(message = "expiresAt must be in the future")
        Instant expiresAt
) {
}
