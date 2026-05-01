package com.aimong.backend.domain.mission.service.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class SimilarityDeduplicatorTest {

    private final SimilarityDeduplicator deduplicator = new SimilarityDeduplicator();

    @Test
    void rejectsExactAndNearDuplicates() {
        assertThat(deduplicator.validate(
                "AI 답은 다시 확인해야 해요",
                List.of("AI 답은 다시 확인해야 해요")
        )).isNotEmpty();

        assertThat(deduplicator.validate(
                "AI 답은 꼭 다시 확인해야 해요",
                List.of("AI 답은 다시 확인해야 해요")
        )).isNotEmpty();
    }

    @Test
    void allowsDifferentPrompt() {
        assertThat(deduplicator.validate(
                "좋은 프롬프트에 들어갈 조건을 고르세요",
                List.of("AI 답은 다시 확인해야 해요")
        )).isEmpty();
    }
}
