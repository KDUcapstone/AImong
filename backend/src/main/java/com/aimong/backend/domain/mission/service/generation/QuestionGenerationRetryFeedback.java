package com.aimong.backend.domain.mission.service.generation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public record QuestionGenerationRetryFeedback(
        boolean wordingQualityWeak,
        boolean highDuplicateRisk,
        boolean optionQualityWeak,
        boolean explanationQualityWeak,
        List<String> repairHints
) {

    private static final int MAX_REPAIR_HINTS = 6;

    public static QuestionGenerationRetryFeedback empty() {
        return new QuestionGenerationRetryFeedback(false, false, false, false, List.of());
    }

    public static QuestionGenerationRetryFeedback fromRejected(List<QuestionGenerationService.RejectedCandidate> rejected) {
        boolean wordingWeak = false;
        boolean duplicateRisk = false;
        boolean optionWeak = false;
        boolean explanationWeak = false;
        Set<String> hints = new LinkedHashSet<>();

        for (QuestionGenerationService.RejectedCandidate rejectedCandidate : rejected) {
            QuestionValidationReport report = rejectedCandidate.report();
            QuestionValidationScores scores = report.scores();
            wordingWeak = wordingWeak
                    || scores.elementaryReadability() < 80
                    || scores.naturalness() < 80
                    || containsAny(report, "natural", "korean", "sentence", "shorten", "particle", "wording");
            duplicateRisk = duplicateRisk
                    || scores.duplicationOriginality() < 75
                    || containsAny(report, "duplicate", "copy", "similar", "semantic");
            optionWeak = optionWeak
                    || scores.answerClarity() < 85
                    || scores.distractorQuality() < 85
                    || containsAny(report, "option", "distractor", "answer");
            explanationWeak = explanationWeak
                    || scores.explanationQuality() < 80
                    || containsAny(report, "explanation");
            hints.addAll(report.repairHints());
        }

        List<String> limitedHints = new ArrayList<>(hints).stream()
                .limit(MAX_REPAIR_HINTS)
                .toList();
        return new QuestionGenerationRetryFeedback(
                wordingWeak,
                duplicateRisk,
                optionWeak,
                explanationWeak,
                limitedHints
        );
    }

    public QuestionGenerationRetryFeedback merge(QuestionGenerationRetryFeedback other) {
        Set<String> mergedHints = new LinkedHashSet<>(repairHints);
        mergedHints.addAll(other.repairHints());
        return new QuestionGenerationRetryFeedback(
                wordingQualityWeak || other.wordingQualityWeak(),
                highDuplicateRisk || other.highDuplicateRisk(),
                optionQualityWeak || other.optionQualityWeak(),
                explanationQualityWeak || other.explanationQualityWeak(),
                new ArrayList<>(mergedHints).stream().limit(MAX_REPAIR_HINTS).toList()
        );
    }

    private static boolean containsAny(QuestionValidationReport report, String... tokens) {
        List<String> bag = new ArrayList<>();
        bag.addAll(report.hardFailReasons());
        bag.addAll(report.softWarnings());
        bag.addAll(report.repairHints());
        for (String line : bag) {
            String normalized = line.toLowerCase(Locale.ROOT);
            for (String token : tokens) {
                if (normalized.contains(token)) {
                    return true;
                }
            }
        }
        return false;
    }
}
