package com.aimong.backend.domain.mission.service.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.GenerationPhase;
import com.aimong.backend.domain.mission.entity.Mission;
import com.aimong.backend.domain.mission.entity.QuestionAnswerKey;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.entity.QuestionPoolStatus;
import com.aimong.backend.domain.mission.entity.QuestionType;
import com.aimong.backend.domain.mission.repository.QuestionAnswerKeyRepository;
import com.aimong.backend.domain.mission.service.generation.NormalizedQuestionView;
import com.aimong.backend.domain.mission.service.generation.QuestionValidationReport;
import com.aimong.backend.domain.mission.service.generation.QuestionValidationScores;
import com.aimong.backend.domain.mission.service.generation.QuestionValidationService;
import com.aimong.backend.domain.mission.service.generation.ValidationDecision;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuestionServingQualityGuardTest {

    @Mock
    private QuestionAnswerKeyRepository questionAnswerKeyRepository;

    @Mock
    private QuestionValidationService questionValidationService;

    @Mock
    private QuestionQualityReviewService questionQualityReviewService;

    @Captor
    private ArgumentCaptor<QuestionValidationService.ValidationRequest> validationRequestCaptor;

    @Test
    void infersNumericDifficultyWhenPersistedQuestionHasNoLegacyNumericDifficulty() {
        Mission mission = mission((short) 1);
        QuestionBank question = QuestionBank.create(
                UUID.randomUUID(),
                QuestionType.MULTIPLE,
                "다음 중 생활 속 인공지능 사례로 가장 알맞은 것은 무엇일까요?",
                "[\"빈 공책\",\"꽃을 구별해 주는 카메라 앱\",\"종이 설명서\",\"바람개비 장난감\"]",
                "[\"FACT\",\"VERIFICATION\"]",
                "KERIS-S01",
                DifficultyBand.LOW,
                "STATIC",
                GenerationPhase.PREGENERATED,
                (short) 1,
                DifficultyBand.LOW,
                QuestionPoolStatus.ACTIVE
        );
        QuestionAnswerKey answerKey = QuestionAnswerKey.create(
                question.getId(),
                "\"꽃을 구별해 주는 카메라 앱\"",
                "생활 속 인공지능은 보거나 듣거나 비교해 판단하는 기능을 갖는 경우가 많아요."
        );

        when(questionAnswerKeyRepository.findAllByQuestionIdIn(List.of(question.getId())))
                .thenReturn(List.of(answerKey));
        when(questionValidationService.validate(validationRequestCaptor.capture()))
                .thenReturn(passingReport());

        QuestionServingQualityGuard guard = new QuestionServingQualityGuard(
                questionAnswerKeyRepository,
                questionValidationService,
                questionQualityReviewService,
                new ObjectMapper()
        );

        var result = guard.validateForServing(mission, List.of(question));

        assertThat(result.validQuestions()).containsExactly(question);
        assertThat(validationRequestCaptor.getValue().candidate().difficulty()).isEqualTo(1);
        assertThat(validationRequestCaptor.getValue().candidate().answer()).isEqualTo(1);
    }

    private Mission mission(short stage) {
        Mission mission = mock(Mission.class);
        when(mission.getStage()).thenReturn(stage);
        when(mission.getMissionCode()).thenReturn("S0101");
        return mission;
    }

    private QuestionValidationReport passingReport() {
        QuestionValidationScores scores = new QuestionValidationScores(
                100, 100, 100, 100, 100, 100, 100, 100, 100,
                100, 100, 100, 100, 100, 100, 100, 100
        );
        NormalizedQuestionView normalizedQuestion = new NormalizedQuestionView(
                "S0101",
                "MULTIPLE",
                "question",
                List.of("a", "b", "c", "d"),
                1,
                "explanation",
                List.of("FACT"),
                1
        );
        return new QuestionValidationReport(
                true,
                ValidationDecision.SAVE,
                List.of(),
                List.of(),
                scores,
                normalizedQuestion,
                List.of()
        );
    }
}
