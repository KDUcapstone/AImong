package com.aimong.backend.domain.reward.dto;

import java.util.List;

public record ReturnRewardClaimResponse(
        List<RewardItem> rewards,
        RemainingTickets remainingTickets
) {

    public record RewardItem(
            String type,
            String ticketType,
            int count,
            String reason
    ) {
    }

    public record RemainingTickets(
            int normal
    ) {
    }
}
