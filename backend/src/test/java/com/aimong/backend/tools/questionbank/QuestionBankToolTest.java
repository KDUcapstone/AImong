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
        Path output = tempDir.resolve("question-bank-serve.sql");

        QuestionBankTool.main(new String[] {
                "serve-sql",
                "_generated/question-bank/question-bank-960-diversified-v4-polished.json",
                output.toString()
        });

        String sql = Files.readString(output);
        assertThat(sql).contains("INSERT INTO missions (id, stage, title, mission_code, description, unlock_condition, is_active) VALUES");
        assertThat(sql).contains("INSERT INTO question_bank (id, mission_id, question_type, prompt, options, content_tags, curriculum_ref, difficulty, source_type, generation_phase, pack_no, difficulty_band, question_pool_status, is_active) VALUES");
        assertThat(sql).contains("'PREGENERATED'");
        assertThat(sql).contains("'ACTIVE'");
        assertThat(sql).contains("'LOW'");
        assertThat(sql).contains("'MEDIUM'");
        assertThat(sql).contains("'HIGH'");
        assertThat(sql).contains("'생활 속 AI 도구와 AI의 기본 개념을 배워요'");
        assertThat(sql).doesNotContain("'KERIS-1 Ch2.1 pp.27-29; Ch3.1 pp.83-96; D0qG389 STEP 1', NULL, TRUE");
        assertThat(sql).contains("mission_code = EXCLUDED.mission_code");
        assertThat(sql).contains("UPDATE missions SET is_active = FALSE WHERE mission_code IS NULL OR mission_code NOT IN");
        assertThat(sql).contains("UPDATE question_bank SET is_active = FALSE WHERE mission_id IN (SELECT id FROM missions WHERE is_active = FALSE)");
        assertThat(sql).contains("pack_no = EXCLUDED.pack_no");
        assertThat(sql).contains("difficulty_band = EXCLUDED.difficulty_band");
        assertThat(sql).contains("question_pool_status = EXCLUDED.question_pool_status");
    }
}
