package com.aimong.backend.domain.mission.service.generation;

import static org.assertj.core.api.Assertions.assertThat;

import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.QuestionType;
import java.util.List;
import org.junit.jupiter.api.Test;

class Step3VocabularyCeilingValidatorTest {

    private final Step3VocabularyCeilingValidator validator = new Step3VocabularyCeilingValidator();

    @Test
    void flagsOverAdvancedStep3Vocabulary() {
        ValidationSubResult result = validator.validate(new StructuredQuestionSchema(
                "S0305",
                1,
                DifficultyBand.HIGH,
                QuestionType.SITUATION,
                "이해관계자와 자동화 편향을 모두 고려해 Moral Machine 장면을 분석해 보세요.",
                List.of("A", "B", "C", "D"),
                0,
                "윤리 프레임으로 설명해 보세요.",
                List.of("FACT", "VERIFICATION"),
                "KERIS",
                4
        ));

        assertThat(result.softWarnings()).contains("step3.vocabulary_ceiling");
        assertThat(result.hardFailReasons()).contains("step3.vocabulary_ceiling_exceeded");
    }
}
