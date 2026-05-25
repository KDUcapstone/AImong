package com.aimong.backend.domain.stagereward.dto;

import java.time.Instant;

public record StageCompletionRewardResponse(
        int stageNumber,
        String rewardText,
        int defaultGearReward,
        int normalTicketReward,
        Instant triggeredAt
) {
}
