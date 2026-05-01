package com.aimong.backend.domain.mission.service.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.QuestionType;
import java.util.List;
import org.junit.jupiter.api.Test;

class SchemaValidatorTest {

    private final SchemaValidator validator = new SchemaValidator();

    @Test
    void passesWithoutPackNoWhenCanonicalDifficultyExists() {
        ValidationSubResult result = validator.validate(candidate(0, DifficultyBand.LOW, null, 0, QuestionType.MULTIPLE, List.of("FACT")));

        assertThat(result.hardFailReasons()).doesNotContain("schema.invalid_pack_no");
        assertThat(result.hardFailReasons()).doesNotContain("schema.invalid_difficulty");
        assertThat(result.hardFailReasons()).doesNotContain("schema.missing_difficulty");
    }

    @Test
    void passesWithoutLegacyDifficultyBandWhenCanonicalDifficultyExists() {
        ValidationSubResult result = validator.validate(candidate(0, null, DifficultyBand.MEDIUM, 0, QuestionType.MULTIPLE, List.of("FACT")));

        assertThat(result.hardFailReasons()).doesNotContain("schema.missing_difficulty");
    }

    @Test
    void rejectsLegacyNumericDifficultyWithoutCanonicalDifficulty() {
        ValidationSubResult result = validator.validate(candidate(0, null, null, 2, QuestionType.MULTIPLE, List.of("FACT")));

        assertThat(result.hardFailReasons()).contains("schema.missing_difficulty", "schema.legacy_numeric_difficulty_requires_conversion");
    }

    @Test
    void allowsOfficialContentTags() {
        ValidationSubResult result = validator.validate(candidate(0, DifficultyBand.MEDIUM, null, 0, QuestionType.MULTIPLE, List.of("FACT", "PRIVACY")));

        assertThat(result.hardFailReasons()).doesNotContain("schema.unsupported_content_tag");
    }

    @Test
    void rejectsUnsupportedContentTags() {
        ValidationSubResult result = validator.validate(candidate(0, DifficultyBand.HIGH, null, 0, QuestionType.MULTIPLE, List.of("FACT", "UNKNOWN")));

        assertThat(result.hardFailReasons()).contains("schema.unsupported_content_tag");
    }

    @Test
    void canonicalLowMediumHighDifficultiesPass() {
        assertThat(validator.validate(candidate(0, null, DifficultyBand.LOW, 0, QuestionType.MULTIPLE, List.of("FACT"))).hardFailReasons())
                .doesNotContain("schema.missing_difficulty");
        assertThat(validator.validate(candidate(0, null, DifficultyBand.MEDIUM, 0, QuestionType.MULTIPLE, List.of("FACT"))).hardFailReasons())
                .doesNotContain("schema.missing_difficulty");
        assertThat(validator.validate(candidate(0, null, DifficultyBand.HIGH, 0, QuestionType.MULTIPLE, List.of("FACT"))).hardFailReasons())
                .doesNotContain("schema.missing_difficulty");
    }

    @Test
    void factPrivacyPromptSafetyVerificationAreNotQuestionTypes() {
        assertThatThrownBy(() -> QuestionType.valueOf("FACT")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> QuestionType.valueOf("PRIVACY")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> QuestionType.valueOf("PROMPT")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> QuestionType.valueOf("SAFETY")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> QuestionType.valueOf("VERIFICATION")).isInstanceOf(IllegalArgumentException.class);
    }

    private StructuredQuestionSchema candidate(
            int packNo,
            DifficultyBand difficultyBand,
            DifficultyBand canonicalDifficulty,
            int numericDifficulty,
            QuestionType type,
            List<String> contentTags
    ) {
        return new StructuredQuestionSchema(
                "S0203",
                packNo,
                difficultyBand,
                type,
                "What information should stay private?",
                List.of("password", "favorite color", "class subject", "pet name"),
                0,
                "Passwords are private because they protect your account.",
                contentTags,
                "KERIS-REF",
                numericDifficulty,
                canonicalDifficulty
        );
    }
}
