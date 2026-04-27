package com.aimong.backend.domain.mission.service.generation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class Step3VocabularyCeilingValidator {

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

    public ValidationSubResult validate(StructuredQuestionSchema candidate) {
        List<String> hardFails = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<String> repairHints = new LinkedHashSet<>();

        if (candidate == null || candidate.difficulty() < 3) {
            return new ValidationSubResult(100, List.of(), List.of(), List.of());
        }

        String text = ValidationTextUtils.joinCandidateText(candidate);
        List<String> hits = FLAGGED_TERMS.stream()
                .filter(text::contains)
                .toList();

        if (hits.isEmpty()) {
            return new ValidationSubResult(100, List.of(), List.of(), List.of());
        }

        warnings.add("step3.vocabulary_ceiling");
        if (hits.size() >= 2) {
            hardFails.add("step3.vocabulary_ceiling_exceeded");
        }
        repairHints.add("Replace advanced Step 3 terms with plain daily-life wording for grade 5-6 learners.");
        repairHints.add("Preferred rewrite style: compare evidence, check reasons, notice fairness, explain impact in simple words.");

        int score = hardFails.isEmpty() ? Math.max(0, 100 - hits.size() * 15) : 0;
        return new ValidationSubResult(score, List.copyOf(hardFails), List.copyOf(warnings), List.copyOf(repairHints));
    }
}
