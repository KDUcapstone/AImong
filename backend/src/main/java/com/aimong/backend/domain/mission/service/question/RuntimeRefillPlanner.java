package com.aimong.backend.domain.mission.service.question;

import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.QuestionType;
import com.aimong.backend.domain.mission.service.generation.QuestionGenerationService;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RuntimeRefillPlanner {

    private static final Map<DifficultyBand, List<QuestionType>> TYPE_PREFERENCES = Map.of(
            DifficultyBand.LOW, List.of(QuestionType.OX, QuestionType.MULTIPLE, QuestionType.FILL, QuestionType.SITUATION),
            DifficultyBand.MEDIUM, List.of(QuestionType.MULTIPLE, QuestionType.FILL, QuestionType.SITUATION, QuestionType.OX),
            DifficultyBand.HIGH, List.of(QuestionType.MULTIPLE, QuestionType.FILL, QuestionType.SITUATION, QuestionType.OX)
    );

    public RuntimeRefillPlan planServingRefill(
            String missionCode,
            short stage,
            RecompositionSelector.ShortageDetails shortageDetails,
            List<String> existingMissionPrompts
    ) {
        Map<DifficultyBand, Integer> shortages = new EnumMap<>(DifficultyBand.class);
        shortages.put(DifficultyBand.LOW, shortageDetails.lowMissing());
        shortages.put(DifficultyBand.MEDIUM, shortageDetails.mediumMissing());
        shortages.put(DifficultyBand.HIGH, shortageDetails.highMissing());
        return new RuntimeRefillPlan(buildRequests(missionCode, stage, shortages, shortageDetails.totalMissing(), existingMissionPrompts));
    }

    public RuntimeRefillPlan planPoolRefill(
            String missionCode,
            short stage,
            long lowAvailable,
            long mediumAvailable,
            long highAvailable,
            int requestBudget,
            List<String> existingMissionPrompts
    ) {
        Map<DifficultyBand, Integer> shortages = new EnumMap<>(DifficultyBand.class);
        shortages.put(DifficultyBand.LOW, Math.max(0, 30 - (int) lowAvailable));
        shortages.put(DifficultyBand.MEDIUM, Math.max(0, 20 - (int) mediumAvailable));
        shortages.put(DifficultyBand.HIGH, Math.max(0, 10 - (int) highAvailable));
        return new RuntimeRefillPlan(buildRequests(missionCode, stage, shortages, requestBudget, existingMissionPrompts));
    }

    private List<RuntimeGenerationRequest> buildRequests(
            String missionCode,
            short stage,
            Map<DifficultyBand, Integer> shortages,
            int requestBudget,
            List<String> existingMissionPrompts
    ) {
        List<RuntimeGenerationRequest> requests = new ArrayList<>();
        int remainingBudget = requestBudget;
        for (DifficultyBand band : List.of(DifficultyBand.LOW, DifficultyBand.MEDIUM, DifficultyBand.HIGH)) {
            int missing = Math.min(shortages.getOrDefault(band, 0), remainingBudget);
            if (missing <= 0) {
                continue;
            }
            List<QuestionType> preferences = TYPE_PREFERENCES.get(band);
            for (int index = 0; index < missing; index++) {
                QuestionType type = preferences.get(index % preferences.size());
                requests.add(new RuntimeGenerationRequest(
                        missionCode,
                        0,
                        band,
                        type,
                        1,
                        inferNumericDifficulty(stage, band),
                        List.copyOf(existingMissionPrompts)
                ));
            }
            remainingBudget -= missing;
            if (remainingBudget <= 0) {
                break;
            }
        }
        return List.copyOf(requests);
    }

    private int inferNumericDifficulty(short stage, DifficultyBand difficultyBand) {
        return switch (stage) {
            case 1 -> difficultyBand == DifficultyBand.LOW ? 1 : 2;
            case 2 -> difficultyBand == DifficultyBand.LOW ? 2 : 3;
            case 3 -> difficultyBand == DifficultyBand.LOW ? 3 : 4;
            default -> 1;
        };
    }

    public record RuntimeRefillPlan(List<RuntimeGenerationRequest> requests) {
        public int requestCount() {
            return requests.size();
        }
    }

    public record RuntimeGenerationRequest(
            String missionCode,
            int packNo,
            DifficultyBand difficultyBand,
            QuestionType desiredType,
            int candidateCount,
            int numericDifficulty,
            List<String> existingMissionPrompts
    ) {
        public QuestionGenerationService.QuestionGenerationRequest toGenerationRequest(
                int validationFailureCount,
                com.aimong.backend.domain.mission.service.generation.QuestionGenerationRetryFeedback feedback
        ) {
            return new QuestionGenerationService.QuestionGenerationRequest(
                    missionCode,
                    packNo,
                    difficultyBand,
                    desiredType,
                    candidateCount,
                    numericDifficulty,
                    validationFailureCount,
                    feedback.wordingQualityWeak(),
                    feedback.highDuplicateRisk(),
                    feedback.optionQualityWeak(),
                    feedback.explanationQualityWeak(),
                    existingMissionPrompts,
                    List.of(),
                    feedback.repairHints()
            );
        }
    }
}
