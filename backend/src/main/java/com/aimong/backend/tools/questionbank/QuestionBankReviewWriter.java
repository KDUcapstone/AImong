package com.aimong.backend.tools.questionbank;

import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class QuestionBankReviewWriter {

    public String write(QuestionBankDraft draft) {
        StringBuilder builder = new StringBuilder();
        builder.append("# KERIS Question Bank Review\n\n");
        builder.append("- source: ").append(draft.sourceTitle()).append('\n');
        builder.append("- total questions: ").append(draft.totalQuestionCount()).append('\n');
        builder.append("- missions: ").append(
                draft.questions().stream().map(QuestionDraft::missionCode).distinct().count()
        ).append("\n\n");

        builder.append("## Type Counts\n\n");
        appendCounts(builder, draft.questions().stream().collect(Collectors.groupingBy(QuestionDraft::type, Collectors.counting())));
        builder.append("\n## Tag Counts\n\n");
        appendCounts(builder, draft.questions().stream()
                .flatMap(question -> question.contentTags().stream())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting())));

        builder.append("\n## Mission Samples\n\n");
        draft.questions().stream()
                .collect(Collectors.groupingBy(QuestionDraft::missionCode))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    QuestionDraft first = entry.getValue().getFirst();
                    builder.append("### ").append(first.missionCode()).append(" - ").append(first.missionTitle()).append("\n\n");
                    entry.getValue().stream().limit(3).forEach(question -> {
                        builder.append("- [").append(question.type()).append("] ").append(question.question()).append('\n');
                    });
                    builder.append('\n');
                });

        return builder.toString();
    }

    private void appendCounts(StringBuilder builder, Map<String, Long> counts) {
        counts.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry<String, Long>::getValue).reversed().thenComparing(Map.Entry::getKey))
                .forEach(entry -> builder.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append('\n'));
    }
}
