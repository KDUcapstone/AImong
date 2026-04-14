package com.aimong.backend.tools.questionbank;

import java.util.List;

public record CurriculumLesson(
        String lessonCode,
        String missionTitle,
        String studentObjective,
        String keyConcept,
        List<String> keywords,
        String promptTip,
        String safetyRule,
        String verificationHabit
) {
}
