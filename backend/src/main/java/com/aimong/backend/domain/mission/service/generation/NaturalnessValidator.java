package com.aimong.backend.domain.mission.service.generation;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NaturalnessValidator {

    public ValidationSubResult validate(StructuredQuestionSchema candidate) {
        List<String> hardFails = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> repairHints = new ArrayList<>();

        String question = candidate.question() == null ? "" : candidate.question().trim();
        String explanation = candidate.explanation() == null ? "" : candidate.explanation().trim();

        if (!question.isBlank()
                && !question.endsWith("?")
                && !question.endsWith("!")
                && !question.endsWith(".")) {
            warnings.add("naturalness.unfinished_stem_ending");
            repairHints.add("Make the stem end like a natural student-facing question.");
        }
        if (question.contains("  ") || explanation.contains("  ")) {
            warnings.add("naturalness.irregular_spacing");
        }
        if (hasRepeatedPhrase(question)) {
            warnings.add("naturalness.repetitive_stem");
            repairHints.add("Rewrite the stem to avoid repetitive phrasing.");
        }
        if (candidate.options() != null && candidate.options().stream().allMatch(option -> option.startsWith("AI"))) {
            warnings.add("naturalness.options_too_patterned");
            repairHints.add("Vary option openings so they read like natural alternatives.");
        }
        if (candidate.options() != null) {
            int min = candidate.options().stream().mapToInt(String::length).min().orElse(0);
            int max = candidate.options().stream().mapToInt(String::length).max().orElse(0);
            if (min > 0 && max >= min * 3) {
                warnings.add("naturalness.option_tone_imbalance");
            }
        }
        if (!explanation.isBlank() && ValidationTextUtils.tokenJaccard(question, explanation) > 0.8d) {
            warnings.add("naturalness.explanation_too_close_to_stem");
            repairHints.add("Make the explanation sound like a reason, not a restatement.");
        }

        int score = hardFails.isEmpty() ? Math.max(0, 100 - warnings.size() * 12) : 0;
        return new ValidationSubResult(score, List.copyOf(hardFails), List.copyOf(warnings), List.copyOf(repairHints));
    }

    private boolean hasRepeatedPhrase(String text) {
        String normalized = ValidationTextUtils.normalize(text);
        if (normalized.isBlank()) {
            return false;
        }
        String[] tokens = normalized.split("\\s+");
        for (int index = 0; index < tokens.length - 2; index++) {
            String phrase = tokens[index] + " " + tokens[index + 1];
            int count = 0;
            for (int cursor = 0; cursor < tokens.length - 1; cursor++) {
                String other = tokens[cursor] + " " + tokens[cursor + 1];
                if (phrase.equals(other)) {
                    count++;
                }
            }
            if (count >= 3) {
                return true;
            }
        }
        return false;
    }
}
