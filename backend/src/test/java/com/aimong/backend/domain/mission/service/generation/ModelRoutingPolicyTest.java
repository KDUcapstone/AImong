package com.aimong.backend.domain.mission.service.generation;

import static org.assertj.core.api.Assertions.assertThat;

import com.aimong.backend.domain.mission.config.ModelRoutingProperties;
import com.aimong.backend.domain.mission.entity.DifficultyBand;
import org.junit.jupiter.api.Test;

class ModelRoutingPolicyTest {

    private final ModelRoutingPolicy policy = new ModelRoutingPolicy(
            new ModelRoutingProperties("gpt-5-mini", "gpt-5.4-mini")
    );

    @Test
    void keepsDefaultModelForLowRiskRequest() {
        ModelRoutingPolicy.RoutingDecision decision = policy.decide(
                new ModelRoutingPolicy.GenerationContext(2, DifficultyBand.LOW, 2, 0, false, false, false, false)
        );

        assertThat(decision.selectedModel()).isEqualTo("gpt-5-mini");
        assertThat(decision.escalated()).isFalse();
    }

    @Test
    void escalatesForStepThreeHighBand() {
        ModelRoutingPolicy.RoutingDecision decision = policy.decide(
                new ModelRoutingPolicy.GenerationContext(3, DifficultyBand.HIGH, 4, 0, false, false, false, false)
        );

        assertThat(decision.selectedModel()).isEqualTo("gpt-5.4-mini");
        assertThat(decision.escalated()).isTrue();
    }
}
