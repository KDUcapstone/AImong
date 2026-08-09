package com.aimong.backend.tools.questionbank;

import java.util.ArrayList;
import java.util.List;

public final class Step3VocabularyCeilingAnalyzer {

    private static final List<String> FLAGGED_TERMS = List.of(
            "자동화 편향",
            "이해관계자",
            "Moral Machine",
            "윤리 프레임",
            "거버넌스",
            "법적 책임",
            "사회구조",
            "공리주의",
            "프레임워크"
    );

    public Step3VocabularyReport validate(List<AuditQuestion> questions) {
        List<String> hits = new ArrayList<>();
        for (AuditQuestion question : questions) {
            if (question.stage() != 3) {
                continue;
            }
            String text = String.join(" ",
                    question.question() == null ? "" : question.question(),
                    question.explanation() == null ? "" : question.explanation());
            for (String term : FLAGGED_TERMS) {
                if (text.contains(term)) {
                    hits.add(question.externalId() + ":" + term);
                }
            }
        }
        return new Step3VocabularyReport(hits.size(), List.copyOf(hits));
    }

    public record Step3VocabularyReport(
            int hitCount,
            List<String> hits
    ) {
    }
}
