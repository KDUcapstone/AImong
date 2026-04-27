package com.aimong.backend.tools.questionbank;

import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.QuestionType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class QuestionBankAuditLoader {

    private final ObjectMapper objectMapper;

    public QuestionBankAuditLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AuditQuestionBank load(Path path) throws IOException {
        JsonNode root = objectMapper.readTree(Files.readString(path));
        List<AuditQuestion> questions = new ArrayList<>();
        for (JsonNode questionNode : root.withArray("questions")) {
            questions.add(new AuditQuestion(
                    text(questionNode, "externalId"),
                    text(questionNode, "missionCode"),
                    (short) questionNode.path("stage").asInt(0),
                    text(questionNode, "stageTitle"),
                    text(questionNode, "missionTitle"),
                    parseType(text(questionNode, "type")),
                    text(questionNode, "question"),
                    parseOptions(questionNode.get("options")),
                    objectMapper.treeToValue(questionNode.get("answer"), Object.class),
                    text(questionNode, "explanation"),
                    parseOptions(questionNode.get("contentTags")),
                    text(questionNode, "curriculumRef"),
                    questionNode.path("difficulty").asInt(0),
                    parseBand(text(questionNode, "difficultyBand")),
                    parsePackNo(questionNode),
                    text(questionNode, "sourceType")
            ));
        }
        return new AuditQuestionBank(
                text(root, "sourceTitle"),
                text(root, "sourceReference"),
                text(root, "generationVersion"),
                root.path("totalMissionCount").asInt(0),
                root.path("totalQuestionCount").asInt(questions.size()),
                List.copyOf(questions)
        );
    }

    private QuestionType parseType(String value) {
        return value == null || value.isBlank() ? null : QuestionType.valueOf(value);
    }

    private DifficultyBand parseBand(String value) {
        return value == null || value.isBlank() ? null : DifficultyBand.valueOf(value);
    }

    private Integer parsePackNo(JsonNode node) {
        if (node.hasNonNull("packNo")) {
            return node.path("packNo").asInt();
        }
        String externalId = text(node, "externalId");
        if (externalId != null && externalId.contains("-P")) {
            int marker = externalId.indexOf("-P");
            int dash = externalId.indexOf('-', marker + 2);
            if (marker >= 0 && dash > marker) {
                return Integer.parseInt(externalId.substring(marker + 2, dash));
            }
        }
        return null;
    }

    private List<String> parseOptions(JsonNode node) throws IOException {
        if (node == null || node.isNull()) {
            return null;
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            values.add(item.asText());
        }
        return List.copyOf(values);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }
}
