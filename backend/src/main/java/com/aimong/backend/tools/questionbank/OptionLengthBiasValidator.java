package com.aimong.backend.tools.questionbank;

import com.aimong.backend.domain.mission.entity.QuestionType;
import java.util.List;

public final class OptionLengthBiasValidator {

    public OptionLengthBiasReport validate(List<AuditQuestion> questions) {
        int evaluatedCount = 0;
        int correctOptionUniqueLongestCount = 0;
        int strongOptionLengthBiasCount = 0;
        double varianceSum = 0d;
        int styleImbalanceWarnings = 0;

        for (AuditQuestion question : questions) {
            if (question.type() != QuestionType.MULTIPLE && question.type() != QuestionType.SITUATION) {
                continue;
            }
            if (question.options() == null || question.options().isEmpty() || !(question.answer() instanceof Integer answerIndex)) {
                continue;
            }
            evaluatedCount++;
            List<Integer> lengths = question.options().stream().map(String::length).toList();
            int max = lengths.stream().mapToInt(Integer::intValue).max().orElse(0);
            long longestCount = lengths.stream().filter(length -> length == max).count();
            int correctLength = lengths.get(answerIndex);
            if (correctLength == max && longestCount == 1) {
                correctOptionUniqueLongestCount++;
                int secondLongest = lengths.stream()
                        .filter(length -> length != max)
                        .mapToInt(Integer::intValue)
                        .max()
                        .orElse(0);
                if (secondLongest > 0 && correctLength >= Math.ceil(secondLongest * 1.3d)) {
                    strongOptionLengthBiasCount++;
                }
            }
            double variance = variance(lengths);
            varianceSum += variance;
            if (variance >= 40d || hasStyleImbalance(question.options())) {
                styleImbalanceWarnings++;
            }
        }

        return new OptionLengthBiasReport(
                evaluatedCount,
                correctOptionUniqueLongestCount,
                strongOptionLengthBiasCount,
                evaluatedCount == 0 ? 0d : varianceSum / evaluatedCount,
                styleImbalanceWarnings
        );
    }

    private boolean hasStyleImbalance(List<String> options) {
        long distinctOpenings = options.stream()
                .map(option -> option == null || option.isBlank() ? "" : option.trim().split("\\s+")[0])
                .distinct()
                .count();
        return distinctOpenings <= 2;
    }

    private double variance(List<Integer> values) {
        if (values.isEmpty()) {
            return 0d;
        }
        double mean = values.stream().mapToInt(Integer::intValue).average().orElse(0d);
        return values.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .average()
                .orElse(0d);
    }

    public record OptionLengthBiasReport(
            int evaluatedCount,
            int correctOptionUniqueLongestCount,
            int strongOptionLengthBiasCount,
            double averageOptionLengthVariance,
            int answerOptionStyleImbalanceWarnings
    ) {
    }
}
