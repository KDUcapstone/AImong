package com.aimong.backend.domain.mission.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.GenerationPhase;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.entity.QuestionPoolStatus;
import com.aimong.backend.domain.mission.entity.QuestionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuestionAnswerMatcherTest {

    private final QuestionAnswerMatcher matcher = new QuestionAnswerMatcher(new ObjectMapper());

    @Test
    void acceptsOneBasedIndexAndOptionTextForChoiceQuestions() {
        QuestionBank question = multipleQuestion();

        assertThat(matcher.matches(question, "2", "2")).isTrue();
        assertThat(matcher.matches(question, "2", "꽃을 구별해 주는 카메라 앱")).isTrue();
        assertThat(matcher.displayAnswer(question, "2")).isEqualTo("꽃을 구별해 주는 카메라 앱");
    }

    @Test
    void acceptsOxAliases() {
        QuestionBank question = oxQuestion();

        assertThat(matcher.matches(question, "true", "O")).isTrue();
        assertThat(matcher.matches(question, "false", "X")).isTrue();
        assertThat(matcher.displayAnswer(question, "true")).isEqualTo("true");
    }

    @Test
    void acceptsFillOptionTextAndCombinedValues() {
        QuestionBank question = multipleQuestion();

        assertThat(matcher.matches(question, "[2]", "꽃을 구별해 주는 카메라 앱")).isTrue();
        assertThat(matcher.displayAnswer(question, "[2]")).isEqualTo("꽃을 구별해 주는 카메라 앱");
    }

    private QuestionBank multipleQuestion() {
        return QuestionBank.create(
                UUID.randomUUID(),
                QuestionType.MULTIPLE,
                "Which one is AI?",
                "[\"빈 공책\",\"꽃을 구별해 주는 카메라 앱\",\"색종이를 접는 종이 설명서\",\"바람개비 장난감\"]",
                "[\"FACT\"]",
                "KERIS",
                DifficultyBand.LOW,
                "STATIC",
                GenerationPhase.PREGENERATED,
                null,
                QuestionPoolStatus.ACTIVE
        );
    }

    private QuestionBank oxQuestion() {
        return QuestionBank.create(
                UUID.randomUUID(),
                QuestionType.OX,
                "AI can identify images.",
                null,
                "[\"FACT\"]",
                "KERIS",
                DifficultyBand.LOW,
                "STATIC",
                GenerationPhase.PREGENERATED,
                null,
                QuestionPoolStatus.ACTIVE
        );
    }
}
