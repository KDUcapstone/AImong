package com.aimong.backend.domain.mission.service.generation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class QuestionPromptSanitizerTest {

    @Test
    void removesLeadingActivityIntroSentences() {
        assertThat(QuestionPromptSanitizer.sanitizeQuestion(
                "수업 활동 속 예를 떠올리며 골라 보세요. 다음 중 개인정보를 넣지 않아도 되는 것은 무엇일까요?"
        )).isEqualTo("다음 중 개인정보를 넣지 않아도 되는 것은 무엇일까요?");

        assertThat(QuestionPromptSanitizer.sanitizeQuestion(
                "활동 장면을 떠올리며 빈칸을 채워 보세요. AI에게 물어보기 전에는 ___을 확인해야 합니다."
        )).isEqualTo("AI에게 물어보기 전에는 ___을 확인해야 합니다.");

        assertThat(QuestionPromptSanitizer.sanitizeQuestion(
                "모둠 토의 중이라고 생각하며 판단해 보세요. 친구의 주소를 입력해도 안전하다."
        )).isEqualTo("친구의 주소를 입력해도 안전하다.");

        assertThat(QuestionPromptSanitizer.sanitizeQuestion(
                "활동 장면을 떠올리며 다음 중 AI 답을 다시 확인해야 하는 까닭은 무엇일까요?"
        )).isEqualTo("다음 중 AI 답을 다시 확인해야 하는 까닭은 무엇일까요?");
    }

    @Test
    void keepsRealScenarioQuestion() {
        assertThat(QuestionPromptSanitizer.sanitizeQuestion(
                "모둠 토의에서 친구가 AI 답을 그대로 발표하자고 말했습니다. 가장 좋은 행동은 무엇일까요?"
        )).isEqualTo("모둠 토의에서 친구가 AI 답을 그대로 발표하자고 말했습니다. 가장 좋은 행동은 무엇일까요?");
    }

    @Test
    void handlesBlankQuestion() {
        assertThat(QuestionPromptSanitizer.sanitizeQuestion(null)).isEmpty();
        assertThat(QuestionPromptSanitizer.sanitizeQuestion("   ")).isEmpty();
    }
}
