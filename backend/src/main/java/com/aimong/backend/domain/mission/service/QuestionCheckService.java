package com.aimong.backend.domain.mission.service;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.mission.dto.MissionSetCheckRequest;
import com.aimong.backend.domain.mission.dto.QuestionCheckResponse;
import com.aimong.backend.domain.mission.entity.QuestionAnswerKey;
import com.aimong.backend.domain.mission.entity.QuizAttempt;
import com.aimong.backend.domain.mission.entity.QuizAttemptStatus;
import com.aimong.backend.domain.mission.repository.QuestionAnswerKeyRepository;
import com.aimong.backend.domain.mission.repository.QuestionBankRepository;
import com.aimong.backend.domain.mission.repository.QuizAttemptRepository;
import com.aimong.backend.domain.reward.service.CurrencyService;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuestionCheckService {

    private final QuestionAnswerKeyRepository questionAnswerKeyRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final ChildProfileRepository childProfileRepository;
    private final ChildActivityService childActivityService;
    private final QuestionAnswerMatcher questionAnswerMatcher;
    private final ObjectMapper objectMapper;

    @Transactional
    public QuestionCheckResponse check(
            UUID childId,
            String setId,
            MissionSetCheckRequest request
    ) {
        childActivityService.touchLastActiveAt(childId);
        UUID questionId = parseQuestionId(request.questionId());
        QuizAttempt quizAttempt = quizAttemptRepository
                .findFirstByChildIdAndSetIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                        childId,
                        setId,
                        QuizAttemptStatus.IN_PROGRESS,
                        Instant.now()
                )
                .orElseThrow(() -> new AimongException(ErrorCode.QUIZ_ATTEMPT_INVALID));
        return checkAgainstAttempt(quizAttempt, questionId, request.answer());
    }

    @Transactional
    public QuestionCheckResponse check(
            UUID childId,
            UUID missionId,
            UUID questionId,
            String answer
    ) {
        childActivityService.touchLastActiveAt(childId);
        QuizAttempt quizAttempt = quizAttemptRepository
                .findFirstByChildIdAndMissionIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                        childId,
                        missionId,
                        QuizAttemptStatus.IN_PROGRESS,
                        Instant.now()
                )
                .orElseThrow(() -> new AimongException(ErrorCode.QUIZ_ATTEMPT_INVALID));
        return checkAgainstAttempt(quizAttempt, questionId, answer);
    }

    private QuestionCheckResponse checkAgainstAttempt(QuizAttempt quizAttempt, UUID questionId, String answer) {
        if (!parseQuestionIds(quizAttempt.getQuestionIdsJson()).contains(questionId)) {
            throw new AimongException(ErrorCode.QUESTION_NOT_FOUND);
        }

        var question = questionBankRepository.findByIdAndMissionIdAndIsActiveTrue(questionId, quizAttempt.getMissionId())
                .orElseThrow(() -> new AimongException(ErrorCode.QUESTION_NOT_FOUND));

        QuestionAnswerKey answerKey = questionAnswerKeyRepository.findById(questionId)
                .orElseThrow(() -> new AimongException(ErrorCode.QUESTION_NOT_FOUND));
        boolean isCorrect = questionAnswerMatcher.matches(question, answerKey.getAnswerPayload(), answer);
        boolean firstAnswerForQuestion = recordAnsweredQuestion(quizAttempt, questionId);
        if (!isCorrect && firstAnswerForQuestion) {
            quizAttempt.recordWrongAnswer();
        }
        ChildProfile childProfile = childProfileRepository.findById(quizAttempt.getChildId())
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
        boolean canRevive = quizAttempt.canRevive();
        return new QuestionCheckResponse(
                questionId,
                isCorrect,
                questionAnswerMatcher.displayAnswer(question, answerKey.getAnswerPayload()),
                answerKey.getExplanation(),
                quizAttempt.getRemainingLives(),
                canRevive,
                CurrencyService.HEART_REVIVE_COST,
                childProfile.getGear(),
                canRevive ? List.of("REVIVE", "ABANDON") : List.of()
        );
    }

    private UUID parseQuestionId(String questionId) {
        try {
            return UUID.fromString(questionId);
        } catch (IllegalArgumentException exception) {
            throw new AimongException(ErrorCode.BAD_REQUEST);
        }
    }

    private List<UUID> parseQuestionIds(String questionIdsJson) {
        try {
            return objectMapper.readValue(questionIdsJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            throw new AimongException(ErrorCode.INTERNAL_SERVER_ERROR, exception);
        }
    }

    private boolean recordAnsweredQuestion(QuizAttempt quizAttempt, UUID questionId) {
        try {
            Set<String> answeredQuestionIds = new LinkedHashSet<>(objectMapper.readValue(
                    quizAttempt.getAnsweredQuestionIdsJson() == null ? "[]" : quizAttempt.getAnsweredQuestionIdsJson(),
                    new TypeReference<List<String>>() {
                    }
            ));
            if (answeredQuestionIds.add(questionId.toString())) {
                quizAttempt.updateAnsweredQuestionIdsJson(objectMapper.writeValueAsString(answeredQuestionIds));
                return true;
            }
            return false;
        } catch (JsonProcessingException exception) {
            throw new AimongException(ErrorCode.INTERNAL_SERVER_ERROR, exception);
        }
    }

}
