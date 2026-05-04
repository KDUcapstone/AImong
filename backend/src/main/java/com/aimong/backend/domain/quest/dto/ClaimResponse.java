package com.aimong.backend.domain.quest.dto;

import java.util.List;

public record ClaimResponse(
        List<RewardResponse> rewards,
        RemainingTicketsResponse remainingTickets
) {
    public record RewardResponse(
            String type,
            String ticketType,
            int count,
            String reason
    ) {
    }

    public record RemainingTicketsResponse(
            int normal,
            int rare,
            int epic
    ) {
    }
}
