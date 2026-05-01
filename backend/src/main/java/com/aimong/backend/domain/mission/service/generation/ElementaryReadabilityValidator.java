package com.aimong.backend.domain.mission.service.generation;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ElementaryReadabilityValidator {

    private static final List<String> COMPLEX_TERMS = List.of(
            "거버넌스", "프레임워크", "알고리즘 최적화", "메타윤리", "이해관계자 매트릭스", "산업 생태계"
    );

    public ReadabilityResult validate(StructuredQuestionSchema candidate) {
        List<String> hardFails = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> repairHints = new ArrayList<>();

        String question = safe(candidate.question());
        String explanation = safe(candidate.explanation());
        int longestSentence = ValidationTextUtils.longestSentenceLength(question);

        if (longestSentence > 90) {
            hardFails.add("readability.sentence_too_long");
            repairHints.add("Split the stem into shorter sentences for grade 5-6 readers.");
        } else if (longestSentence > 55) {
            warnings.add("readability.long_sentence");
            repairHints.add("Shorten the stem and remove extra clauses.");
        }
        if (containsAny(question + " " + explanation, COMPLEX_TERMS)) {
            hardFails.add("readability.too_abstract_or_technical");
        }
        if (question.contains("않지 않") || question.contains("없지 않")) {
            hardFails.add("readability.double_negative");
        }
        if (candidate.options() != null && !candidate.options().isEmpty()) {
            int min = candidate.options().stream().mapToInt(option -> safe(option).length()).min().orElse(0);
            int max = candidate.options().stream().mapToInt(option -> safe(option).length()).max().orElse(0);
            if (min > 0 && max >= min * 3) {
                warnings.add("readability.option_length_imbalance");
                repairHints.add("Balance option length and tone so the answer is not obvious.");
            }
        }
        if (question.contains("항상") || question.contains("절대") || question.contains("무조건")) {
            warnings.add("readability.absolute_trap_language");
        }

        int score = hardFails.isEmpty() ? Math.max(0, 100 - warnings.size() * 10) : 0;
        return new ReadabilityResult(score, List.copyOf(hardFails), List.copyOf(warnings), List.copyOf(repairHints));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean containsAny(String text, List<String> keywords) {
        String normalized = ValidationTextUtils.normalize(text);
        return keywords.stream()
                .map(ValidationTextUtils::normalize)
                .anyMatch(normalized::contains);
    }

    public record ReadabilityResult(
            int score,
            List<String> hardFailReasons,
            List<String> softWarnings,
            List<String> repairHints
    ) {
    }
}
