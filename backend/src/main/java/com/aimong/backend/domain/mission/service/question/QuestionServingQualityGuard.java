package com.aimong.backend.domain.mission.service.question;

import com.aimong.backend.domain.mission.entity.Mission;
import com.aimong.backend.domain.mission.entity.QuestionAnswerKey;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.repository.QuestionAnswerKeyRepository;
import com.aimong.backend.domain.mission.service.generation.QuestionValidationReport;
import com.aimong.backend.domain.mission.service.generation.QuestionValidationService;
import com.aimong.backend.domain.mission.service.generation.StructuredQuestionSchema;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QuestionServingQualityGuard {

    private final QuestionAnswerKeyRepository questionAnswerKeyRepository;
    private final QuestionValidationService questionValidationService;
    private final QuestionQualityReviewService questionQualityReviewService;
    private final ObjectMapper objectMapper;

    public ServingValidationResult validateForServing(Mission mission, List<QuestionBank> selectedQuestions) {
        if (selectedQuestions.isEmpty()) {
            return new ServingValidationResult(List.of(), List.of());
        }

        Map<UUID, QuestionAnswerKey> answerKeys = questionAnswerKeyRepository.findAllByQuestionIdIn(
                        selectedQuestions.stream().map(QuestionBank::getId).toList()
                ).stream()
                .collect(Collectors.toMap(QuestionAnswerKey::getQuestionId, Function.identity()));

        List<QuestionBank> validQuestions = new ArrayList<>();
        List<UUID> invalidQuestionIds = new ArrayList<>();

        for (QuestionBank question : selectedQuestions) {
            QuestionAnswerKey answerKey = answerKeys.get(question.getId());
            if (answerKey == null) {
                questionQualityReviewService.recordServingFailure(
                        mission,
                        question,
                        "MISSING_ANSWER_KEY",
                        "Serving-time revalidation failed because the answer key was missing.",
                        null
                );
                invalidQuestionIds.add(question.getId());
                continue;
            }

            try {
                StructuredQuestionSchema schema = reconstruct(mission, question, answerKey);
                QuestionValidationReport report = questionValidationService.validate(
                        new QuestionValidationService.ValidationRequest(
                                schema,
                                List.of(),
                                List.of()
                        )
                );
                if (report.pass()) {
                    validQuestions.add(question);
                    continue;
                }

                questionQualityReviewService.recordServingFailure(
                        mission,
                        question,
                        "SERVING_VALIDATION_REJECTED",
                        "Serving-time revalidation rejected the persisted question.",
                        report
                );
                invalidQuestionIds.add(question.getId());
            } catch (IllegalStateException exception) {
                questionQualityReviewService.recordServingFailure(
                        mission,
                        question,
                        "SERVING_RECONSTRUCTION_FAILED",
                        exception.getMessage(),
                        null
                );
                invalidQuestionIds.add(question.getId());
            }
        }

        return new ServingValidationResult(List.copyOf(validQuestions), List.copyOf(invalidQuestionIds));
    }

    private StructuredQuestionSchema reconstruct(
            Mission mission,
            QuestionBank question,
            QuestionAnswerKey answerKey
    ) {
        return new StructuredQuestionSchema(
                mission.getMissionCode(),
                question.getPackNo() == null ? 0 : question.getPackNo(),
                question.getDifficulty(),
                question.getQuestionType(),
                question.getPrompt(),
                readOptions(question.getOptionsJson()),
                readAnswer(question, answerKey.getAnswerPayload()),
                answerKey.getExplanation(),
                readStringList(question.getContentTagsJson()),
                question.getCurriculumRef(),
                resolveNumericDifficulty(mission, question),
                question.getDifficulty()
        );
    }

    private int resolveNumericDifficulty(Mission mission, QuestionBank question) {
        if (question.getLegacyNumericDifficulty() != null && question.getLegacyNumericDifficulty() > 0) {
            return question.getLegacyNumericDifficulty();
        }
        return switch (mission.getStage()) {
            case 1 -> question.getDifficulty() == DifficultyBand.LOW ? 1 : 2;
            case 2 -> question.getDifficulty() == DifficultyBand.LOW ? 2 : 3;
            case 3 -> question.getDifficulty() == DifficultyBand.LOW ? 3 : 4;
            default -> 1;
        };
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize persisted question list payload", exception);
        }
    }

    private List<String> readOptions(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return readStringList(json);
    }

    private Object readAnswer(QuestionBank question, String json) {
        try {
            Object answer = objectMapper.readValue(json, Object.class);
            List<String> options = readOptions(question.getOptionsJson());
            return normalizePersistedAnswer(question, options, answer);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize persisted answer payload", exception);
        }
    }

    private Object normalizePersistedAnswer(QuestionBank question, List<String> options, Object answer) {
        return switch (question.getQuestionType()) {
            case OX -> answer;
            case MULTIPLE, SITUATION -> normalizeSingleChoiceAnswer(options, answer);
            case FILL -> normalizeFillAnswer(options, answer);
        };
    }

    private Object normalizeSingleChoiceAnswer(List<String> options, Object answer) {
        if (answer instanceof Integer) {
            return answer;
        }
        if (answer instanceof String text) {
            Integer index = optionIndex(options, text);
            return index == null ? answer : index;
        }
        return answer;
    }

    private Object normalizeFillAnswer(List<String> options, Object answer) {
        if (answer instanceof List<?> answers) {
            List<Integer> indexes = answers.stream()
                    .map(value -> value instanceof Integer index ? index : optionIndex(options, String.valueOf(value)))
                    .filter(java.util.Objects::nonNull)
                    .toList();
            return indexes.isEmpty() ? answer : indexes;
        }
        if (answer instanceof String text) {
            Integer index = optionIndex(options, text);
            return index == null ? answer : List.of(index);
        }
        return answer;
    }

    private Integer optionIndex(List<String> options, String answerText) {
        if (options == null || answerText == null) {
            return null;
        }
        String normalizedAnswer = normalizeAnswerText(answerText);
        for (int index = 0; index < options.size(); index++) {
            if (normalizeAnswerText(options.get(index)).equals(normalizedAnswer)) {
                return index;
            }
        }
        return null;
    }

    private String normalizeAnswerText(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").trim();
    }

    public record ServingValidationResult(
            List<QuestionBank> validQuestions,
            List<UUID> invalidQuestionIds
    ) {
    }
}
