package com.aimong.backend.tools.questionbank;

import java.util.ArrayList;
import java.util.List;

public final class BatchSurfaceLintAnalyzer {

    private static final List<String> PATTERNS = List.of(
            "자료을",
            "문장가",
            "제목가",
            "소리을",
            "카메라을"
    );

    public BatchSurfaceLintReport validate(List<AuditQuestion> questions) {
        List<String> hits = new ArrayList<>();
        for (AuditQuestion question : questions) {
            String text = String.join(" ",
                    question.question() == null ? "" : question.question(),
                    question.explanation() == null ? "" : question.explanation(),
                    question.options() == null ? "" : String.join(" ", question.options()));
            for (String pattern : PATTERNS) {
                if (text.contains(pattern)) {
                    hits.add(question.externalId() + ":" + pattern);
                }
            }
        }
        return new BatchSurfaceLintReport(hits.size(), List.copyOf(hits));
    }

    public record BatchSurfaceLintReport(
            int hitCount,
            List<String> hits
    ) {
    }
}
