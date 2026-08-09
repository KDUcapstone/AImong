package com.aimong.backend.tools.questionbank;

import java.util.List;

public record AuditQuestionBank(
        String sourceTitle,
        String sourceReference,
        String generationVersion,
        int totalMissionCount,
        int totalQuestionCount,
        List<AuditQuestion> questions
) {
}
