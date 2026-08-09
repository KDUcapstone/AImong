package com.aimong.backend.tools.questionbank;

import java.util.List;

public record CurriculumUnit(
        String unitCode,
        short stage,
        String unitTitle,
        List<CurriculumLesson> lessons
) {
}
