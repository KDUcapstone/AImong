package com.aimong.backend.tools.questionbank;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ValidationSyntheticFixtureCoverageTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void coversRequiredNegativeCategories() throws Exception {
        SyntheticValidationFixtureSet fixtureSet = objectMapper.readValue(
                Files.readString(Path.of("src/test/resources/questionbank/validation-synthetic-negatives.json")),
                SyntheticValidationFixtureSet.class
        );

        Set<String> categories = fixtureSet.cases().stream()
                .map(SyntheticValidationFixture::category)
                .collect(Collectors.toSet());

        assertThat(categories).contains(
                "type_answer_shape_error",
                "unsupported_tag",
                "personal_data",
                "biometric_upload",
                "stage_guardrail_step1",
                "stage_guardrail_step2",
                "step3_vocabulary",
                "identical_core_question_repetition",
                "correct_option_unique_longest_bias",
                "answer_index_collapse",
                "explanation_template_copy",
                "surface_lint",
                "explanation_length"
        );
    }

    @Test
    void includesBatchSignalsForBatchFixtures() throws Exception {
        SyntheticValidationFixtureSet fixtureSet = objectMapper.readValue(
                Files.readString(Path.of("src/test/resources/questionbank/validation-synthetic-negatives.json")),
                SyntheticValidationFixtureSet.class
        );

        assertThat(fixtureSet.cases().stream()
                .filter(SyntheticValidationFixture::isBatchCase)
                .allMatch(fixture -> fixture.expectedBatchSignals() != null && !fixture.expectedBatchSignals().isEmpty()))
                .isTrue();
    }
}
