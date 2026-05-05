package com.aimong.backend.domain.mission.service.generation;

import static org.assertj.core.api.Assertions.assertThat;

import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.QuestionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuestionQualityStrictnessTest {

    private final QuestionValidationService service = validationService();

    @Test
    void rejectsFillQuestionWithQuotedMetaBlankAndOverexposedAnswer() {
        StructuredQuestionSchema candidate = new StructuredQuestionSchema(
                "S0202",
                1,
                DifficultyBand.MEDIUM,
                QuestionType.FILL,
                "학교 과학 게시판용으로 로봇 작동 원리를 AI에게 부탁하려고 해요. 대상은 초등학교 1학년이고 형식은 '세 줄 목록'으로 하려면 질문에 넣을 조건은 '________'.",
                List.of(
                        "초등학교 1학년 눈높이로, 세 줄 목록으로",
                        "전문 용어를 상세히 써서, 여러 문단으로",
                        "수식과 표를 포함해 자세히",
                        "한 문장으로 매우 간단하게"
                ),
                List.of(0),
                "대상과 형식을 함께 알려 주면 AI가 더 알맞게 답합니다.",
                List.of("PROMPT", "SAFETY"),
                "KERIS-1 Ch3.1 pp.83-96; Ch3.4-3.5 pp.119-154; D0qG389 STEP 2",
                3
        );

        QuestionValidationReport report = service.validate(
                new QuestionValidationService.ValidationRequest(candidate, List.of(), List.of())
        );

        assertThat(report.pass()).isFalse();
        assertThat(report.hardFailReasons()).contains("naturalness.fill_blank_quoted_or_meta");
        assertThat(report.repairHints()).anyMatch(hint -> hint.contains("natural sentence blank"));
    }

    @Test
    void rejectsFillQuestionThatRevealsMostOfCorrectOption() {
        StructuredQuestionSchema candidate = new StructuredQuestionSchema(
                "S0202",
                1,
                DifficultyBand.MEDIUM,
                QuestionType.FILL,
                "AI에게 초등학교 1학년 눈높이로 세 줄 목록으로 설명해 달라고 부탁하려면 ____을 조건에 넣어요.",
                List.of(
                        "초등학교 1학년 눈높이로 세 줄 목록으로",
                        "전문 용어를 길게 사용해서",
                        "출처 없이 바로 정답만",
                        "친구 이름을 넣어서"
                ),
                List.of(0),
                "대상과 형식을 정하면 더 알맞은 답을 받을 수 있습니다.",
                List.of("PROMPT", "SAFETY"),
                "KERIS-1 Ch3.1 pp.83-96; Ch3.4-3.5 pp.119-154; D0qG389 STEP 2",
                3
        );

        QuestionValidationReport report = service.validate(
                new QuestionValidationService.ValidationRequest(candidate, List.of(), List.of())
        );

        assertThat(report.pass()).isFalse();
        assertThat(report.hardFailReasons()).contains("answer.fill_answer_overexposed_in_stem");
    }

    @Test
    void rejectsOxQuestionThatOnlyRestatesPromptCondition() {
        StructuredQuestionSchema candidate = new StructuredQuestionSchema(
                "S0202",
                1,
                DifficultyBand.LOW,
                QuestionType.OX,
                "질문에 3개 항목으로 간단히 적어 달라고 하면 인공지능이 보통 3개짜리 간단한 목록을 만들어 줘요.",
                null,
                true,
                "항목 수나 형식을 알려 주면 AI는 그 조건에 맞춰 답을 만들려고 합니다.",
                List.of("PROMPT", "VERIFICATION"),
                "KERIS-1 Ch3.1 pp.83-96; Ch3.4-3.5 pp.119-154; D0qG389 STEP 2",
                2
        );

        QuestionValidationReport report = service.validate(
                new QuestionValidationService.ValidationRequest(candidate, List.of(), List.of())
        );

        assertThat(report.pass()).isFalse();
        assertThat(report.hardFailReasons()).contains("naturalness.ox_tautological_condition_result");
    }

    @Test
    void rejectsExplanationOptionNumberMismatch() {
        StructuredQuestionSchema candidate = new StructuredQuestionSchema(
                "S0202",
                1,
                DifficultyBand.HIGH,
                QuestionType.SITUATION,
                "동생 눈높이로 기후 변화를 알려 달라고 AI에게 부탁하려고 해요. 가장 알맞은 요청은 무엇일까요?",
                List.of(
                        "기후 변화에 대해 알려줘.",
                        "초등 2학년인 동생이 이해할 수 있게 쉬운 말로 3문장으로 설명해 주고, 짧은 예시 문장 하나 포함해 줘.",
                        "중학생 수준으로 용어와 원리를 자세히 설명한 긴 글로 작성해 줘.",
                        "전문 용어를 많이 사용해 줘."
                ),
                2,
                "정답은 2번입니다. 대상과 길이를 구체적으로 담고 있습니다.",
                List.of("PROMPT", "SAFETY", "VERIFICATION"),
                "KERIS-1 Ch3.1 pp.83-96; Ch3.4-3.5 pp.119-154; D0qG389 STEP 2",
                3
        );

        QuestionValidationReport report = service.validate(
                new QuestionValidationService.ValidationRequest(candidate, List.of(), List.of())
        );

        assertThat(report.pass()).isFalse();
        assertThat(report.hardFailReasons()).contains("answer.explanation_option_number_mismatch");
    }

    @Test
    void rejectsCorrectOptionThatContradictsStemCount() {
        StructuredQuestionSchema candidate = new StructuredQuestionSchema(
                "S0202",
                1,
                DifficultyBand.MEDIUM,
                QuestionType.SITUATION,
                "학교 신문에 실을 50자짜리 소개 글을 AI에게 부탁하려고 해요. 함께 알려 주면 가장 알맞은 조건은 무엇일까요?",
                List.of(
                        "대상: 같은 반 친구, 길이: 50자 이내, 형식: 한 문장",
                        "대상: 전교 학생, 길이: 200자 이상, 형식: 여러 문단",
                        "대상: 담임 선생님, 길이: 50자 이내, 형식: 한 단어",
                        "길이: 50자 이내, 형식: 핵심 키워드 3개"
                ),
                1,
                "대상과 길이, 형식을 함께 알려 주면 AI가 맞춤형으로 글을 줄 가능성이 높습니다.",
                List.of("PROMPT", "VERIFICATION", "SAFETY"),
                "KERIS-1 Ch3.1 pp.83-96; Ch3.4-3.5 pp.119-154; D0qG389 STEP 2",
                3
        );

        QuestionValidationReport report = service.validate(
                new QuestionValidationService.ValidationRequest(candidate, List.of(), List.of())
        );

        assertThat(report.pass()).isFalse();
        assertThat(report.hardFailReasons()).contains("answer.correct_option_contradicts_stem");
    }

    @Test
    void rejectsFillQuestionWithUngrammaticalBlankEnding() {
        StructuredQuestionSchema candidate = new StructuredQuestionSchema(
                "S0202",
                1,
                DifficultyBand.MEDIUM,
                QuestionType.FILL,
                "좋은 질문은 대상과 길이를 함께 넣을 때 더 ____ 있어요.",
                List.of(
                        "정확해질",
                        "짧아질",
                        "흐려질",
                        "늦어질"
                ),
                List.of(0),
                "대상과 길이 같은 조건을 함께 넣으면 AI가 답의 방향을 더 잘 잡을 수 있어요.",
                List.of("PROMPT", "VERIFICATION", "SAFETY"),
                "KERIS-1 Ch3.1 pp.83-96; Ch3.4-3.5 pp.119-154; D0qG389 STEP 2",
                3
        );

        QuestionValidationReport report = service.validate(
                new QuestionValidationService.ValidationRequest(candidate, List.of(), List.of())
        );

        assertThat(report.pass()).isFalse();
        assertThat(report.hardFailReasons()).contains("naturalness.fill_ungrammatical_blank_ending");
    }

    @Test
    void rejectsHighBandQuestionWithTooObviousAnswerSet() {
        StructuredQuestionSchema candidate = new StructuredQuestionSchema(
                "S0202",
                1,
                DifficultyBand.HIGH,
                QuestionType.FILL,
                "친구에게 보낼 짧은 안내문을 만들 때 설명을 ____ 써 달라고 요청하세요.",
                List.of(
                        "짧고 쉬운 말로",
                        "긴 문장으로 자세히",
                        "전문 용어로만",
                        "형식 없이 막 적어"
                ),
                List.of(0),
                "짧고 쉬운 말은 친구가 빠르게 이해하도록 도와줍니다.",
                List.of("PROMPT", "VERIFICATION", "SAFETY"),
                "KERIS-1 Ch3.1 pp.83-96; Ch3.4-3.5 pp.119-154; D0qG389 STEP 2",
                3
        );

        QuestionValidationReport report = service.validate(
                new QuestionValidationService.ValidationRequest(candidate, List.of(), List.of())
        );

        assertThat(report.pass()).isFalse();
        assertThat(report.hardFailReasons()).contains("answer.high_band_too_obvious");
    }

    @Test
    void rejectsCartoonishImplausibleDistractors() {
        StructuredQuestionSchema candidate = new StructuredQuestionSchema(
                "S0104",
                1,
                DifficultyBand.LOW,
                QuestionType.MULTIPLE,
                "음성 번역 앱이 어린이 발음을 자주 못 알아들을 때, 딥러닝 때문이라고 생각할 때 가장 먼저 확인할 것은?",
                List.of(
                        "앱이 사람처럼 일부러 틀린다",
                        "앱이 배운 어린이 발음 예시가 충분한지",
                        "앱이 전기를 아껴서 일부러 작동을 멈춘다",
                        "앱이 사용자의 기분을 읽고 판단한다"
                ),
                1,
                "딥러닝은 학습한 자료를 바탕으로 판단하므로 어린이 발음 예시가 부족하면 인식이 잘 안 될 수 있습니다.",
                List.of("FACT", "VERIFICATION"),
                "KERIS-1 Ch2.1 pp.27-29; Ch2.3 pp.61-80; Ch2.1 pp.28-29",
                1
        );

        QuestionValidationReport report = service.validate(
                new QuestionValidationService.ValidationRequest(candidate, List.of(), List.of())
        );

        assertThat(report.pass()).isFalse();
        assertThat(report.hardFailReasons()).contains("answer.implausible_distractors");
    }

    private QuestionValidationService validationService() {
        ObjectMapper objectMapper = new ObjectMapper();
        KerisCurriculumRegistry registry = new KerisCurriculumRegistry();
        return new QuestionValidationService(
                new SchemaValidator(),
                new SafetyValidator(),
                new CurriculumFitValidator(registry),
                new StructureRuleValidator(registry),
                new ElementaryReadabilityValidator(),
                new AnswerQualityValidator(),
                new ExplanationQualityValidator(),
                new NaturalnessValidator(),
                new KoreanSurfaceLintValidator(),
                new Step3VocabularyCeilingValidator(),
                new SimilarityDeduplicator(),
                new KerisGoldExampleRegistry(objectMapper),
                objectMapper
        );
    }
}
