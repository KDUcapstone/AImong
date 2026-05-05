package com.aimong.backend.domain.mission.service;

import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.mission.dto.QuestionCheckRequest;
import com.aimong.backend.domain.mission.dto.QuestionCheckResponse;
import com.aimong.backend.domain.mission.entity.QuestionAnswerKey;
import com.aimong.backend.domain.mission.entity.QuizAttempt;
import com.aimong.backend.domain.mission.repository.QuestionAnswerKeyRepository;
import com.aimong.backend.domain.mission.repository.QuizAttemptRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuestionCheckService {

    private final QuizAttemptRepository quizAttemptRepository;
    private final QuestionAnswerKeyRepository questionAnswerKeyRepository;
    private final ChildActivityService childActivityService;
    private final QuizService quizService;
    private final ObjectMapper objectMapper;

    @Transactional
    public QuestionCheckResponse check(
            UUID childId,
            UUID missionId,
            UUID questionId,
            QuestionCheckRequest request
    ) {
        childActivityService.touchLastActiveAt(childId);
        QuizAttempt quizAttempt = quizAttemptRepository.findById(request.quizAttemptId())
                .orElseThrow(() -> new AimongException(ErrorCode.QUIZ_ATTEMPT_INVALID));

        if (!quizAttempt.getChildId().equals(childId) || !quizAttempt.getMissionId().equals(missionId)) {
            throw new AimongException(ErrorCode.FORBIDDEN);
        }
        if (quizAttempt.getSubmittedAt() != null) {
            throw new AimongException(ErrorCode.QUIZ_ATTEMPT_ALREADY_SUBMITTED);
        }
        if (!quizAttempt.getExpiresAt().isAfter(Instant.now())) {
            throw new AimongException(ErrorCode.ATTEMPT_EXPIRED);
        }
        if (!quizService.parseQuestionIds(quizAttempt.getQuestionIdsJson()).contains(questionId)) {
            throw new AimongException(ErrorCode.QUESTION_NOT_FOUND);
        }

        QuestionAnswerKey answerKey = questionAnswerKeyRepository.findById(questionId)
                .orElseThrow(() -> new AimongException(ErrorCode.QUESTION_NOT_FOUND));
        return new QuestionCheckResponse(
                questionId,
                matchesAnswerPayload(answerKey.getAnswerPayload(), request.selected()),
                answerKey.getExplanation()
        );
    }

    private boolean matchesAnswerPayload(String answerPayload, String selected) {
        Set<String> expectedValues = parseExpectedAnswerValues(answerPayload);
        String normalizedSelected = normalizeAnswerText(selected);
        return expectedValues.stream()
                .map(this::normalizeAnswerText)
                .anyMatch(normalizedSelected::equals);
    }

    private Set<String> parseExpectedAnswerValues(String answerPayload) {
        try {
            JsonNode root = objectMapper.readTree(answerPayload);
            Set<String> values = new HashSet<>();
            collectExpectedAnswerValues(root, values);
            return values;
        } catch (JsonProcessingException exception) {
            throw new AimongException(ErrorCode.INTERNAL_SERVER_ERROR, exception);
        }
    }

    private void collectExpectedAnswerValues(JsonNode node, Set<String> values) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isTextual() || node.isNumber() || node.isBoolean()) {
            addAnswerValue(values, node.asText());
            return;
        }
        if (node.isArray()) {
            node.forEach(child -> collectExpectedAnswerValues(child, values));
            return;
        }
        if (node.has("value")) {
            addAnswerValue(values, node.get("value").asText());
        }
        if (node.has("values") && node.get("values").isArray()) {
            java.util.List<String> fillValues = new ArrayList<>();
            node.get("values").forEach(valueNode -> {
                String value = valueNode.asText();
                addAnswerValue(values, value);
                fillValues.add(value);
            });
            if (!fillValues.isEmpty()) {
                addAnswerValue(values, String.join(",", fillValues));
                addAnswerValue(values, String.join(" ", fillValues));
            }
        }
        if (node.has("index")) {
            addAnswerValue(values, node.get("index").asText());
        }
    }

    private void addAnswerValue(Set<String> values, String value) {
        values.add(value);
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

    private String normalizeAnswerText(String value) {
        return value == null ? "" : value.trim();
    }
}
