package com.aimong.backend.domain.mission.dto;

public record StageProgressResponse(
        long stage1Completed,
        long stage2Completed,
        long stage3Completed
) {
}
