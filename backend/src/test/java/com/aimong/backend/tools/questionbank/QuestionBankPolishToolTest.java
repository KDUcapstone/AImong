package com.aimong.backend.tools.questionbank;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class QuestionBankPolishToolTest {

    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @TempDir
    Path tempDir;

    @Test
    void createsPolishedBankWithoutBreakingPassMetrics() throws Exception {
        Path input = Path.of("_generated/question-bank/question-bank-960-diversified-v4.json");
        Path output = tempDir.resolve("question-bank-960-diversified-v4-polished.json");
        Path reportMd = tempDir.resolve("question-bank-v4-polish-report.md");
        Path reportJson = tempDir.resolve("question-bank-v4-polish-report.json");

        QuestionBankPolishTool.main(new String[] {
                input.toString(),
                output.toString(),
                reportMd.toString(),
                reportJson.toString()
        });

        assertThat(Files.exists(output)).isTrue();
        assertThat(Files.exists(reportMd)).isTrue();
        assertThat(Files.exists(reportJson)).isTrue();

        QuestionBankAuditLoader loader = new QuestionBankAuditLoader(objectMapper);
        AuditQuestionBank before = loader.load(input);
        AuditQuestionBank after = loader.load(output);

        PostPolishAuditRunner runner = new PostPolishAuditRunner();
        PostPolishAuditRunner.BatchMetrics beforeMetrics = runner.evaluate(before);
        PostPolishAuditRunner.BatchMetrics afterMetrics = runner.evaluate(after);

        assertThat(after.totalQuestionCount()).isEqualTo(before.totalQuestionCount());
        assertThat(afterMetrics.verdict()).isEqualTo("PASS");
        assertThat(afterMetrics.repeatedExplanationSuffixPatternCount())
                .isLessThan(beforeMetrics.repeatedExplanationSuffixPatternCount());
        assertThat(afterMetrics.answerOptionStyleImbalanceWarnings())
                .isLessThan(beforeMetrics.answerOptionStyleImbalanceWarnings());
        assertThat(afterMetrics.strongOptionLengthBiasCount())
                .isLessThanOrEqualTo(beforeMetrics.strongOptionLengthBiasCount());
        assertThat(afterMetrics.multipleAnswerIndexMaxMinRatio())
                .isEqualTo(beforeMetrics.multipleAnswerIndexMaxMinRatio());
        assertThat(afterMetrics.situationAnswerIndexMaxMinRatio())
                .isEqualTo(beforeMetrics.situationAnswerIndexMaxMinRatio());
        assertThat(afterMetrics.koreanSurfaceLintHits()).isZero();
        assertThat(afterMetrics.step3VocabularyHits()).isZero();

        Map<String, JsonNode> beforeQuestions = questionsById(objectMapper.readTree(Files.readString(input)));
        Map<String, JsonNode> afterQuestions = questionsById(objectMapper.readTree(Files.readString(output)));
        assertThat(afterQuestions.keySet()).isEqualTo(beforeQuestions.keySet());

        int changedCount = 0;
        for (Map.Entry<String, JsonNode> entry : beforeQuestions.entrySet()) {
            JsonNode beforeQuestion = entry.getValue();
            JsonNode afterQuestion = afterQuestions.get(entry.getKey());
            assertThat(afterQuestion.get("answer")).isEqualTo(beforeQuestion.get("answer"));
            assertThat(sentenceCount(afterQuestion.path("explanation").asText())).isLessThanOrEqualTo(2);
            if (!afterQuestion.path("explanation").asText().equals(beforeQuestion.path("explanation").asText())
                    || !afterQuestion.path("options").equals(beforeQuestion.path("options"))) {
                changedCount++;
            }
        }
        assertThat(changedCount).isGreaterThan(0);
    }

    private Map<String, JsonNode> questionsById(JsonNode root) {
        Map<String, JsonNode> values = new LinkedHashMap<>();
        root.withArray("questions").forEach(node -> values.put(node.path("externalId").asText(), node));
        return values;
    }

    private int sentenceCount(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        String[] parts = value.trim().split("(?<=[.!?])\\s+");
        return parts.length;
    }
}
