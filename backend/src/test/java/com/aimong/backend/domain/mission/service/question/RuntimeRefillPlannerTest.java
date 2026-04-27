package com.aimong.backend.domain.mission.service.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.QuestionType;
import org.junit.jupiter.api.Test;

class RuntimeRefillPlannerTest {

    private final RuntimeRefillPlanner planner = new RuntimeRefillPlanner();

    @Test
    void plansServingShortageByBandWithTypePreferences() {
        RuntimeRefillPlanner.RuntimeRefillPlan plan = planner.planServingRefill(
                "S0203",
                (short) 2,
                new RecompositionSelector.ShortageDetails(
                        2,
                        1,
                        1,
                        3,
                        "INSUFFICIENT_DIFFICULTY_BAND_POOL",
                        new RecompositionSelector.CandidatePoolCounts(6, 3, 2, 1)
                ),
                java.util.List.of()
        );

        assertThat(plan.requestCount()).isEqualTo(4);
        assertThat(plan.requests())
                .extracting(RuntimeRefillPlanner.RuntimeGenerationRequest::difficultyBand)
                .containsExactly(DifficultyBand.LOW, DifficultyBand.LOW, DifficultyBand.MEDIUM, DifficultyBand.HIGH);
        assertThat(plan.requests())
                .extracting(RuntimeRefillPlanner.RuntimeGenerationRequest::desiredType)
                .containsExactly(QuestionType.OX, QuestionType.MULTIPLE, QuestionType.MULTIPLE, QuestionType.MULTIPLE);
    }

    @Test
    void plansAsyncPoolRefillAgainstMissionBandTargets() {
        RuntimeRefillPlanner.RuntimeRefillPlan plan = planner.planPoolRefill(
                "S0203",
                (short) 2,
                28,
                18,
                9,
                4,
                java.util.List.of()
        );

        assertThat(plan.requestCount()).isEqualTo(4);
        assertThat(plan.requests())
                .extracting(RuntimeRefillPlanner.RuntimeGenerationRequest::difficultyBand)
                .containsExactly(DifficultyBand.LOW, DifficultyBand.LOW, DifficultyBand.MEDIUM, DifficultyBand.MEDIUM);
    }
}
