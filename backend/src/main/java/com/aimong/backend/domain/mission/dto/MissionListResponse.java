package com.aimong.backend.domain.mission.dto;

import java.util.List;

public record MissionListResponse(
        List<MissionSummaryResponse> missions,
        StageProgressResponse stageProgress
) {
}
