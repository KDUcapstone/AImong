package com.aimong.backend.domain.mission.service.question;

import com.aimong.backend.domain.mission.config.MissionQuestionProperties;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.entity.QuestionPoolStatus;
import com.aimong.backend.domain.mission.entity.QuizAttempt;
import com.aimong.backend.domain.mission.repository.QuestionBankRepository;
import com.aimong.backend.domain.mission.repository.QuizAttemptRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StaticQuestionProvider implements ApprovedQuestionProvider {

    private final QuestionBankRepository questionBankRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final MissionQuestionProperties missionQuestionProperties;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<List<QuestionBank>> findIntactUnusedPack(UUID missionId, UUID childId, boolean isReview) {
        Set<UUID> solvedQuestionIds = isReview ? Set.of() : findSolvedQuestionIds(childId, missionId);
        List<Short> intactPackNumbers = questionBankRepository.findIntactPackNumbers(
                missionId,
                missionQuestionProperties.setSize()
        );

        for (Short packNo : intactPackNumbers) {
            List<QuestionBank> packQuestions = questionBankRepository.findAllByMissionIdAndIsActiveTrueAndPackNoOrderByCreatedAtAsc(
                    missionId,
                    packNo
            );
            if (packQuestions.size() != missionQuestionProperties.setSize()) {
                continue;
            }
            if (!isReview && packQuestions.stream().anyMatch(question -> solvedQuestionIds.contains(question.getId()))) {
                continue;
            }
            return Optional.of(packQuestions);
        }
        return Optional.empty();
    }

    @Override
    public ApprovedQuestionPool findApprovedQuestionPool(UUID missionId, UUID childId, boolean isReview) {
        Set<UUID> solvedQuestionIds = isReview ? Set.of() : findSolvedQuestionIds(childId, missionId);
        List<QuestionBank> questions = new ArrayList<>(
                questionBankRepository.findAllByMissionIdAndIsActiveTrueAndQuestionPoolStatus(missionId, QuestionPoolStatus.ACTIVE)
        );
        if (questions.isEmpty()) {
            questions = new ArrayList<>(questionBankRepository.findAllByMissionIdAndIsActiveTrue(missionId));
        }
        int excludedBySolved = 0;
        if (!isReview) {
            int originalSize = questions.size();
            questions.removeIf(question -> solvedQuestionIds.contains(question.getId()));
            excludedBySolved = originalSize - questions.size();
        }
        return new ApprovedQuestionPool(List.copyOf(questions), excludedBySolved);
    }

    private Set<UUID> findSolvedQuestionIds(UUID childId, UUID missionId) {
        Set<UUID> solvedQuestionIds = new LinkedHashSet<>();
        for (QuizAttempt attempt : quizAttemptRepository.findAllByChildIdAndMissionIdAndSubmittedAtIsNotNull(childId, missionId)) {
            solvedQuestionIds.addAll(readQuestionIds(attempt.getQuestionIdsJson()));
        }
        return solvedQuestionIds;
    }

    private List<UUID> readQuestionIds(String questionIdsJson) {
        try {
            return objectMapper.readValue(questionIdsJson, new TypeReference<>() {
            });
        } catch (Exception exception) {
            return List.of();
        }
    }
}
