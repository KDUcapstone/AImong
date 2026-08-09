package com.aimong.backend.domain.mission.service.question;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SimilarityDeduplicator {

    private static final double NEAR_DUPLICATE_THRESHOLD = 0.82d;
    private static final double CORE_TOKEN_THRESHOLD = 0.68d;
    private static final double CHARACTER_NGRAM_THRESHOLD = 0.68d;
    private static final double TOKEN_CONTAINMENT_THRESHOLD = 0.82d;
    private static final int MIN_COMPACT_CONTAINMENT_LENGTH = 12;
    private static final List<String> BOILERPLATE_TOKENS = List.of(
            "\uB2E4\uC74C", "\uC911", "\uBCF4\uAE30", "\uBB38\uC81C", "\uC815\uB2F5",
            "\uAC00\uC7A5", "\uC54C\uB9DE\uC740", "\uC62C\uBC14\uB978", "\uC62E\uC740", "\uD2C0\uB9B0",
            "\uAC83\uC740", "\uAC83\uC744", "\uAC83\uC774", "\uAC83\uC778\uAC00\uC694",
            "\uBB34\uC5C7\uC778\uAC00\uC694", "\uBB34\uC5C7\uC77C\uAE4C\uC694",
            "\uACE0\uB974\uC138\uC694", "\uC120\uD0DD\uD558\uC138\uC694",
            "\uC544\uB798", "\uC704", "\uC124\uBA85", "\uB0B4\uC6A9", "\uC608\uC2DC",
            "\uC0C1\uD669", "\uCE5C\uAD6C", "\uD559\uC0DD"
    );
    private static final List<String> SUFFIXES = List.of(
            "\uC73C\uB85C\uBD80\uD130", "\uB85C\uBD80\uD130", "\uC5D0\uAC8C\uC11C", "\uD55C\uD14C\uC11C",
            "\uC5D0\uAC8C", "\uD55C\uD14C", "\uC5D0\uC11C", "\uC73C\uB85C", "\uCC98\uB7FC", "\uBCF4\uB2E4",
            "\uAE4C\uC9C0", "\uBD80\uD130", "\uC774\uB77C\uBA74", "\uB77C\uBA74", "\uC774\uB77C\uC11C",
            "\uB77C\uC11C", "\uC774\uB77C\uACE0", "\uB77C\uACE0", "\uC774\uB77C\uB294", "\uB77C\uB294",
            "\uC774\uBA70", "\uC774\uACE0", "\uC774\uB098", "\uAC70\uB098", "\uC785\uB2C8\uB2E4",
            "\uC785\uB2C8\uAE4C", "\uC778\uAC00\uC694", "\uD55C\uAC00\uC694", "\uAC00\uC694",
            "\uC77C\uAE4C\uC694", "\uD560\uAE4C\uC694", "\uAE4C\uC694",
            "\uD588\uB098\uC694", "\uD558\uB098\uC694", "\uD558\uC138\uC694", "\uD574\uC694",
            "\uC5B4\uC694", "\uC544\uC694", "\uC608\uC694", "\uC774\uC5D0\uC694", "\uB2C8\uB2E4",
            "\uB2C8\uAE4C", "\uC778\uAC00", "\uC774\uB2E4", "\uD55C\uB2E4", "\uD588\uB2E4",
            "\uD55C\uB2E4\uBA74", "\uD558\uBA74", "\uD558\uBA70", "\uBA74", "\uC740", "\uB294", "\uC774",
            "\uAC00", "\uC744", "\uB97C", "\uC5D0", "\uC758", "\uC640", "\uACFC", "\uB3C4",
            "\uB9CC", "\uB85C", "\uB098"
    );

    public List<String> validate(String candidate, List<String> existingTexts) {
        return existingTexts.stream()
                .filter(existing -> isDuplicateOrNearDuplicate(candidate, existing))
                .map(existing -> "duplicate-or-near-duplicate")
                .findFirst()
                .stream()
                .toList();
    }

    public boolean isDuplicateOrNearDuplicate(String candidate, String existing) {
        if (candidate == null || existing == null) {
            return false;
        }
        String normalizedCandidate = normalize(candidate);
        String normalizedExisting = normalize(existing);
        if (normalizedCandidate.isBlank() || normalizedExisting.isBlank()) {
            return false;
        }
        if (normalizedCandidate.equals(normalizedExisting)) {
            return true;
        }
        return similarityScore(normalizedCandidate, normalizedExisting) >= NEAR_DUPLICATE_THRESHOLD
                || coreTokenSimilarity(normalizedCandidate, normalizedExisting) >= CORE_TOKEN_THRESHOLD
                || characterNgramSimilarity(normalizedCandidate, normalizedExisting) >= CHARACTER_NGRAM_THRESHOLD
                || compactContainment(normalizedCandidate, normalizedExisting)
                || tokenContainment(normalizedCandidate, normalizedExisting) >= TOKEN_CONTAINMENT_THRESHOLD;
    }

    public String nearDuplicateKey(String value) {
        String core = coreNormalize(value);
        if (core.isBlank()) {
            return normalize(value);
        }
        return core.replace(" ", "");
    }

    public double similarityScore(String left, String right) {
        String normalizedLeft = normalize(left);
        String normalizedRight = normalize(right);
        if (normalizedLeft.isBlank() || normalizedRight.isBlank()) {
            return 0d;
        }
        return Math.max(
                tokenJaccard(tokenSet(normalizedLeft), tokenSet(normalizedRight)),
                Math.max(
                        coreTokenSimilarity(normalizedLeft, normalizedRight),
                        characterNgramSimilarity(normalizedLeft, normalizedRight)
                )
        );
    }

    private double coreTokenSimilarity(String left, String right) {
        return tokenJaccard(tokenSet(coreNormalize(left)), tokenSet(coreNormalize(right)));
    }

    private double characterNgramSimilarity(String left, String right) {
        String leftCompact = coreNormalize(left).replace(" ", "");
        String rightCompact = coreNormalize(right).replace(" ", "");
        if (leftCompact.length() < 6 || rightCompact.length() < 6) {
            return 0d;
        }
        return tokenJaccard(ngrams(leftCompact, 2), ngrams(rightCompact, 2));
    }

    private boolean compactContainment(String left, String right) {
        String leftCompact = coreNormalize(left).replace(" ", "");
        String rightCompact = coreNormalize(right).replace(" ", "");
        if (leftCompact.length() < MIN_COMPACT_CONTAINMENT_LENGTH
                || rightCompact.length() < MIN_COMPACT_CONTAINMENT_LENGTH) {
            return false;
        }
        int minLength = Math.min(leftCompact.length(), rightCompact.length());
        int maxLength = Math.max(leftCompact.length(), rightCompact.length());
        return maxLength > 0
                && minLength / (double) maxLength >= 0.72d
                && (leftCompact.contains(rightCompact) || rightCompact.contains(leftCompact));
    }

    private double tokenContainment(String left, String right) {
        Set<String> leftTokens = tokenSet(coreNormalize(left));
        Set<String> rightTokens = tokenSet(coreNormalize(right));
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return 0d;
        }
        Set<String> intersection = new LinkedHashSet<>(leftTokens);
        intersection.retainAll(rightTokens);
        return intersection.size() / (double) Math.min(leftTokens.size(), rightTokens.size());
    }

    private String coreNormalize(String value) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            return "";
        }
        normalized = normalized
                .replace("\uAC1C\uC778 \uC815\uBCF4", "\uAC1C\uC778\uC815\uBCF4")
                .replace("\uC628\uB77C\uC778", "\uC778\uD130\uB137")
                .replace("\uC54C\uB824 \uC8FC", "\uC54C\uB824\uC8FC");
        return Arrays.stream(normalized.split("\\s+"))
                .map(this::stripKoreanSuffixes)
                .filter(token -> token.length() > 1)
                .filter(token -> !BOILERPLATE_TOKENS.contains(token))
                .distinct()
                .reduce((left, right) -> left + " " + right)
                .orElse("");
    }

    private String stripKoreanSuffixes(String token) {
        String current = token.toLowerCase(Locale.ROOT);
        boolean changed = true;
        while (changed && current.length() > 2) {
            changed = false;
            for (String suffix : SUFFIXES) {
                if (current.length() - suffix.length() >= 2 && current.endsWith(suffix)) {
                    current = current.substring(0, current.length() - suffix.length());
                    changed = true;
                    break;
                }
            }
        }
        return current;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Set<String> tokenSet(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return new LinkedHashSet<>(Arrays.asList(value.split("\\s+")));
    }

    private Set<String> ngrams(String value, int size) {
        if (value.length() < size) {
            return Set.of(value);
        }
        Set<String> result = new LinkedHashSet<>();
        for (int index = 0; index <= value.length() - size; index++) {
            result.add(value.substring(index, index + size));
        }
        return result;
    }

    private double tokenJaccard(Set<String> left, Set<String> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return 0d;
        }
        Set<String> intersection = new LinkedHashSet<>(left);
        intersection.retainAll(right);
        Set<String> union = new LinkedHashSet<>(left);
        union.addAll(right);
        return union.isEmpty() ? 0d : intersection.size() / (double) union.size();
    }
}
