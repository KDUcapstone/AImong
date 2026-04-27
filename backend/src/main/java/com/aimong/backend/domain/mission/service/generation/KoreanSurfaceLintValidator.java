package com.aimong.backend.domain.mission.service.generation;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class KoreanSurfaceLintValidator {

    private static final List<String> OBVIOUS_PARTICLE_ERRORS = List.of(
            "자료을",
            "문장가",
            "제목가",
            "소리을",
            "카메라을",
            "사진을를",
            "이름을를",
            "주소를을",
            "번호를을",
            "친구가를",
            "설명을를"
    );

    private static final List<String> AWKWARD_REPETITIONS = List.of(
            "해 보아 보아요",
            "확인해 보아 보아요",
            "생각해 보아 보아요",
            "골라 보아 보세요"
    );

    public ValidationSubResult validate(StructuredQuestionSchema candidate) {
        List<String> hardFails = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Set<String> repairHints = new LinkedHashSet<>();

        List<String> texts = new ArrayList<>();
        texts.add(candidate.question() == null ? "" : candidate.question());
        texts.add(candidate.explanation() == null ? "" : candidate.explanation());
        if (candidate.options() != null) {
            texts.addAll(candidate.options());
        }

        for (String text : texts) {
            if (text == null || text.isBlank()) {
                continue;
            }
            if (containsAny(text, OBVIOUS_PARTICLE_ERRORS)) {
                hardFails.add("surface.obvious_particle_error");
                repairHints.add("Fix obvious particle errors such as '자료을' or '카메라을'.");
                break;
            }
            if (containsAny(text, AWKWARD_REPETITIONS)) {
                warnings.add("surface.awkward_repetition");
                repairHints.add("Rewrite repeated phrasing so the sentence sounds natural in Korean.");
            }
            if (text.contains("  ")) {
                warnings.add("surface.irregular_spacing");
            }
        }

        int score = hardFails.isEmpty() ? Math.max(0, 100 - warnings.size() * 10) : 0;
        return new ValidationSubResult(score, List.copyOf(hardFails), List.copyOf(warnings), List.copyOf(repairHints));
    }

    private boolean containsAny(String text, List<String> patterns) {
        return patterns.stream().anyMatch(text::contains);
    }
}
