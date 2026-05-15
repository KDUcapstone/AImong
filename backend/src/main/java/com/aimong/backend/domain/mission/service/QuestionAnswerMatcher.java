package com.aimong.backend.domain.mission.service;

import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class QuestionAnswerMatcher {

    private final ObjectMapper objectMapper;

    public QuestionAnswerMatcher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean matches(QuestionBank question, String answerPayload, String selected) {
        Set<String> expectedValues = expectedValues(question, answerPayload);
        String normalizedSelected = normalizeAnswerText(selected);
        return expectedValues.stream()
                .map(this::normalizeAnswerText)
                .anyMatch(normalizedSelected::equals);
    }

    public String displayAnswer(QuestionBank question, String answerPayload) {
        List<String> displayValues = displayValues(question, answerPayload);
        return displayValues.isEmpty() ? null : String.join(", ", displayValues);
    }

    private Set<String> expectedValues(QuestionBank question, String answerPayload) {
        JsonNode root = readAnswerPayload(answerPayload);
        Set<String> values = new LinkedHashSet<>();
        collectExpectedAnswerValues(root, options(question), values);
        return values;
    }

    private List<String> displayValues(QuestionBank question, String answerPayload) {
        JsonNode root = readAnswerPayload(answerPayload);
        List<String> options = options(question);
        List<String> values = new ArrayList<>();
        collectDisplayAnswerValues(root, options, values);
        return values;
    }

    private JsonNode readAnswerPayload(String answerPayload) {
        try {
            return objectMapper.readTree(answerPayload);
        } catch (JsonProcessingException exception) {
            throw new AimongException(ErrorCode.INTERNAL_SERVER_ERROR, exception);
        }
    }

    private List<String> options(QuestionBank question) {
        if (question.getOptionsJson() == null || question.getOptionsJson().isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(question.getOptionsJson(), new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new AimongException(ErrorCode.INTERNAL_SERVER_ERROR, exception);
        }
    }

    private void collectExpectedAnswerValues(JsonNode node, List<String> options, Set<String> values) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isTextual() || node.isNumber() || node.isBoolean()) {
            addAnswerValue(values, node.asText(), options);
            return;
        }
        if (node.isArray()) {
            List<String> combined = new ArrayList<>();
            node.forEach(child -> {
                String displayValue = displayValue(child.asText(), options);
                if (displayValue != null) {
                    combined.add(displayValue);
                }
                collectExpectedAnswerValues(child, options, values);
            });
            addCombinedValues(values, combined);
            return;
        }
        if (node.has("value")) {
            addAnswerValue(values, node.get("value").asText(), options);
        }
        if (node.has("values") && node.get("values").isArray()) {
            List<String> combined = new ArrayList<>();
            node.get("values").forEach(valueNode -> {
                String rawValue = valueNode.asText();
                String displayValue = displayValue(rawValue, options);
                addAnswerValue(values, rawValue, options);
                if (displayValue != null) {
                    combined.add(displayValue);
                }
            });
            addCombinedValues(values, combined);
        }
        if (node.has("index")) {
            addAnswerValue(values, node.get("index").asText(), options);
        }
    }

    private void collectDisplayAnswerValues(JsonNode node, List<String> options, List<String> values) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isTextual() || node.isNumber() || node.isBoolean()) {
            values.add(displayValue(node.asText(), options));
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> collectDisplayAnswerValues(child, options, values));
            return;
        }
        if (node.has("value")) {
            values.add(displayValue(node.get("value").asText(), options));
        }
        if (node.has("values") && node.get("values").isArray()) {
            node.get("values").forEach(valueNode -> values.add(displayValue(valueNode.asText(), options)));
        }
        if (node.has("index")) {
            values.add(displayValue(node.get("index").asText(), options));
        }
    }

    private void addAnswerValue(Set<String> values, String value, List<String> options) {
        values.add(value);
        String displayValue = displayValue(value, options);
        if (displayValue != null) {
            values.add(displayValue);
        }
        if ("true".equalsIgnoreCase(value)) {
            values.add("O");
            values.add("o");
            values.add("true");
        }
        if ("false".equalsIgnoreCase(value)) {
            values.add("X");
            values.add("x");
            values.add("false");
        }
    }

    private void addCombinedValues(Set<String> values, List<String> combined) {
        if (!combined.isEmpty()) {
            values.add(String.join(",", combined));
            values.add(String.join(", ", combined));
            values.add(String.join(" ", combined));
        }
    }

    private String displayValue(String value, List<String> options) {
        if (options.isEmpty()) {
            return value;
        }
        try {
            int oneBasedIndex = Integer.parseInt(value);
            int index = oneBasedIndex - 1;
            if (index >= 0 && index < options.size()) {
                return options.get(index);
            }
        } catch (NumberFormatException ignored) {
            return value;
        }
        return value;
    }

    private String normalizeAnswerText(String value) {
        return value == null ? "" : value.trim();
    }
}
