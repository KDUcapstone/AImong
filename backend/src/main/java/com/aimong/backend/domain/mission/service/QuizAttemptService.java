package com.aimong.backend.domain.mission.service;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.mission.dto.AbandonAttemptRequest;
import com.aimong.backend.domain.mission.dto.AbandonAttemptResponse;
import com.aimong.backend.domain.mission.dto.MissionListResponse;
import com.aimong.backend.domain.mission.dto.MissionStatusResponse;
import com.aimong.backend.domain.mission.dto.QuizAttemptResponse;
import com.aimong.backend.domain.mission.dto.ReviveAttemptRequest;
import com.aimong.backend.domain.mission.dto.ReviveAttemptResponse;
import com.aimong.backend.domain.mission.entity.Mission;
import com.aimong.backend.domain.mission.entity.MissionSet;
import com.aimong.backend.domain.mission.entity.MissionSetProgress;
import com.aimong.backend.domain.mission.entity.QuizAttempt;
import com.aimong.backend.domain.mission.entity.QuizAttemptStatus;
import com.aimong.backend.domain.mission.repository.MissionRepository;
import com.aimong.backend.domain.mission.repository.MissionSetProgressRepository;
import com.aimong.backend.domain.mission.repository.MissionSetRepository;
import com.aimong.backend.domain.mission.repository.QuizAttemptRepository;
import com.aimong.backend.domain.reward.entity.CurrencyTransactionReason;
import com.aimong.backend.domain.reward.service.CurrencyService;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuizAttemptService {

    private final MissionRepository missionRepository;
    private final MissionSetRepository missionSetRepository;
    private final MissionSetProgressRepository missionSetProgressRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final ChildProfileRepository childProfileRepository;
    private final ChildActivityService childActivityService;
    private final MissionService missionService;
    private final QuizService quizService;
    private final CurrencyService currencyService;
    private final ObjectMapper objectMapper;

    @Transactional
    public MissionStatusResponse getMissionStatus(UUID childId, UUID missionId) {
        childActivityService.touchLastActiveAt(childId);
        Mission mission = missionRepository.findById(missionId)
                .filter(Mission::isActive)
                .orElseThrow(() -> new AimongException(ErrorCode.MISSION_NOT_FOUND));
        List<MissionSet> sets = missionSetRepository.findAllByMissionIdAndActiveTrueOrderByStarLevelAscVariantNoAscSetIdAsc(missionId);
        boolean unlocked = sets.stream().findFirst()
                .map(set -> missionService.isUnlocked(childId, set))
                .orElse(true);

        ChildProfile childProfile = childProfileRepository.findWithLockById(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
        childProfile.recoverEnergy(Instant.now());
        Map<Integer, List<MissionSet>> setsByStar = sets.stream()
                .collect(Collectors.groupingBy(MissionSet::getStarLevel));
        List<MissionStatusResponse.StarLevelStatus> starLevels = setsByStar.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> toStarStatus(childId, unlocked, entry.getKey(), entry.getValue()))
                .toList();
        MissionStatusResponse.InProgressAttempt inProgressAttempt = quizAttemptRepository
                .findFirstByChildIdAndMissionIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                        childId,
                        missionId,
                        QuizAttemptStatus.IN_PROGRESS,
                        Instant.now()
                )
                .map(attempt -> new MissionStatusResponse.InProgressAttempt(
                        attempt.getId(),
                        attempt.getSetId(),
                        attempt.getStarLevel(),
                        attempt.getExpiresAt()
                ))
                .orElse(null);
        return new MissionStatusResponse(
                mission.getId(),
                mission.getMissionCode(),
                mission.getTitle(),
                unlocked,
                unlocked && childProfile.getEnergy() >= ChildProfile.MISSION_ENERGY_COST,
                new MissionStatusResponse.EnergyStatus(
                        childProfile.getEnergy(),
                        ChildProfile.MISSION_ENERGY_COST,
                        ChildProfile.MAX_ENERGY,
                        childProfile.nextEnergyRecoverAt()
                ),
                starLevels,
                inProgressAttempt
        );
    }

    @Transactional
    public QuizAttemptResponse getAttempt(UUID childId, UUID attemptId) {
        childActivityService.touchLastActiveAt(childId);
        QuizAttempt attempt = quizAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new AimongException(ErrorCode.ATTEMPT_NOT_FOUND));
        if (!attempt.getChildId().equals(childId)) {
            throw new AimongException(ErrorCode.ATTEMPT_NOT_FOUND);
        }
        if (attempt.getStatus() == QuizAttemptStatus.IN_PROGRESS && !attempt.getExpiresAt().isAfter(Instant.now())) {
            attempt.markExpired();
            throw new AimongException(ErrorCode.ATTEMPT_EXPIRED);
        }
        if (attempt.getStatus() == QuizAttemptStatus.EXPIRED) {
            throw new AimongException(ErrorCode.ATTEMPT_EXPIRED);
        }
        if (attempt.getStatus() != QuizAttemptStatus.IN_PROGRESS) {
            throw new AimongException(ErrorCode.ATTEMPT_ALREADY_CLOSED);
        }
        return toAttemptResponse(attempt);
    }

    @Transactional
    public AbandonAttemptResponse abandon(UUID childId, UUID attemptId, AbandonAttemptRequest request) {
        childActivityService.touchLastActiveAt(childId);
        QuizAttempt attempt = quizAttemptRepository.findWithLockById(attemptId)
                .orElseThrow(() -> new AimongException(ErrorCode.ATTEMPT_NOT_FOUND));
        if (!attempt.getChildId().equals(childId)) {
            throw new AimongException(ErrorCode.ATTEMPT_NOT_FOUND);
        }
        if (attempt.getStatus() == QuizAttemptStatus.IN_PROGRESS && !attempt.getExpiresAt().isAfter(Instant.now())) {
            attempt.markExpired();
            throw new AimongException(ErrorCode.ATTEMPT_EXPIRED);
        }
        if (attempt.getStatus() != QuizAttemptStatus.IN_PROGRESS) {
            throw new AimongException(ErrorCode.ATTEMPT_ALREADY_CLOSED);
        }
        String reason = request == null || request.reason() == null || request.reason().isBlank()
                ? "USER_EXIT"
                : request.reason();
        attempt.abandon(reason, Instant.now());
        return new AbandonAttemptResponse(true, attempt.getId(), attempt.getStatus().name(), false);
    }

    @Transactional
    public ReviveAttemptResponse revive(UUID childId, UUID attemptId, ReviveAttemptRequest request) {
        childActivityService.touchLastActiveAt(childId);
        if (request == null || !request.useCurrency()) {
            throw new AimongException(ErrorCode.BAD_REQUEST);
        }
        QuizAttempt attempt = quizAttemptRepository.findWithLockById(attemptId)
                .orElseThrow(() -> new AimongException(ErrorCode.ATTEMPT_NOT_FOUND));
        if (!attempt.getChildId().equals(childId)) {
            throw new AimongException(ErrorCode.ATTEMPT_NOT_FOUND);
        }
        if (attempt.getStatus() == QuizAttemptStatus.IN_PROGRESS && !attempt.getExpiresAt().isAfter(Instant.now())) {
            attempt.markExpired();
            throw new AimongException(ErrorCode.ATTEMPT_EXPIRED);
        }
        if (!attempt.canRevive()) {
            throw new AimongException(ErrorCode.ATTEMPT_NOT_REVIVABLE);
        }
        ChildProfile childProfile = childProfileRepository.findWithLockById(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
        if (!currencyService.consumeGear(
                childProfile,
                CurrencyService.HEART_REVIVE_COST,
                CurrencyTransactionReason.HEART_REVIVE,
                "QUIZ_ATTEMPT",
                attempt.getId().toString()
        )) {
            throw new AimongException(ErrorCode.GEAR_NOT_ENOUGH);
        }
        attempt.revive(Instant.now());
        return new ReviveAttemptResponse(
                attempt.getId(),
                attempt.getRemainingLives(),
                attempt.getReviveCount(),
                CurrencyService.HEART_REVIVE_COST,
                childProfile.getGear()
        );
    }

    private MissionStatusResponse.StarLevelStatus toStarStatus(
            UUID childId,
            boolean unlocked,
            int starLevel,
            List<MissionSet> sets
    ) {
        List<String> setIds = sets.stream().map(MissionSet::getSetId).toList();
        long completed = setIds.isEmpty()
                ? 0
                : missionSetProgressRepository.findAllByChildIdAndSetIdIn(childId, setIds)
                .stream()
                .map(MissionSetProgress::getSetId)
                .distinct()
                .count();
        return new MissionStatusResponse.StarLevelStatus(
                starLevel,
                MissionListResponse.labelForStar(starLevel),
                sets.size(),
                completed,
                unlocked,
                completed > 0
        );
    }

    private QuizAttemptResponse toAttemptResponse(QuizAttempt attempt) {
        long remainingSeconds = Math.max(0L, Duration.between(Instant.now(), attempt.getExpiresAt()).toSeconds());
        return new QuizAttemptResponse(
                attempt.getId(),
                attempt.getSetId(),
                attempt.getMissionId(),
                attempt.getStarLevel(),
                attempt.getStatus().name(),
                attempt.isReview(),
                attempt.getExpiresAt(),
                remainingSeconds,
                parseAnsweredQuestionIds(attempt.getAnsweredQuestionIdsJson()),
                attempt.getRemainingLives(),
                attempt.getWrongCountInSession(),
                attempt.getReviveCount(),
                attempt.canRevive(),
                quizService.parseQuestionIds(attempt.getQuestionIdsJson()).size()
        );
    }

    private List<String> parseAnsweredQuestionIds(String answeredQuestionIdsJson) {
        try {
            return objectMapper.readValue(
                    answeredQuestionIdsJson == null ? "[]" : answeredQuestionIdsJson,
                    new TypeReference<>() {
                    }
            );
        } catch (JsonProcessingException exception) {
            throw new AimongException(ErrorCode.INTERNAL_SERVER_ERROR, exception);
        }
    }
}
