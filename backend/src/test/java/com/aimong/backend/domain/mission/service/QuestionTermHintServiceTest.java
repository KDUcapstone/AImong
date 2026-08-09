package com.aimong.backend.domain.mission.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class QuestionTermHintServiceTest {

    private final QuestionTermHintService service = new QuestionTermHintService();

    @Test
    void findHintsReturnsMatchedTermsUpToThree() {
        var hints = service.findHints(
                "인공지능은 자료의 출처를 확인해야 해요.",
                List.of("개인정보를 입력한다", "편향을 확인한다")
        );

        assertThat(hints).hasSize(3);
        assertThat(hints).extracting("term").containsExactly("인공지능", "자료", "편향");
    }

    @Test
    void findHintsReturnsEmptyListWhenNothingMatches() {
        assertThat(service.findHints("오늘 날씨를 골라요.", List.of("맑음", "흐림"))).isEmpty();
    }
}
