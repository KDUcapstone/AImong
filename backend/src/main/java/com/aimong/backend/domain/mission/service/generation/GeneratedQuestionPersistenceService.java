package com.aimong.backend.domain.mission.service.generation;

import com.aimong.backend.domain.mission.entity.GenerationPhase;
import com.aimong.backend.domain.mission.entity.QuestionAnswerKey;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.entity.QuestionPoolStatus;
import com.aimong.backend.domain.mission.repository.QuestionAnswerKeyRepository;
import com.aimong.backend.domain.mission.repository.QuestionBankRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GeneratedQuestionPersistenceService {

    private final QuestionBankRepository questionBankRepository;
    private final QuestionAnswerKeyRepository questionAnswerKeyRepository;
    private final QuestionValidationService questionValidationService;
    private final ObjectMapper objectMapper;

    @Transactional
    public List<QuestionBank> persistCandidates(
            UUID missionId,
            List<StructuredQuestionSchema> candidates,
            GenerationPhase generationPhase,
            String sourceType
    ) {
        List<QuestionBank> saved = new ArrayList<>();
        List<String> existingMissionPrompts = questionBankRepository.findAllByMissionIdAndIsActiveTrue(missionId).stream()
                .map(QuestionBank::getPrompt)
                .toList();

        for (StructuredQuestionSchema candidate : candidates) {
            QuestionValidationReport report = questionValidationService.validate(
                    new QuestionValidationService.ValidationRequest(
                            candidate,
                            existingMissionPrompts,
                            List.of()
                    )
            );
            if (!report.pass()) {
                continue;
            }
            StructuredQuestionSchema normalizedCandidate = questionValidationService.normalizeCandidate(candidate);

            QuestionBank questionBank = QuestionBank.create(
                    missionId,
                    normalizedCandidate.type(),
                    normalizedCandidate.question(),
                    writeJson(normalizedCandidate.options()),
                    writeJson(normalizedCandidate.contentTags()),
                    normalizedCandidate.curriculumRef(),
                    normalizedCandidate.effectiveDifficulty(),
                    sourceType,
                    generationPhase,
                    normalizedCandidate.packNo() <= 0 ? null : (short) normalizedCandidate.packNo(),
                    QuestionPoolStatus.ACTIVE
            );
            questionBankRepository.save(questionBank);
            questionAnswerKeyRepository.save(QuestionAnswerKey.create(
                    questionBank.getId(),
                    writeJson(toExternalAnswer(normalizedCandidate)),
                    normalizedCandidate.explanation()
            ));
            saved.add(questionBank);
        }
        return saved;
    }

    private Object toExternalAnswer(StructuredQuestionSchema candidate) {
        return switch (candidate.type()) {
            case MULTIPLE, SITUATION -> candidate.answer() instanceof Integer index
                    ? index + 1
                    : candidate.answer();
            case FILL -> candidate.answer() instanceof List<?> answers
                    ? answers.stream()
                            .map(value -> value instanceof Integer index ? index + 1 : value)
                            .toList()
                    : candidate.answer();
            case OX -> candidate.answer();
        };
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize generated question payload", exception);
        }
    }
}
