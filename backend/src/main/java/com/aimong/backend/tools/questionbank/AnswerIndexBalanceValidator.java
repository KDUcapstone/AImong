package com.aimong.backend.tools.questionbank;

import com.aimong.backend.domain.mission.entity.QuestionType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AnswerIndexBalanceValidator {

    public AnswerIndexBalanceReport validate(List<AuditQuestion> questions) {
        Map<Integer, Integer> multiple = new LinkedHashMap<>();
        Map<Integer, Integer> situation = new LinkedHashMap<>();
        Map<Integer, Integer> combined = new LinkedHashMap<>();
        for (int index = 0; index < 4; index++) {
            multiple.put(index, 0);
            situation.put(index, 0);
            combined.put(index, 0);
        }

        for (AuditQuestion question : questions) {
            if (!(question.answer() instanceof Integer answerIndex)) {
                continue;
            }
            if (question.type() == QuestionType.MULTIPLE) {
                multiple.computeIfPresent(answerIndex, (key, count) -> count + 1);
                combined.computeIfPresent(answerIndex, (key, count) -> count + 1);
            }
            if (question.type() == QuestionType.SITUATION) {
                situation.computeIfPresent(answerIndex, (key, count) -> count + 1);
                combined.computeIfPresent(answerIndex, (key, count) -> count + 1);
            }
        }

        return new AnswerIndexBalanceReport(
                multiple,
                situation,
                combined,
                ratio(multiple),
                ratio(situation),
                ratio(combined)
        );
    }

    private double ratio(Map<Integer, Integer> distribution) {
        int total = distribution.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) {
            return 0d;
        }
        if (distribution.values().stream().anyMatch(value -> value == 0)) {
            return Double.POSITIVE_INFINITY;
        }
        int max = distribution.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        int min = distribution.values().stream().mapToInt(Integer::intValue).min().orElse(0);
        return max / (double) min;
    }

    public record AnswerIndexBalanceReport(
            Map<Integer, Integer> multipleDistribution,
            Map<Integer, Integer> situationDistribution,
            Map<Integer, Integer> combinedDistribution,
            double multipleMaxMinRatio,
            double situationMaxMinRatio,
            double combinedMaxMinRatio
    ) {
    }
}
