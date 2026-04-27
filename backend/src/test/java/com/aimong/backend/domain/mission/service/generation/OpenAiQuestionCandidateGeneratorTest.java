package com.aimong.backend.domain.mission.service.generation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.QuestionType;
import com.aimong.backend.infra.openai.OpenAiClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OpenAiQuestionCandidateGeneratorTest {

    @Mock
    private OpenAiClient openAiClient;

    @Test
    void generatesStructuredCandidatesFromOpenAiResponse() throws Exception {
        OpenAiQuestionCandidateGenerator generator = new OpenAiQuestionCandidateGenerator(
                openAiClient,
                new KerisCurriculumRegistry(),
                new ObjectMapper()
        );

        when(openAiClient.createStructuredResponse(eq("gpt-5-mini"), any(), any(), eq("aimong_question_candidates"), any()))
                .thenReturn(new ObjectMapper().readTree("""
                        {
                          "questions": [
                            {
                              "missionCode": "S0203",
                              "packNo": 1,
                              "difficultyBand": "LOW",
                              "type": "MULTIPLE",
                              "question": "AI에게 정보를 보낼 때 무엇을 먼저 확인해야 할까요?",
                              "options": ["보내는 사람", "배경 색", "글자 수", "파일 이름"],
                              "answer": 0,
                              "explanation": "정보를 보내기 전에는 보내는 사람을 먼저 확인해야 해요.",
                              "contentTags": ["PRIVACY", "SAFETY"],
                              "curriculumRef": "KERIS-REF",
                              "difficulty": 2
                            }
                          ]
                        }
                        """));

        List<StructuredQuestionSchema> candidates = generator.generate(
                new QuestionGenerationService.QuestionGenerationRequest(
                        "S0203",
                        1,
                        DifficultyBand.LOW,
                        QuestionType.MULTIPLE,
                        1,
                        2,
                        0,
                        false,
                        false,
                        false,
                        false,
                        List.of(),
                        List.of(),
                        List.of("Keep the explanation within two short sentences.")
                ),
                "gpt-5-mini"
        );

        assertThat(candidates).hasSize(1);
        assertThat(candidates.getFirst().missionCode()).isEqualTo("S0203");
        assertThat(candidates.getFirst().type()).isEqualTo(QuestionType.MULTIPLE);
    }
}
