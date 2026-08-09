package com.aimong.backend.tools.questionbank;

import java.util.List;

public record QuestionBankDraft(
        String sourceTitle,
        String sourceReference,
        int totalQuestionCount,
        List<QuestionDraft> questions
) {
}
