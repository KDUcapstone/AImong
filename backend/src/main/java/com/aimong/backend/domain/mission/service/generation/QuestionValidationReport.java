package com.aimong.backend.domain.mission.service.generation;

import java.util.List;

public record QuestionValidationReport(
        boolean pass,
        ValidationDecision recommendedAction,
        List<String> hardFailReasons,
        List<String> softWarnings,
        QuestionValidationScores scores,
        NormalizedQuestionView normalizedQuestion,
        List<String> repairHints
) {
}
