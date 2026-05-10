package com.aimong.backend.domain.mission.dto;

import java.util.List;
import java.util.UUID;

public record MissionListResponse(
        List<StageResponse> stages,
        ProgressResponse progress
) {
    public static String labelForStar(int starLevel) {
        return switch (starLevel) {
            case 1 -> "쉬움";
            case 2 -> "보통";
            case 3 -> "어려움";
            default -> "알 수 없음";
        };
    }

    public record StageResponse(
            int stage,
            String title,
            List<MissionResponse> missions
    ) {
    }

    public record MissionResponse(
            UUID missionId,
            String missionCode,
            String title,
            String description,
            boolean isUnlocked,
            List<StarLevelResponse> starLevels,
            int stage
    ) {
    }

    public record StarLevelResponse(
            int starLevel,
            String label,
            long totalSetCount,
            long completedSetCount,
            boolean isPlayable,
            boolean isReviewable
    ) {
    }

    public record ProgressResponse(
            long completedSetCount,
            long totalSetCount,
            int currentStarLevel
    ) {
    }
}
