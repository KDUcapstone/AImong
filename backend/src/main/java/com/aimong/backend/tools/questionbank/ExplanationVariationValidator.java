package com.aimong.backend.tools.questionbank;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class ExplanationVariationValidator {

    public ExplanationVariationReport validate(List<AuditQuestion> questions) {
        Map<String, Long> normalizedCounts = questions.stream()
                .collect(Collectors.groupingBy(
                        question -> normalize(question.explanation()),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
        Map<String, Long> suffixCounts = questions.stream()
                .collect(Collectors.groupingBy(
                        question -> suffix(normalize(question.explanation())),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        long repeatedExplanationSuffixPatternCount = suffixCounts.values().stream()
                .filter(count -> count >= 8)
                .count();
        List<String> overusedExplanationEndings = suffixCounts.entrySet().stream()
                .filter(entry -> entry.getValue() >= 8)
                .map(Map.Entry::getKey)
                .toList();

        return new ExplanationVariationReport(
                normalizedCounts.size(),
                repeatedExplanationSuffixPatternCount,
                overusedExplanationEndings
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\s가-힣]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String suffix(String normalized) {
        if (normalized == null || normalized.isBlank()) {
            return "";
        }
        String[] tokens = normalized.split("\\s+");
        if (tokens.length <= 4) {
            return normalized;
        }
        return String.join(" ", java.util.Arrays.copyOfRange(tokens, tokens.length - 4, tokens.length));
    }

    public record ExplanationVariationReport(
            int explanationUniqueCount,
            long repeatedExplanationSuffixPatternCount,
            List<String> overusedExplanationEndings
    ) {
    }
}
