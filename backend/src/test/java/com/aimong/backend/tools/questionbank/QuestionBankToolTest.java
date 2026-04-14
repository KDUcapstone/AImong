package com.aimong.backend.tools.questionbank;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

class QuestionBankToolTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void generatorCreatesFiveHundredValidatedQuestions() throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream("/question-bank/keris-elementary-ai-curriculum.json")) {
            CurriculumManifest manifest = objectMapper.readValue(inputStream, CurriculumManifest.class);
            QuestionBankDraft draft = new QuestionBankGenerator().generate(manifest);

            assertThat(draft.totalQuestionCount()).isEqualTo(500);
            assertThat(new QuestionBankValidator().validate(draft)).isEmpty();
        }
    }
}
