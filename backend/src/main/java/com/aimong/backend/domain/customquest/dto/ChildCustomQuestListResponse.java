package com.aimong.backend.domain.customquest.dto;

import java.util.List;

public record ChildCustomQuestListResponse(
        List<CustomQuestItemResponse> quests,
        boolean hasPendingConfirm
) {
}
