package com.aimong.backend.domain.mission.service;

import com.aimong.backend.domain.mission.dto.MissionQuestionsResponse;
import com.aimong.backend.domain.mission.dto.QuestionResponse;
import com.aimong.backend.domain.mission.dto.StageProgressResponse;
import com.aimong.backend.domain.mission.config.MissionQuestionProperties;
import com.aimong.backend.domain.mission.entity.Mission;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.entity.QuizAttempt;
import com.aimong.backend.domain.mission.repository.MissionAttemptRepository;
import com.aimong.backend.domain.mission.repository.MissionRepository;
import com.aimong.backend.domain.mission.repository.QuizAttemptRepository;
import com.aimong.backend.domain.mission.service.question.MissionQuestionSetFactory;
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
    private final MissionAttemptRepository missionAttemptRepository;
    private final MissionService missionService;
    private final MissionQuestionSetFactory missionQuestionSetFactory;
    private final MissionQuestionProperties missionQuestionProperties;
    private final ObjectMapper objectMapper;

    @Transactional
    public MissionQuestionsResponse getQuestions(UUID childId, UUID missionId) {
        Mission mission = missionRepository.findById(missionId)
                .filter(Mission::isActive)
                .orElseThrow(() -> new AimongException(ErrorCode.MISSION_NOT_FOUND));

        StageProgressResponse stageProgress = missionService.getMissions(childId).stageProgress();
        if (!missionService.isUnlocked(mission, stageProgress)) {
            throw new AimongException(ErrorCode.MISSION_LOCKED);
        }

        boolean isReview = missionAttemptRepository.existsByChildIdAndMissionIdAndAttemptDate(
                childId,
                missionId,
                KstDateUtils.today()
        );

        List<QuestionBank> selectedQuestions = missionQuestionSetFactory.create(missionId, childId, isReview);
        List<UUID> selectedQuestionIds = selectedQuestions.stream()
                .map(QuestionBank::getId)
                .toList();

        QuizAttempt quizAttempt = QuizAttempt.create(
                childId,
                missionId,
                writeQuestionIds(selectedQuestionIds),
                Instant.now().plus(missionQuestionProperties.attemptTtlMinutes(), ChronoUnit.MINUTES)
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
