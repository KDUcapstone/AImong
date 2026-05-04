package com.aimong.backend.domain.mission.service;

import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.mission.dto.MissionQuestionsResponse;
import com.aimong.backend.domain.mission.dto.QuestionResponse;
import com.aimong.backend.domain.mission.dto.StageProgressResponse;
import com.aimong.backend.domain.mission.config.MissionQuestionProperties;
import com.aimong.backend.domain.mission.entity.Mission;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.entity.QuizAttempt;
import com.aimong.backend.domain.mission.repository.MissionDailyProgressRepository;
import com.aimong.backend.domain.mission.repository.MissionRepository;
import com.aimong.backend.domain.mission.repository.QuizAttemptRepository;
import com.aimong.backend.domain.mission.service.question.MissionQuestionSetFactory;
import com.aimong.backend.domain.mission.service.question.QuestionServingQualityGuard;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.aimong.backend.global.util.KstDateUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final MissionRepository missionRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final MissionDailyProgressRepository missionDailyProgressRepository;
    private final ChildActivityService childActivityService;
    private final MissionService missionService;
    private final MissionQuestionSetFactory missionQuestionSetFactory;
    private final QuestionServingQualityGuard questionServingQualityGuard;
    private final MissionQuestionProperties missionQuestionProperties;
    private final ObjectMapper objectMapper;

    @Transactional
    public MissionQuestionsResponse getQuestions(UUID childId, UUID missionId) {
        childActivityService.touchLastActiveAt(childId);
        Mission mission = missionRepository.findById(missionId)
                .filter(Mission::isActive)
                .orElseThrow(() -> new AimongException(ErrorCode.MISSION_NOT_FOUND));

        StageProgressResponse stageProgress = missionService.getMissions(childId).stageProgress();
        if (!missionService.isUnlockedForChild(childId, mission, stageProgress)) {
            throw new AimongException(ErrorCode.MISSION_QUESTIONS_LOCKED);
        }

        boolean isReview = missionDailyProgressRepository.findByChildIdAndMissionIdAndProgressDate(
                childId,
                missionId,
                KstDateUtils.today()
        ).isPresent();

        List<QuestionBank> selectedQuestions = createServingReadyQuestionSet(mission, childId, isReview);
        if (selectedQuestions.size() != missionQuestionProperties.setSize()) {
            throw new AimongException(ErrorCode.MISSION_SET_NOT_READY);
        }
        List<UUID> selectedQuestionIds = selectedQuestions.stream()
                .map(QuestionBank::getId)
                .toList();

        QuizAttempt quizAttempt = QuizAttempt.create(
                childId,
                missionId,
                writeQuestionIds(selectedQuestionIds),
                Instant.now().plus(missionQuestionProperties.attemptTtlMinutes(), ChronoUnit.MINUTES),
                isReview
        );
        quizAttemptRepository.save(quizAttempt);

        return new MissionQuestionsResponse(
                mission.getId(),
                mission.getTitle(),
                isReview,
                quizAttempt.getId(),
                missionQuestionProperties.setSize(),
                quizAttempt.getExpiresAt(),
                selectedQuestions.stream().map(this::toQuestionResponse).toList()
        );
    }

    private List<QuestionBank> createServingReadyQuestionSet(Mission mission, UUID childId, boolean isReview) {
        if (!missionQuestionProperties.servingAutoQuarantineEnabled()) {
            return missionQuestionSetFactory.create(mission.getId(), childId, isReview);
        }

        for (int attempt = 0; attempt < 2; attempt++) {
            List<QuestionBank> selectedQuestions = missionQuestionSetFactory.create(mission.getId(), childId, isReview);
            QuestionServingQualityGuard.ServingValidationResult validationResult =
                    questionServingQualityGuard.validateForServing(mission, selectedQuestions);
            if (validationResult.validQuestions().size() == missionQuestionProperties.setSize()) {
                return validationResult.validQuestions();
            }
        }
        throw new AimongException(ErrorCode.MISSION_SET_NOT_READY);
    }

    public List<UUID> parseQuestionIds(String questionIdsJson) {
        try {
            return objectMapper.readValue(questionIdsJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new AimongException(ErrorCode.INTERNAL_SERVER_ERROR, exception);
        }
    }

    private String writeQuestionIds(List<UUID> questionIds) {
        try {
            return objectMapper.writeValueAsString(questionIds);
        } catch (JsonProcessingException exception) {
            throw new AimongException(ErrorCode.INTERNAL_SERVER_ERROR, exception);
        }
    }

    private QuestionResponse toQuestionResponse(QuestionBank question) {
        return new QuestionResponse(
                question.getId(),
                question.getQuestionType().name(),
                question.getPrompt(),
                readOptions(question.getOptionsJson())
        );
    }

    private List<String> readOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(optionsJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new AimongException(ErrorCode.INTERNAL_SERVER_ERROR, exception);
        }
    }
}
