package com.aimong.backend.domain.stagereward.dto;

import java.time.Instant;
import java.util.UUID;

public record StageRewardResponse(
        UUID rewardId,
        int stageNumber,
        String rewardText,
        boolean isTriggered,
        Instant triggeredAt,
        int defaultGearReward,
        int normalTicketReward,
        MissionProgressResponse missionProgress
) {
    public record MissionProgressResponse(
            long completed,
            long total
    ) {
    }
}
