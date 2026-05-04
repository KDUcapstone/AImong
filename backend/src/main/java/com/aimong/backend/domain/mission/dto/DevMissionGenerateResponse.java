package com.aimong.backend.domain.mission.dto;

import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.QuestionType;
import com.aimong.backend.domain.mission.service.generation.QuestionValidationScores;
import java.util.List;
import java.util.UUID;

public record DevMissionGenerateResponse(
        UUID missionId,
        String missionCode,
        String model,
        boolean escalated,
        DifficultyBand difficultyBand,
        QuestionType type,
        int requestedCount,
        int acceptedCount,
        int rejectedCount,
        int savedCount,
        List<CandidateResponse> accepted,
        List<RejectedCandidateResponse> rejected
) {

    public record CandidateResponse(
            UUID savedQuestionId,
            String question,
            List<String> options,
            Object answer,
            String explanation,
            List<String> contentTags,
            String curriculumRef,
            int difficulty,
            QuestionValidationScores scores
    ) {
    }

    public record RejectedCandidateResponse(
            String question,
            List<String> hardFailReasons,
            List<String> softWarnings,
            List<String> repairHints,
            QuestionValidationScores scores
    ) {
    }
}
