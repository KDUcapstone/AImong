package com.aimong.backend.domain.mission.service.question;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.aimong.backend.domain.mission.config.MissionQuestionProperties;
import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.GenerationPhase;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.entity.QuestionPoolStatus;
import com.aimong.backend.domain.mission.entity.QuestionType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MissionQuestionSeedSelectionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void generatedSeedCanServeEveryMissionAndStarLevelWithCurrentSelectionRules() throws Exception {
        SeedPools seedPools = readSeedPools();
        MissionQuestionSetFactory factory = new MissionQuestionSetFactory(
                seedPools::findByMissionAndDifficulty,
                new MissionQuestionProperties(10, 30, false)
        );

        for (UUID missionId : seedPools.missionIds()) {
            for (int starLevel = 1; starLevel <= 3; starLevel++) {
                int currentStarLevel = starLevel;
                assertThatCode(() -> factory.create(missionId, UUID.randomUUID(), currentStarLevel, false))
                        .doesNotThrowAnyException();
            }
        }
    }

    private SeedPools readSeedPools() throws Exception {
        JsonNode root = objectMapper.readTree(Files.readString(
                Path.of("_generated/question-bank/question-bank-1056-starlevel-edits.json"),
                StandardCharsets.UTF_8
        ));
        Map<String, UUID> missionIdsByCode = new HashMap<>();
        Map<PoolKey, List<QuestionBank>> questionsByPool = new HashMap<>();
        for (JsonNode questionNode : root.path("questions")) {
            String missionCode = questionNode.path("missionCode").asText();
            UUID missionId = missionIdsByCode.computeIfAbsent(
                    missionCode,
                    code -> UUID.nameUUIDFromBytes(code.getBytes(StandardCharsets.UTF_8))
            );
            DifficultyBand difficulty = DifficultyBand.valueOf(questionNode.path("difficulty").asText());
            QuestionBank question = QuestionBank.create(
                    missionId,
                    QuestionType.valueOf(questionNode.path("type").asText()),
                    questionNode.path("question").asText(),
                    questionNode.path("options").isMissingNode() || questionNode.path("options").isNull()
                            ? null
                            : objectMapper.writeValueAsString(questionNode.path("options")),
                    objectMapper.writeValueAsString(questionNode.path("contentTags")),
                    questionNode.path("curriculumRef").asText("seed"),
                    difficulty,
                    questionNode.path("sourceType").asText("STATIC"),
                    GenerationPhase.PREGENERATED,
                    questionNode.hasNonNull("packNo") ? (short) questionNode.path("packNo").asInt() : null,
                    QuestionPoolStatus.ACTIVE
            );
            questionsByPool.computeIfAbsent(new PoolKey(missionId, difficulty), ignored -> new ArrayList<>())
                    .add(question);
        }
        return new SeedPools(missionIdsByCode, questionsByPool);
    }

    private record SeedPools(
            Map<String, UUID> missionIdsByCode,
            Map<PoolKey, List<QuestionBank>> questionsByPool
    ) {
        List<UUID> missionIds() {
            return List.copyOf(missionIdsByCode.values());
        }

        List<QuestionBank> findByMissionAndDifficulty(UUID missionId, DifficultyBand difficulty) {
            return questionsByPool.getOrDefault(new PoolKey(missionId, difficulty), List.of());
        }
    }

    private record PoolKey(UUID missionId, DifficultyBand difficulty) {
    }
}
