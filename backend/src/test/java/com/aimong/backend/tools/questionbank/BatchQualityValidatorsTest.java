package com.aimong.backend.tools.questionbank;

import static org.assertj.core.api.Assertions.assertThat;

import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.QuestionType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BatchQualityValidatorsTest {

    @Test
    void detectsIdenticalSixPackSlotRepetition() {
        List<AuditQuestion> questions = List.of(
                question("S0101-P1-01", 1, QuestionType.OX, true, null, "같은 핵심 질문입니다?"),
                question("S0101-P2-01", 2, QuestionType.OX, true, null, "같은 핵심 질문입니다?"),
                question("S0101-P3-01", 3, QuestionType.OX, true, null, "같은 핵심 질문입니다?"),
                question("S0101-P4-01", 4, QuestionType.OX, true, null, "같은 핵심 질문입니다?"),
                question("S0101-P5-01", 5, QuestionType.OX, true, null, "같은 핵심 질문입니다?"),
                question("S0101-P6-01", 6, QuestionType.OX, true, null, "같은 핵심 질문입니다?")
        );

        CoreQuestionDiversityValidator.CoreQuestionDiversityReport report =
                new CoreQuestionDiversityValidator().validate(questions);

        assertThat(report.identicalSixPackSlotCount()).isEqualTo(1);
    }

    @Test
    void detectsStrongCorrectOptionLengthBias() {
        AuditQuestion question = question(
                "S0201-P1-03",
                1,
                QuestionType.MULTIPLE,
                0,
                List.of(
                        "발표용으로 목적과 형식을 분명히 알려 주세요",
                        "해 줘",
                        "아무거나",
                        "빨리"
                ),
                "가장 좋은 질문은 무엇일까요?"
        );

        OptionLengthBiasValidator.OptionLengthBiasReport report =
                new OptionLengthBiasValidator().validate(List.of(question));

        assertThat(report.correctOptionUniqueLongestCount()).isEqualTo(1);
        assertThat(report.strongOptionLengthBiasCount()).isEqualTo(1);
    }

    @Test
    void measuresAnswerIndexDistribution() {
        List<AuditQuestion> questions = List.of(
                question("S0201-P1-01", 1, QuestionType.MULTIPLE, 0, List.of("A", "B", "C", "D"), "Q1?"),
                question("S0201-P1-02", 1, QuestionType.MULTIPLE, 0, List.of("A", "B", "C", "D"), "Q2?"),
                question("S0201-P1-03", 1, QuestionType.MULTIPLE, 3, List.of("A", "B", "C", "D"), "Q3?")
        );

        AnswerIndexBalanceValidator.AnswerIndexBalanceReport report =
                new AnswerIndexBalanceValidator().validate(questions);

        assertThat(report.multipleDistribution()).isEqualTo(Map.of(0, 2, 1, 0, 2, 0, 3, 1));
        assertThat(report.multipleMaxMinRatio()).isEqualTo(Double.POSITIVE_INFINITY);
    }

    @Test
    void detectsRepeatedExplanationSuffixPatterns() {
        List<AuditQuestion> questions = java.util.stream.IntStream.rangeClosed(1, 8)
                .mapToObj(index -> new AuditQuestion(
                        "S0201-P1-0" + index,
                        "S0201",
                        (short) 2,
                        "STEP 2",
                        "Prompt mission",
                        QuestionType.FILL,
                        "질문 " + index,
                        null,
                        "답",
                        "이유를 분명히 쓰면 더 알맞아요.",
                        List.of("PROMPT"),
                        "KERIS",
                        2,
                        DifficultyBand.LOW,
                        1,
                        "GPT"
                ))
                .toList();

        ExplanationVariationValidator.ExplanationVariationReport report =
                new ExplanationVariationValidator().validate(questions);

        assertThat(report.repeatedExplanationSuffixPatternCount()).isEqualTo(1);
        assertThat(report.overusedExplanationEndings()).isNotEmpty();
    }

    private AuditQuestion question(
            String externalId,
            int packNo,
            QuestionType type,
            Object answer,
            List<String> options,
            String question
    ) {
        return new AuditQuestion(
                externalId,
                "S0201",
                (short) 2,
                "STEP 2",
                "Prompt mission",
                type,
                question,
                options,
                answer,
                "이유를 설명해요.",
                List.of("PROMPT"),
                "KERIS",
                2,
                DifficultyBand.LOW,
                packNo,
                "GPT"
        );
    }
}
