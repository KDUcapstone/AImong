package com.aimong.backend.domain.mission.service.generation;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ExplanationQualityValidator {

    public ValidationSubResult validate(StructuredQuestionSchema candidate) {
        List<String> hardFails = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> repairHints = new ArrayList<>();

        String explanation = candidate.explanation() == null ? "" : candidate.explanation().trim();
        long sentenceCount = ValidationTextUtils.sentenceCount(explanation);

        if (sentenceCount > 2) {
            hardFails.add("explanation.too_many_sentences");
            repairHints.add("Keep the explanation within two short sentences.");
        }
        if (!explanation.isBlank() && ValidationTextUtils.normalize(explanation).equals(ValidationTextUtils.normalize(candidate.question()))) {
            hardFails.add("explanation.repeats_question_only");
        }
        if (explanation.contains("혼나") || explanation.contains("틀렸어") || explanation.contains("반드시 외워")) {
            hardFails.add("explanation.scolding_tone");
        }
        if (explanation.length() < 8) {
            warnings.add("explanation.too_short");
        }
        if (!explanation.isBlank() && !containsReasoningCue(explanation)) {
            warnings.add("explanation.weak_reasoning_signal");
            repairHints.add("Explain why the answer is correct, not only what the answer is.");
        }

        int score = hardFails.isEmpty() ? Math.max(0, 100 - warnings.size() * 12) : 0;
        return new ValidationSubResult(score, List.copyOf(hardFails), List.copyOf(warnings), List.copyOf(repairHints));
    }

    private boolean containsReasoningCue(String explanation) {
        return List.of("때문", "그래서", "왜냐", "도와", "맞는 이유", "틀린 이유").stream()
                .anyMatch(explanation::contains);
    }
}
