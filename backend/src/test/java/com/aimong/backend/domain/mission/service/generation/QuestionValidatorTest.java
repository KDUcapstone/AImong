package com.aimong.backend.domain.mission.service.generation;

import static org.assertj.core.api.Assertions.assertThat;

import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.QuestionType;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuestionValidatorTest {

    private final QuestionValidator validator = new QuestionValidator(
            new KerisCurriculumRegistry(),
            new ChildSafetyFilter()
    );

    @Test
    void validatorAcceptsWellFormedCandidate() {
        StructuredQuestionSchema candidate = new StructuredQuestionSchema(
                "S0203",
                1,
                DifficultyBand.LOW,
                QuestionType.MULTIPLE,
                "AI에게 숙제 도움을 받을 때 가장 안전한 정보는 무엇일까요?",
                List.of("가상의 이름", "좋아하는 색", "발표 주제", "원하는 문장 길이"),
                0,
                "실제 개인정보 대신 가상의 예시를 쓰는 것이 안전해요.",
                List.of("PRIVACY", "SAFETY"),
                "KERIS-1 Ch2.1 pp.27-29; Ch4.2 pp.163-178; D0qG389 STEP 2",
                2
        );

        assertThat(validator.validate(candidate)).isEmpty();
    }

    @Test
    void validatorRejectsUnsafeAndInvalidCandidate() {
        StructuredQuestionSchema candidate = new StructuredQuestionSchema(
                "S0203",
                7,
                DifficultyBand.HIGH,
                QuestionType.OX,
                "실제 주소와 전화번호를 입력해도 될까요?",
                List.of("예", "아니오"),
                "yes",
                "첫 문장입니다. 둘째 문장입니다. 셋째 문장입니다.",
                List.of("PRIVACY", "UNKNOWN"),
                "",
                4
        );

        List<String> errors = validator.validate(candidate);

        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(error -> error.contains("packNo"));
        assertThat(errors).anyMatch(error -> error.contains("OX options"));
        assertThat(errors).anyMatch(error -> error.contains("OX answer"));
        assertThat(errors).anyMatch(error -> error.contains("unsupported content tag"));
        assertThat(errors).anyMatch(error -> error.contains("2 sentences"));
        assertThat(errors).anyMatch(error -> error.contains("child-safety"));
        assertThat(errors).anyMatch(error -> error.contains("difficulty out of stage range"));
    }
}
