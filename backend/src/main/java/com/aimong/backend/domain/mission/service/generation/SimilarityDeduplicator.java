package com.aimong.backend.domain.mission.service.generation;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SimilarityDeduplicator {

    private static final double NEAR_DUPLICATE_THRESHOLD = 0.82d;
    private static final double GOLD_EXAMPLE_WARNING_THRESHOLD = 0.72d;

    public List<String> validate(String candidate, List<String> existingTexts) {
        String normalizedCandidate = normalize(candidate);
        var candidateTokens = ValidationTextUtils.tokenSet(normalizedCandidate);

        return existingTexts.stream()
                .map(this::normalize)
                .filter(existing -> !existing.isBlank())
                .filter(existing -> existing.equals(normalizedCandidate)
                        || jaccard(candidateTokens, ValidationTextUtils.tokenSet(existing)) >= NEAR_DUPLICATE_THRESHOLD)
                .map(existing -> "duplicate-or-near-duplicate")
                .findFirst()
                .stream()
                .toList();
    }

    public SimilarityCheckResult validate(
            StructuredQuestionSchema candidate,
            List<String> existingTexts,
            List<String> goldExampleTexts
    ) {
        String question = candidate.question() == null ? "" : candidate.question();
        String normalizedCandidate = normalize(question);
        var candidateTokens = ValidationTextUtils.tokenSet(normalizedCandidate);

        List<String> hardFails = existingTexts.stream()
                .map(this::normalize)
                .filter(existing -> !existing.isBlank())
                .filter(existing -> existing.equals(normalizedCandidate)
                        || jaccard(candidateTokens, ValidationTextUtils.tokenSet(existing)) >= NEAR_DUPLICATE_THRESHOLD)
                .map(existing -> "originality.duplicate_or_near_duplicate")
                .findFirst()
                .stream()
                .toList();

        List<String> warnings = goldExampleTexts.stream()
                .map(this::normalize)
                .filter(existing -> !existing.isBlank())
                .filter(existing -> jaccard(candidateTokens, ValidationTextUtils.tokenSet(existing)) >= GOLD_EXAMPLE_WARNING_THRESHOLD)
                .map(existing -> "originality.too_close_to_gold_example")
                .findFirst()
                .stream()
                .toList();

        List<String> repairHints = hardFails.isEmpty() && warnings.isEmpty()
                ? List.of()
                : List.of("Change the scenario, stem, distractors, and explanation pattern to make the item more original.");

        int originalityScore = hardFails.isEmpty()
                ? Math.max(0, 100 - warnings.size() * 20)
                : 0;
        boolean escalateSuggested = !hardFails.isEmpty() && candidate.difficulty() >= 4;

        return new SimilarityCheckResult(
                originalityScore,
                hardFails,
                warnings,
                repairHints,
                escalateSuggested
        );
    }

    String normalize(String value) {
        return ValidationTextUtils.normalize(value);
    }

    private double jaccard(java.util.Set<String> left, java.util.Set<String> right) {
        return ValidationTextUtils.tokenJaccard(String.join(" ", left), String.join(" ", right));
    }

    public record SimilarityCheckResult(
            int originalityScore,
            List<String> hardFailReasons,
            List<String> softWarnings,
            List<String> repairHints,
            boolean escalateSuggested
    ) {
        static SimilarityCheckResult clean() {
            return new SimilarityCheckResult(100, List.of(), List.of(), List.of(), false);
        }
    }
}
