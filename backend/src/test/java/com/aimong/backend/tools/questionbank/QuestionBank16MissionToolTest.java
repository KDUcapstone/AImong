package com.aimong.backend.tools.questionbank;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class QuestionBank16MissionToolTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void generatorCreatesOneHundredSixtyValidatedQuestionsForSixteenMissions() throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream("/question-bank/keris-elementary-ai-16-mission-curriculum.json")) {
            CurriculumManifest manifest = objectMapper.readValue(inputStream, CurriculumManifest.class);
            QuestionBankDraft draft = new QuestionBankGenerator().generate(manifest);

            assertThat(draft.totalQuestionCount()).isEqualTo(160);
            assertThat(draft.questions().stream().map(QuestionDraft::missionCode).distinct()).hasSize(16);
            assertThat(new QuestionBankValidator().validate(draft)).isEmpty();

            Map<Short, Long> missionCountsByStage = manifest.units().stream()
                    .flatMap(unit -> unit.lessons().stream().map(lesson -> Map.entry(unit.stage(), lesson.lessonCode())))
                    .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.counting()));

            assertThat(missionCountsByStage).containsEntry((short) 1, 5L);
            assertThat(missionCountsByStage).containsEntry((short) 2, 6L);
            assertThat(missionCountsByStage).containsEntry((short) 3, 5L);
        }
    }
}
