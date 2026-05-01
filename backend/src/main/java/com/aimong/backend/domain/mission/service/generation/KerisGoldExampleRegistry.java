package com.aimong.backend.domain.mission.service.generation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class KerisGoldExampleRegistry {

    private static final Path GOLD_EXAMPLES_PATH = Path.of("docs", "keris", "04_gold_examples.json");

    private final ObjectMapper objectMapper;

    public KerisGoldExampleRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<String> findQuestionPrompts(String missionCode) {
        if (missionCode == null || missionCode.isBlank() || !Files.exists(GOLD_EXAMPLES_PATH)) {
            return List.of();
        }

        try (InputStream inputStream = Files.newInputStream(GOLD_EXAMPLES_PATH)) {
            JsonNode root = objectMapper.readTree(inputStream);
            List<String> prompts = new ArrayList<>();
            for (JsonNode mission : root.path("missions")) {
                if (missionCode.equals(mission.path("missionCode").asText())) {
                    for (JsonNode example : mission.path("examples")) {
                        String question = example.path("question").asText("");
                        if (!question.isBlank()) {
                            prompts.add(question);
                        }
                    }
                }
            }
            return prompts;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read gold example registry", exception);
        }
    }
}
