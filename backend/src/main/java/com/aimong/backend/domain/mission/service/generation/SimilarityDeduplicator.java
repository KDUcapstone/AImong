package com.aimong.backend.domain.mission.service.generation;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SimilarityDeduplicator {

    private static final double NEAR_DUPLICATE_THRESHOLD = 0.82d;

    public List<String> validate(String candidate, List<String> existingTexts) {
        String normalizedCandidate = normalize(candidate);
        Set<String> candidateTokens = tokenSet(normalizedCandidate);

        return existingTexts.stream()
                .map(this::normalize)
                .filter(existing -> !existing.isBlank())
                .filter(existing -> existing.equals(normalizedCandidate)
                        || jaccard(candidateTokens, tokenSet(existing)) >= NEAR_DUPLICATE_THRESHOLD)
                .map(existing -> "duplicate-or-near-duplicate")
                .findFirst()
                .stream()
                .toList();
    }

    String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\s가-힣]", "")
                .trim();
        return normalized;
    }

    private Set<String> tokenSet(String normalized) {
        if (normalized.isBlank()) {
            return Set.of();
        }
        return new LinkedHashSet<>(Arrays.asList(normalized.split("\\s+")));
    }

    private double jaccard(Set<String> left, Set<String> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return 0d;
        }
        Set<String> intersection = new LinkedHashSet<>(left);
        intersection.retainAll(right);

        Set<String> union = new LinkedHashSet<>(left);
        union.addAll(right);

        return union.isEmpty() ? 0d : (double) intersection.size() / union.size();
    }
}
