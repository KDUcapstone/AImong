package com.aimong.backend.domain.mission.service.generation;

import java.util.List;

public record NormalizedQuestionView(
        String missionCode,
        String type,
        String question,
        List<String> options,
        Object answer,
        String explanation,
        List<String> contentTags,
        int difficulty
) {
}
