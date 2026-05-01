package com.aimong.backend.domain.mission.service.generation;

public record QuestionValidationScores(
        int schemaQuality,
        int safety,
        int curriculumFit,
        int stageFit,
        int elementaryReadability,
        int answerClarity,
        int distractorQuality,
        int explanationQuality,
        int originality,
        int criteriaFit,
        int kerisThreeStepFit,
        int elementaryDifficultyDirection,
        int structureQuotaTypeRules,
        int naturalness,
        int duplicationOriginality,
        int productionReadiness,
        int overall
) {
}
