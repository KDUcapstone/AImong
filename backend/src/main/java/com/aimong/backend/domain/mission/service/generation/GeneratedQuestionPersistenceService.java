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

            QuestionBank questionBank = QuestionBank.create(
                    missionId,
                    candidate.type(),
                    candidate.question(),
                    writeJson(candidate.options()),
                    writeJson(candidate.contentTags()),
                    candidate.curriculumRef(),
                    (short) candidate.difficulty(),
                    sourceType,
                    generationPhase,
                    (short) candidate.packNo(),
                    candidate.difficultyBand(),
                    QuestionPoolStatus.ACTIVE
            );
            questionBankRepository.save(questionBank);
            questionAnswerKeyRepository.save(QuestionAnswerKey.create(
                    questionBank.getId(),
                    writeJson(candidate.answer()),
                    candidate.explanation()
            ));
            saved.add(questionBank);
        }
        return saved;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize generated question payload", exception);
        }
    }
}
