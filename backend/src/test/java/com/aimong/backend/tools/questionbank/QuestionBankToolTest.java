package com.aimong.backend.tools.questionbank;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class QuestionBankToolTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void generatorCreatesFiveHundredValidatedQuestions() throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream("/question-bank/keris-elementary-ai-curriculum.json")) {
            CurriculumManifest manifest = objectMapper.readValue(inputStream, CurriculumManifest.class);
            QuestionBankDraft draft = new QuestionBankGenerator().generate(manifest);

            assertThat(draft.totalQuestionCount()).isEqualTo(500);
            assertThat(new QuestionBankValidator().validate(draft)).isEmpty();
        }
    }

    @Test
    void serveSqlCommandExportsRuntimeServingMetadata() throws Exception {
        Path input = tempDir.resolve("question-bank-fixture.json");
        Path output = tempDir.resolve("question-bank-serve.sql");
        Files.writeString(input, """
                {
                  "sourceTitle": "test bank",
                  "sourceReference": "test",
                  "generationVersion": "test",
                  "totalMissionCount": 3,
                  "totalQuestionCount": 3,
                  "questions": [
                    {
                      "externalId": "S0101-P1-01",
                      "missionCode": "S0101",
                      "stage": 1,
                      "stageTitle": "인공지능 이해",
                      "missionTitle": "AI 알아보기",
                      "type": "MULTIPLE",
                      "question": "AI가 학습 자료를 바탕으로 판단한다는 설명으로 알맞은 것은?",
                      "options": ["배운 예시를 바탕으로 판단한다", "무조건 정답만 말한다", "전기를 아끼려고 답한다", "기분을 읽고 답한다"],
                      "answer": 1,
                      "explanation": "AI는 학습한 자료를 바탕으로 답을 고릅니다.",
                      "contentTags": ["FACT"],
                      "curriculumRef": "KERIS-1 Ch2.1 pp.27-29",
                      "difficulty": "LOW",
                      "packNo": 1,
                      "sourceType": "STATIC"
                    },
                    {
                      "externalId": "S0102-P1-01",
                      "missionCode": "S0102",
                      "stage": 1,
                      "stageTitle": "인공지능 이해",
                      "missionTitle": "AI 활용하기",
                      "type": "OX",
                      "question": "AI 답은 확인 없이 그대로 믿기보다 다시 살펴보는 것이 좋다.",
                      "options": null,
                      "answer": true,
                      "explanation": "AI 답도 틀릴 수 있으므로 확인이 필요합니다.",
                      "contentTags": ["VERIFICATION"],
                      "curriculumRef": "KERIS-1 Ch2.3 pp.61-80",
                      "difficulty": "MEDIUM",
                      "difficultyBand": "MEDIUM",
                      "packNo": 1,
                      "sourceType": "STATIC"
                    },
                    {
                      "externalId": "S0103-P1-01",
                      "missionCode": "S0103",
                      "stage": 1,
                      "stageTitle": "인공지능 이해",
                      "missionTitle": "AI 안전하게 쓰기",
                      "type": "FILL",
                      "question": "개인정보는 AI 질문에 ____ 않는 것이 좋다.",
                      "options": ["넣지", "많이", "자주", "먼저"],
                      "answer": [1],
                      "explanation": "개인정보를 넣으면 안전하지 않을 수 있습니다.",
                      "contentTags": ["PRIVACY", "SAFETY"],
                      "curriculumRef": "KERIS-1 Ch3.4 pp.119-154",
                      "difficulty": "HIGH",
                      "difficultyBand": "HIGH",
                      "packNo": 1,
                      "sourceType": "STATIC"
                    }
                  ]
                }
                """);

        QuestionBankTool.main(new String[] {
                "serve-sql",
                input.toString(),
                output.toString()
        });

        String sql = Files.readString(output);
        assertThat(sql).contains("INSERT INTO missions (id, stage, title, mission_code, description, unlock_condition, is_active) VALUES");
        assertThat(sql).contains("INSERT INTO question_bank (id, mission_id, question_type, prompt, options, content_tags, curriculum_ref, difficulty, source_type, generation_phase, pack_no, question_pool_status, is_active) VALUES");
        assertThat(sql).contains("$aimong$PREGENERATED$aimong$");
        assertThat(sql).contains("$aimong$ACTIVE$aimong$");
        assertThat(sql).contains("$aimong$LOW$aimong$");
        assertThat(sql).contains("$aimong$MEDIUM$aimong$");
        assertThat(sql).contains("$aimong$HIGH$aimong$");
        assertThat(sql).contains("$aimong$2$aimong$");
        assertThat(sql).containsPattern("\\$aimong\\$\\[\\s*2\\s*]\\$aimong\\$");
        assertThat(sql).contains("$aimong$");
        assertThat(sql).doesNotContain("'KERIS-1 Ch2.1 pp.27-29; Ch3.1 pp.83-96; D0qG389 STEP 1', NULL, TRUE");
        assertThat(sql).contains("mission_code = EXCLUDED.mission_code");
        assertThat(sql).contains("UPDATE missions SET is_active = FALSE WHERE mission_code IS NULL OR mission_code NOT IN");
        assertThat(sql).contains("UPDATE question_bank SET is_active = FALSE WHERE mission_id IN (SELECT id FROM missions WHERE is_active = FALSE)");
        assertThat(sql).contains("pack_no = EXCLUDED.pack_no");
        assertThat(sql).doesNotContain("difficulty_band = EXCLUDED.difficulty_band");
        assertThat(sql).contains("question_pool_status = EXCLUDED.question_pool_status");
    }
}
