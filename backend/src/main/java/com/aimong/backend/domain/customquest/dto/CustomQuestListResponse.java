package com.aimong.backend.domain.customquest.dto;

import java.util.List;

public record CustomQuestListResponse(
        int page,
        int size,
        long totalCount,
        boolean hasNext,
        List<CustomQuestItemResponse> quests
) {
}
