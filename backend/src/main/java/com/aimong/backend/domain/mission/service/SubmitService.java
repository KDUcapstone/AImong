package com.aimong.backend.domain.mission.service;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.mission.MissionCompletionPolicy;
import com.aimong.backend.domain.mission.dto.SubmitRequest;
import com.aimong.backend.domain.mission.dto.SubmitResponse;
import com.aimong.backend.domain.mission.entity.Mission;
import com.aimong.backend.domain.mission.entity.MissionAttempt;
import com.aimong.backend.domain.mission.entity.QuestionAnswerKey;
import com.aimong.backend.domain.mission.entity.QuizAttempt;
import com.aimong.backend.domain.mission.repository.MissionAttemptRepository;
import com.aimong.backend.domain.mission.repository.MissionRepository;
import com.aimong.backend.domain.mission.repository.QuestionAnswerKeyRepository;
import com.aimong.backend.domain.mission.repository.QuestionBankRepository;
import com.aimong.backend.domain.mission.repository.QuizAttemptRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.aimong.backend.global.util.KstDateUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubmitService {

    private static final int TOTAL_QUESTIONS = 10;

    private final QuizAttemptRepository quizAttemptRepository;
    private final MissionRepository missionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionAnswerKeyRepository questionAnswerKeyRepository;
    private final MissionAttemptRepository missionAttemptRepository;
    private final ChildProfileRepository childProfileRepository;
    private final QuizService quizService;
    private final ObjectMapper objectMapper;

    @Transactional
    public SubmitResponse submit(UUID childId, UUID missionId, SubmitRequest request) {
        Mission mission = missionRepository.findById(missionId)
                .filter(Mission::isActive)
                .orElseThrow(() -> new AimongException(ErrorCode.MISSION_NOT_FOUND));

        QuizAttempt quizAttempt = quizAttemptRepository.findByIdAndChildId(request.quizAttemptId(), childId)
                .orElseThrow(() -> new AimongException(ErrorCode.QUIZ_ATTEMPT_INVALID));

        if (!mission.getId().equals(quizAttempt.getMissionId())) {
            throw new AimongException(ErrorCode.FORBIDDEN);
        }
        if (quizAttempt.getSubmittedAt() != null) {
            throw new AimongException(ErrorCode.QUIZ_ATTEMPT_ALREADY_SUBMITTED);
        }
        if (!quizAttempt.getExpiresAt().isAfter(Instant.now())) {
            throw new AimongException(ErrorCode.QUIZ_ATTEMPT_INVALID);
        }
        if (request.answers().size() != TOTAL_QUESTIONS) {
            throw new AimongException(ErrorCode.BAD_REQUEST);
        }

        List<UUID> questionIds = quizService.parseQuestionIds(quizAttempt.getQuestionIdsJson());
        validateQuestionIds(questionIds, request.answers());

        Map<UUID, QuestionAnswerKey> answerKeysById = questionAnswerKeyRepository.findAllByQuestionIdIn(questionIds)
                .stream()
                .collect(LinkedHashMap::new, (map, key) -> map.put(key.getQuestionId(), key), Map::putAll);

        if (questionBankRepository.findAllByIdIn(questionIds).size() != TOTAL_QUESTIONS
                || answerKeysById.size() != TOTAL_QUESTIONS) {
            throw new AimongException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        int score = 0;
        List<SubmitResponse.ResultResponse> results = new ArrayList<>();
        for (SubmitRequest.AnswerRequest answer : request.answers()) {
            QuestionAnswerKey answerKey = answerKeysById.get(answer.questionId());
            boolean isCorrect = answerKey != null && parseAnswerPayload(answerKey.getAnswerPayload()).equals(answer.selected());
            if (isCorrect) {
                score++;
            }

            results.add(new SubmitResponse.ResultResponse(
                    answer.questionId().toString(),
                    isCorrect,
                    answerKey != null ? answerKey.getExplanation() : ""
            ));
        }

        boolean isPassed = MissionCompletionPolicy.isPassed(score, TOTAL_QUESTIONS);
        boolean isPerfect = score == TOTAL_QUESTIONS;
        LocalDate today = KstDateUtils.today();
        long todayAttempts = missionAttemptRepository.countByChildIdAndMissionIdAndAttemptDate(childId, missionId, today);
        int attemptNo = Math.toIntExact(todayAttempts) + 1;
        boolean isReview = attemptNo > 1;
        int xpEarned = calculateXp(isPassed, isPerfect, isReview);

        ChildProfile childProfile = childProfileRepository.findById(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
        if (xpEarned > 0) {
            childProfile.applyMissionXp(xpEarned, today, KstDateUtils.currentWeekStart());
        }

        missionAttemptRepository.save(MissionAttempt.create(
                childId,
                missionId,
                today,
                attemptNo,
                score,
                TOTAL_QUESTIONS,
                xpEarned
        ));
        quizAttempt.markSubmitted(Instant.now());

        return new SubmitResponse(
                score,
                TOTAL_QUESTIONS,
                isPerfect,
                xpEarned,
                null,
                null,
                false,
                false,
                null,
                0,
                attemptNo,
                List.of(),
                new SubmitResponse.RemainingTicketsResponse(0, 0, 0),
                childProfile.getProfileImageType().name(),
                false,
                isReview,
                results
        );
    }

    private void validateQuestionIds(List<UUID> expectedQuestionIds, List<SubmitRequest.AnswerRequest> answers) {
        Set<UUID> actualQuestionIds = answers.stream()
                .map(SubmitRequest.AnswerRequest::questionId)
                .collect(Collectors.toSet());

        if (actualQuestionIds.size() != TOTAL_QUESTIONS || !actualQuestionIds.containsAll(expectedQuestionIds)) {
            throw new AimongException(ErrorCode.BAD_REQUEST);
        }
    }

    private int calculateXp(boolean isPassed, boolean isPerfect, boolean isReview) {
        if (!isPassed) {
            return 0;
        }

        int xpEarned = 10;
        if (isPerfect) {
            xpEarned += 10;
        }
        if (isReview) {
            xpEarned = Math.floorDiv(xpEarned, 2);
        }
        return xpEarned;
    }

    private String parseAnswerPayload(String answerPayload) {
        try {
            return objectMapper.readValue(answerPayload, String.class);
        } catch (JsonProcessingException exception) {
            throw new AimongException(ErrorCode.INTERNAL_SERVER_ERROR, exception);
        }
    }
}
