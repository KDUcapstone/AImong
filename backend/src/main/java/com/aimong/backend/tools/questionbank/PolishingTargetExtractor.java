package com.aimong.backend.tools.questionbank;

import com.aimong.backend.domain.mission.entity.QuestionType;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class PolishingTargetExtractor {

    public TargetReport extract(AuditQuestionBank bank) {
        Map<String, List<AuditQuestion>> bySuffix = new LinkedHashMap<>();
        for (AuditQuestion question : bank.questions()) {
            String suffix = suffix(normalize(question.explanation()));
            bySuffix.computeIfAbsent(suffix, ignored -> new ArrayList<>()).add(question);
        }

        List<ExplanationCluster> explanationClusters = bySuffix.entrySet().stream()
                .filter(entry -> entry.getValue().size() >= 8)
                .sorted((left, right) -> Integer.compare(right.getValue().size(), left.getValue().size()))
                .map(entry -> new ExplanationCluster(
                        entry.getKey(),
                        entry.getValue().size(),
                        entry.getValue().stream().map(AuditQuestion::externalId).toList(),
                        entry.getValue().stream()
                                .map(question -> question.missionCode() + "/P" + question.packNo() + "/" + slotNo(question.externalId()))
                                .toList()
                ))
                .toList();

        List<OptionStyleTarget> optionStyleTargets = bank.questions().stream()
                .filter(question -> question.type() == QuestionType.MULTIPLE || question.type() == QuestionType.SITUATION)
                .filter(question -> question.options() != null && !question.options().isEmpty())
                .filter(question -> question.answer() instanceof Integer)
                .map(this::toOptionStyleTarget)
                .filter(OptionStyleTarget::flagged)
                .sorted((left, right) -> Double.compare(right.variance(), left.variance()))
                .toList();

        return new TargetReport(explanationClusters, optionStyleTargets);
    }

    private OptionStyleTarget toOptionStyleTarget(AuditQuestion question) {
        List<Integer> lengths = question.options().stream().map(String::length).toList();
        double variance = variance(lengths);
        long distinctOpenings = question.options().stream()
                .map(this::firstToken)
                .distinct()
                .count();
        List<String> reasons = new ArrayList<>();
        if (variance >= 40d) {
            reasons.add("length_variance");
        }
        if (distinctOpenings <= 2) {
            reasons.add("opening_style_imbalance");
        }
        return new OptionStyleTarget(
                question.externalId(),
                question.missionCode(),
                question.packNo() == null ? 0 : question.packNo(),
                question.type().name(),
                (Integer) question.answer(),
                variance,
                distinctOpenings <= 2,
                lengths,
                question.options(),
                List.copyOf(reasons),
                !reasons.isEmpty()
        );
    }

    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{Nd}\\s가-힣]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    static String suffix(String normalized) {
        if (normalized == null || normalized.isBlank()) {
            return "";
        }
        String[] tokens = normalized.split("\\s+");
        if (tokens.length <= 4) {
            return normalized;
        }
        return String.join(" ", java.util.Arrays.copyOfRange(tokens, tokens.length - 4, tokens.length));
    }

    private String firstToken(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().split("\\s+")[0];
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

    private Integer slotNo(String externalId) {
        if (externalId == null) {
            return null;
        }
        int lastDash = externalId.lastIndexOf('-');
        if (lastDash < 0 || lastDash + 1 >= externalId.length()) {
            return null;
        }
        return Integer.parseInt(externalId.substring(lastDash + 1));
    }

    public record ExplanationCluster(
            String suffix,
            int count,
            List<String> externalIds,
            List<String> missionPackSlots
    ) {
    }

    public record OptionStyleTarget(
            String externalId,
            String missionCode,
            int packNo,
            String type,
            int answerIndex,
            double variance,
            boolean openingStyleImbalance,
            List<Integer> optionLengths,
            List<String> options,
            List<String> reasons,
            boolean flagged
    ) {
    }

    public record TargetReport(
            List<ExplanationCluster> explanationClusters,
            List<OptionStyleTarget> optionStyleTargets
    ) {
        public Set<String> explanationTargetIds() {
            return explanationClusters.stream()
                    .flatMap(cluster -> cluster.externalIds().stream())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        public Set<String> optionTargetIds() {
            return optionStyleTargets.stream()
                    .map(OptionStyleTarget::externalId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }
}
