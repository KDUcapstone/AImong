package com.aimong.backend.domain.reward.dto;

public record ReturnRewardResponse(
        boolean hasReward,
        Long daysMissed,
        Integer ticketCount,
        String message
) {

    public static ReturnRewardResponse noReward() {
        return new ReturnRewardResponse(false, null, null, null);
    }

    public static ReturnRewardResponse hasReward(long daysMissed, int ticketCount) {
        return new ReturnRewardResponse(
                true,
                daysMissed,
                ticketCount,
                daysMissed + " days since your last mission. Here are " + ticketCount + " normal tickets."
        );
    }
}
