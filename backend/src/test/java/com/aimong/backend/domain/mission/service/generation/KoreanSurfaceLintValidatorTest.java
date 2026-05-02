package com.aimong.backend.domain.mission.service.generation;

import static org.assertj.core.api.Assertions.assertThat;

import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.QuestionType;
import java.util.List;
import org.junit.jupiter.api.Test;

class KoreanSurfaceLintValidatorTest {

    private final KoreanSurfaceLintValidator validator = new KoreanSurfaceLintValidator();

    @Test
    void rejectsObviousParticleErrors() {
        ValidationSubResult result = validator.validate(new StructuredQuestionSchema(
                "S0203",
                1,
                DifficultyBand.LOW,
                QuestionType.MULTIPLE,
                "자료을 AI에 넣어도 될까요?",
                List.of("된다", "안 된다", "모른다", "상관없다"),
                1,
                "자료을 그대로 넣으면 안 돼요.",
                List.of("PRIVACY"),
                "KERIS",
                2
        ));

        assertThat(result.hardFailReasons()).contains("surface.obvious_particle_error");
    }
}
