package com.aimong.backend.tools.questionbank;

import java.util.List;

public record CurriculumManifest(
        String sourceTitle,
        String sourceReference,
        List<CurriculumUnit> units
) {
}
