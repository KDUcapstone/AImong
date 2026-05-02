package com.aimong.backend.domain.mission.service.generation;

import static org.assertj.core.api.Assertions.assertThat;

import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.QuestionType;
import java.util.List;
import org.junit.jupiter.api.Test;

class BatchDistributionValidatorTest {

    private final BatchDistributionValidator validator = new BatchDistributionValidator();

    @Test
    void warnsWhenTypeDistributionDriftsTooFar() {
        List<StructuredQuestionSchema> batch = List.of(
                candidate(QuestionType.MULTIPLE, DifficultyBand.HIGH, "Q1"),
                candidate(QuestionType.MULTIPLE, DifficultyBand.HIGH, "Q2"),
                candidate(QuestionType.MULTIPLE, DifficultyBand.HIGH, "Q3"),
                candidate(QuestionType.MULTIPLE, DifficultyBand.MEDIUM, "Q4"),
                candidate(QuestionType.MULTIPLE, DifficultyBand.HIGH, "Q5"),
                candidate(QuestionType.MULTIPLE, DifficultyBand.HIGH, "Q6")
        );

        BatchDistributionValidator.BatchDistributionReport report = validator.validate(batch);

        assertThat(report.warnings()).contains("batch.type_ratio_drift.MULTIPLE", "batch.type_variety_too_low");
        assertThat(report.warnings()).contains("batch.high_band_overrepresented");
    }

    @Test
    void staysCleanForBalancedMixedBatch() {
        List<StructuredQuestionSchema> batch = List.of(
                candidate(QuestionType.OX, DifficultyBand.LOW, "Q1"),
                candidate(QuestionType.MULTIPLE, DifficultyBand.LOW, "Q2"),
                candidate(QuestionType.MULTIPLE, DifficultyBand.MEDIUM, "Q3"),
                candidate(QuestionType.FILL, DifficultyBand.MEDIUM, "Q4"),
                candidate(QuestionType.SITUATION, DifficultyBand.HIGH, "Q5"),
                candidate(QuestionType.SITUATION, DifficultyBand.LOW, "Q6")
        );

        BatchDistributionValidator.BatchDistributionReport report = validator.validate(batch);

        assertThat(report.warnings()).isEmpty();
        assertThat(report.score()).isEqualTo(100);
    }

    private StructuredQuestionSchema candidate(QuestionType type, DifficultyBand band, String question) {
        return new StructuredQuestionSchema(
                "S0203",
                1,
                band,
                type,
                question + "?",
                type == QuestionType.OX ? null : List.of("A", "B", "C", "D"),
                type == QuestionType.OX ? true : 0,
                "Because it is safer.",
                List.of("PRIVACY"),
                "KERIS",
                2
        );
    }
}
