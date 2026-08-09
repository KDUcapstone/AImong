package com.aimong.backend.domain.quest.dto;

public record QuestItemResponse(
        String questType,
        String label,
        String reward,
        String claimType,
        boolean completed,
        boolean rewardClaimed,
        ProgressResponse progress
) {
}
