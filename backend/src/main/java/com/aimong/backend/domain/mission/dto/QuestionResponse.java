package com.aimong.backend.domain.mission.dto;

import java.util.List;
import java.util.UUID;

public record QuestionResponse(
        UUID id,
        String type,
        String question,
        List<String> options
) {
}
