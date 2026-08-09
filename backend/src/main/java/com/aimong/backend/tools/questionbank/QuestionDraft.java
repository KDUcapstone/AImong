package com.aimong.backend.tools.questionbank;

import java.util.List;

public record QuestionDraft(
        String externalId,
        String missionCode,
        short stage,
        String missionTitle,
        String type,
        String question,
        List<String> options,
        Object answer,
        String explanation,
        List<String> contentTags,
        String sourceType,
        String sourceReference
) {
}
