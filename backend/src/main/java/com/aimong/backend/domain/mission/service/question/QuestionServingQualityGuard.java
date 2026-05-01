package com.aimong.backend.domain.mission.service.question;

import com.aimong.backend.domain.mission.entity.Mission;
import com.aimong.backend.domain.mission.entity.QuestionAnswerKey;
import com.aimong.backend.domain.mission.entity.QuestionBank;
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
                readAnswer(answerKey.getAnswerPayload()),
                answerKey.getExplanation(),
                readStringList(question.getContentTagsJson()),
                question.getCurriculumRef(),
                question.getLegacyNumericDifficulty() == null ? 0 : question.getLegacyNumericDifficulty(),
                question.getDifficulty()
        );
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

    private Object readAnswer(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize persisted answer payload", exception);
        }
    }

    public record ServingValidationResult(
            List<QuestionBank> validQuestions,
            List<UUID> invalidQuestionIds
    ) {
    }
}
