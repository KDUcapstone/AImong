package com.aimong.backend.domain.mission.service.question;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class SimilarityDeduplicatorTest {

    private final SimilarityDeduplicator deduplicator = new SimilarityDeduplicator();

    @Test
    void rejectsExactAndNearDuplicates() {
        assertThat(deduplicator.validate(
                "AI \uB2F5\uC740 \uB2E4\uC2DC \uD655\uC778\uD574\uC57C \uD574\uC694",
                List.of("AI \uB2F5\uC740 \uB2E4\uC2DC \uD655\uC778\uD574\uC57C \uD574\uC694")
        )).isNotEmpty();

        assertThat(deduplicator.validate(
                "AI \uB2F5\uC740 \uAF2D \uB2E4\uC2DC \uD655\uC778\uD574\uC57C \uD574\uC694",
                List.of("AI \uB2F5\uC740 \uB2E4\uC2DC \uD655\uC778\uD574\uC57C \uD574\uC694")
        )).isNotEmpty();
    }

    @Test
    void allowsDifferentPrompt() {
        assertThat(deduplicator.validate(
                "\uC88B\uC740 \uD504\uB86C\uD504\uD2B8\uC5D0 \uB4E4\uC5B4\uAC08 \uC870\uAC74\uC744 \uACE0\uB974\uC138\uC694",
                List.of("AI \uB2F5\uC740 \uB2E4\uC2DC \uD655\uC778\uD574\uC57C \uD574\uC694")
        )).isEmpty();
    }

    @Test
    void rejectsKoreanNearDuplicatesWithOnlyParticlesAndEndingsChanged() {
        assertThat(deduplicator.validate(
                "\uBE44\uBC00\uBC88\uD638\uB97C \uCE5C\uAD6C\uC5D0\uAC8C \uC54C\uB824 \uC8FC\uBA74 \uC65C \uC704\uD5D8\uD55C\uAC00\uC694?",
                List.of("\uBE44\uBC00\uBC88\uD638\uB294 \uCE5C\uAD6C\uD55C\uD14C \uC54C\uB824\uC8FC\uBA74 \uC65C \uC704\uD5D8\uD560\uAE4C\uC694?")
        )).isNotEmpty();
    }

    @Test
    void rejectsPromptsWithSameCoreQuestionAndSmallPrefixChange() {
        assertThat(deduplicator.validate(
                "\uB2E4\uC74C \uC911 \uAC1C\uC778\uC815\uBCF4\uB97C \uC628\uB77C\uC778\uC5D0 \uC62C\uB9AC\uBA74 \uC548 \uB418\uB294 \uC774\uC720\uB294 \uBB34\uC5C7\uC778\uAC00\uC694?",
                List.of("\uAC1C\uC778 \uC815\uBCF4\uB97C \uC778\uD130\uB137\uC5D0 \uC62C\uB9AC\uBA74 \uC548 \uB418\uB294 \uC774\uC720\uB97C \uACE0\uB974\uC138\uC694.")
        )).isNotEmpty();
    }
}
