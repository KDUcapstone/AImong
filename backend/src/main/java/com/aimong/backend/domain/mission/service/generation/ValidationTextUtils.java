package com.aimong.backend.domain.mission.service.generation;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class ValidationTextUtils {

    private ValidationTextUtils() {
    }

    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\s가-힣]", "")
                .trim();
    }

    static Set<String> tokenSet(String value) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            return Set.of();
        }
        return new LinkedHashSet<>(Arrays.asList(normalized.split("\\s+")));
    }

    static double tokenJaccard(String left, String right) {
        Set<String> leftTokens = tokenSet(left);
        Set<String> rightTokens = tokenSet(right);
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return 0d;
        }
        Set<String> intersection = new LinkedHashSet<>(leftTokens);
        intersection.retainAll(rightTokens);
        Set<String> union = new LinkedHashSet<>(leftTokens);
        union.addAll(rightTokens);
        return union.isEmpty() ? 0d : (double) intersection.size() / union.size();
    }

    static String joinCandidateText(StructuredQuestionSchema candidate) {
        return String.join("\n",
                candidate.question() == null ? "" : candidate.question(),
                candidate.explanation() == null ? "" : candidate.explanation(),
                candidate.options() == null ? "" : String.join("\n", candidate.options())
        );
    }

    static long sentenceCount(String text) {
        long sentenceCount = text.chars()
                .filter(ch -> ch == '.' || ch == '!' || ch == '?')
                .count();
        return sentenceCount == 0 ? 1 : sentenceCount;
    }

    static int longestSentenceLength(String text) {
        return Arrays.stream(text.split("[.!?\\n]"))
                .map(String::trim)
                .mapToInt(String::length)
                .max()
                .orElse(0);
    }

    static List<String> normalizeOptions(List<String> options) {
        if (options == null) {
            return null;
        }
        return options.stream()
                .map(option -> option == null ? "" : option.trim().replaceAll("\\s+", " "))
                .toList();
    }
}
