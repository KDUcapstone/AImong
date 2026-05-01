package com.aimong.backend.domain.mission.service.generation;

import static org.assertj.core.api.Assertions.assertThat;

import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.QuestionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuestionValidationServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final KerisCurriculumRegistry registry = new KerisCurriculumRegistry();
    private final QuestionValidationService service = new QuestionValidationService(
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

    @Test
    void rejectsUnsupportedTypeOrSchemaMismatch() {
        StructuredQuestionSchema candidate = new StructuredQuestionSchema(
                "S0203",
                1,
                DifficultyBand.LOW,
                QuestionType.OX,
                "개인정보를 넣어도 괜찮을까요?",
                List.of("네", "아니요"),
                "yes",
                "안전합니다.",
                List.of("PRIVACY", "UNKNOWN"),
                "",
                2
        );

        QuestionValidationReport report = service.validate(
                new QuestionValidationService.ValidationRequest(candidate, List.of(), List.of("다음 중 개인정보는 무엇일까요?"))
        );

        assertThat(report.pass()).isFalse();
        assertThat(report.hardFailReasons()).contains("schema.ox_options_must_be_null", "schema.ox_answer_must_be_boolean");
        assertThat(report.hardFailReasons()).contains("schema.unsupported_content_tag");
    }

    @Test
    void rejectsExplanationOverTwoSentences() {
        StructuredQuestionSchema candidate = validStep2Candidate(
                "학교 발표용 문장을 AI에게 물을 때 어떤 정보가 더 안전할까요?",
                List.of("우리 집 주소", "가족의 이름", "친구 전화번호", "발표 주제"),
                3,
                "발표 주제는 개인정보가 아닙니다. 그래서 안전합니다. 추가 설명입니다."
        );

        QuestionValidationReport report = service.validate(
                new QuestionValidationService.ValidationRequest(candidate, List.of(), List.of())
        );

        assertThat(report.hardFailReasons()).contains("explanation.too_many_sentences");
    }

    @Test
    void rejectsPersonalDataInputPrompt() {
        StructuredQuestionSchema candidate = validStep2Candidate(
                "Upload your real address and phone number to the AI app and make an intro.",
                List.of("yes", "no", "maybe", "not sure"),
                1,
                "Real personal data should not be entered."
        );

        QuestionValidationReport report = service.validate(
                new QuestionValidationService.ValidationRequest(candidate, List.of(), List.of())
        );

        assertThat(report.hardFailReasons()).contains("safety.personal_data_request");
    }

    @Test
    void rejectsStep1QuestionWithStep3Concepts() {
        StructuredQuestionSchema candidate = new StructuredQuestionSchema(
                "S0101",
                1,
                DifficultyBand.LOW,
                QuestionType.MULTIPLE,
                "출처와 날짜, 기관을 비교할 때 가장 믿을 만한 설명을 고르세요.",
                List.of("보기1", "보기2", "보기3", "보기4"),
                0,
                "출처 비교가 중요합니다.",
                List.of("FACT", "VERIFICATION"),
                "KERIS-1 Ch2.1 pp.27-29; Ch3.1 pp.83-96; D0qG389 STEP 1",
                1
        );

        QuestionValidationReport report = service.validate(
                new QuestionValidationService.ValidationRequest(candidate, List.of(), List.of())
        );

        assertThat(report.hardFailReasons()).contains("curriculum.step1_contains_step3_concepts");
    }

    @Test
    void allowsStep3SourceComparisonQuestion() {
        StructuredQuestionSchema candidate = new StructuredQuestionSchema(
                "S0302",
                1,
                DifficultyBand.LOW,
                QuestionType.MULTIPLE,
                "두 글의 날짜와 기관, 근거를 비교할 때 먼저 볼 것은 무엇일까요?",
                List.of("글 길이", "작성 날짜와 기관", "이모지 개수", "배경색"),
                1,
                "날짜와 기관을 보면 정보가 언제, 누가 만들었는지 확인할 수 있어 비교에 도움이 됩니다.",
                List.of("VERIFICATION", "FACT"),
                "KERIS-1 Ch4.1-4.2 pp.157-178; D0qG389 STEP 3",
                3
        );

        QuestionValidationReport report = service.validate(
                new QuestionValidationService.ValidationRequest(candidate, List.of(), List.of())
        );

        assertThat(report.hardFailReasons()).isEmpty();
        assertThat(report.scores().stageFit()).isGreaterThanOrEqualTo(90);
        assertThat(report.scores().kerisThreeStepFit()).isGreaterThanOrEqualTo(90);
        assertThat(report.scores().structureQuotaTypeRules()).isGreaterThanOrEqualTo(90);
    }

    @Test
    void rejectsAmbiguousDuplicateOptions() {
        StructuredQuestionSchema candidate = validStep2Candidate(
                "발표 자료에 넣을 안전한 정보는 무엇일까요?",
                List.of("가족의 이름", "가족의 이름", "좋아하는 색", "학습 목표"),
                0,
                "가족의 이름은 실제 개인정보가 아닙니다."
        );

        QuestionValidationReport report = service.validate(
                new QuestionValidationService.ValidationRequest(candidate, List.of(), List.of())
        );

        assertThat(report.hardFailReasons()).contains("answer.duplicate_options");
    }

    @Test
    void rejectsWhenCorrectAnswerAndDistractorAreTooSimilar() {
        StructuredQuestionSchema candidate = validStep2Candidate(
                "AI에게 발표 도움을 받을 때 넣지 말아야 할 정보는 무엇일까요?",
                List.of("우리 집 주소", "우리집 주소", "발표 제목", "발표 형식"),
                0,
                "집 주소는 실제 개인정보라서 넣지 않는 것이 안전합니다."
        );

        QuestionValidationReport report = service.validate(
                new QuestionValidationService.ValidationRequest(candidate, List.of(), List.of())
        );

        assertThat(report.hardFailReasons()).containsAnyOf(
                "answer.duplicate_options",
                "answer.correct_and_distractor_too_similar",
                "answer.multiple_options_mean_too_similar"
        );
    }

    @Test
    void rejectsWhenMultipleOptionsEchoTheStem() {
        StructuredQuestionSchema candidate = validStep2Candidate(
                "발표 제목, 발표 주제, 발표 내용 제목 중에서 AI에 넣어도 되는 정보는 무엇일까요?",
                List.of("발표 제목", "발표 주제", "발표 내용 제목", "집 주소"),
                0,
                "발표 제목처럼 개인정보가 아닌 학습 정보는 넣어도 됩니다."
        );

        QuestionValidationReport report = service.validate(
                new QuestionValidationService.ValidationRequest(candidate, List.of(), List.of())
        );

        assertThat(report.hardFailReasons()).contains("answer.multiple_options_match_stem");
    }

    @Test
    void warnsOrFailsWhenTooCloseToGoldExample() {
        StructuredQuestionSchema candidate = validStep2Candidate(
                "다음 중 가족 소개에 넣으면 안 되는 개인정보는 무엇일까요?",
                List.of("오늘 급식 메뉴", "좋아하는 색", "전화번호와 집 주소", "발표 주제"),
                2,
                "전화번호와 집 주소는 실제로 사람을 찾을 수 있는 정보라서 조심해야 합니다."
        );

        QuestionValidationReport report = service.validate(
                new QuestionValidationService.ValidationRequest(
                        candidate,
                        List.of(),
                        List.of("다음 중 가족 소개에 넣으면 안 되는 개인정보는 무엇일까요?")
                )
        );

        assertThat(report.softWarnings()).anyMatch(value -> value.contains("gold_example"));
    }

    @Test
    void producesStableJsonReportShape() throws Exception {
        StructuredQuestionSchema candidate = validStep2Candidate(
                "학교 발표용 문장을 AI에게 물을 때 어떤 정보가 더 안전할까요?",
                List.of("우리 집 주소", "가족의 이름", "친구 전화번호", "발표 주제"),
                3,
                "발표 주제는 실제 연락처나 주소가 아니어서 비교적 안전합니다."
        );

        QuestionValidationReport report = service.validate(
                new QuestionValidationService.ValidationRequest(candidate, List.of(), List.of("학교 발표용 문장을 AI에게 물을 때 어떤 정보가 더 안전할까요?"))
        );
        String json = objectMapper.writeValueAsString(report);

        assertThat(json).contains("\"pass\"");
        assertThat(json).contains("\"recommendedAction\"");
        assertThat(json).contains("\"scores\"");
        assertThat(json).contains("\"criteriaFit\"");
        assertThat(json).contains("\"kerisThreeStepFit\"");
        assertThat(json).contains("\"elementaryDifficultyDirection\"");
        assertThat(json).contains("\"structureQuotaTypeRules\"");
        assertThat(json).contains("\"naturalness\"");
        assertThat(json).contains("\"duplicationOriginality\"");
        assertThat(json).contains("\"productionReadiness\"");
        assertThat(json).contains("\"normalizedQuestion\"");
        assertThat(json).contains("\"repairHints\"");
    }

    private StructuredQuestionSchema validStep2Candidate(
            String question,
            List<String> options,
            int answer,
            String explanation
    ) {
        return new StructuredQuestionSchema(
                "S0203",
                1,
                DifficultyBand.LOW,
                QuestionType.MULTIPLE,
                question,
                options,
                answer,
                explanation,
                List.of("PRIVACY", "SAFETY"),
                "KERIS-1 Ch2.1 pp.27-29; Ch4.2 pp.163-178; D0qG389 STEP 2",
                2
        );
    }
}
