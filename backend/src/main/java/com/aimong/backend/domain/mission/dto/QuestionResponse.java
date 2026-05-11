package com.aimong.backend.domain.mission.dto;

import java.util.List;
import java.util.UUID;

public record QuestionResponse(
        UUID questionId,
        int questionNo,
        String type,
        String difficulty,
        String prompt,
        List<String> choices,
        String answerFormat
) {
    public QuestionResponse(UUID questionId, String type, String prompt, List<String> choices) {
        this(questionId, 1, type, null, prompt, choices, answerFormatFor(choices));
    }

    public static String answerFormatFor(List<String> choices) {
        return choices == null || choices.isEmpty() ? "TEXT" : "SINGLE_CHOICE";
    }
}
