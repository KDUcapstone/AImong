package com.aimong.backend.domain.stagereward.dto;

import java.util.List;

public record StageRewardListResponse(
        List<StageRewardResponse> stages
) {
}
