package com.aimong.backend.domain.mission.service.generation;

import com.aimong.backend.domain.mission.config.ModelRoutingProperties;
import com.aimong.backend.domain.mission.entity.DifficultyBand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ModelRoutingPolicy {

    private final ModelRoutingProperties properties;

    public RoutingDecision decide(GenerationContext context) {
        boolean escalate = context.stage() == 3 && context.difficultyBand() == DifficultyBand.HIGH;
        escalate = escalate || context.numericDifficulty() >= 4;
        escalate = escalate || context.validationFailureCount() >= 2;
        escalate = escalate || context.wordingQualityWeak();
        escalate = escalate || context.highDuplicateRisk();
        escalate = escalate || context.optionQualityWeak();
        escalate = escalate || context.explanationQualityWeak();

        return new RoutingDecision(
                escalate ? properties.escalatedModel() : properties.defaultModel(),
                escalate
        );
    }

    public record GenerationContext(
            int stage,
            DifficultyBand difficultyBand,
            int numericDifficulty,
            int validationFailureCount,
            boolean wordingQualityWeak,
            boolean highDuplicateRisk,
            boolean optionQualityWeak,
            boolean explanationQualityWeak
    ) {
    }

    public record RoutingDecision(
            String selectedModel,
            boolean escalated
    ) {
    }
}
