package com.aimong.backend.domain.mission.service;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.gacha.entity.Ticket;
import com.aimong.backend.domain.gacha.repository.TicketRepository;
import com.aimong.backend.domain.mission.MissionCompletionPolicy;
import com.aimong.backend.domain.mission.dto.SubmitRequest;
import com.aimong.backend.domain.mission.dto.SubmitResponse;
import com.aimong.backend.domain.mission.dto.StageProgressResponse;
import com.aimong.backend.domain.mission.entity.Mission;
import com.aimong.backend.domain.mission.entity.MissionAttempt;
import com.aimong.backend.domain.mission.entity.MissionDailyProgress;
import com.aimong.backend.domain.mission.entity.QuestionAnswerKey;
import com.aimong.backend.domain.mission.entity.QuizAttempt;
import com.aimong.backend.domain.mission.repository.MissionAttemptRepository;
import com.aimong.backend.domain.mission.repository.MissionDailyProgressRepository;
import com.aimong.backend.domain.mission.repository.MissionRepository;
import com.aimong.backend.domain.mission.repository.QuestionAnswerKeyRepository;
import com.aimong.backend.domain.mission.repository.QuestionBankRepository;
import com.aimong.backend.domain.mission.repository.QuizAttemptRepository;
import com.aimong.backend.domain.pet.entity.PetGrade;
import com.aimong.backend.domain.pet.service.PetGrowthService;
import com.aimong.backend.domain.quest.service.AchievementService;
import com.aimong.backend.domain.quest.service.DailyQuestService;
import com.aimong.backend.domain.quest.service.WeeklyQuestService;
import com.aimong.backend.domain.streak.entity.MilestoneReward;
import com.aimong.backend.domain.streak.entity.StreakRecord;
import com.aimong.backend.domain.streak.repository.FriendStreakRepository;
import com.aimong.backend.domain.streak.repository.MilestoneRewardRepository;
import com.aimong.backend.domain.streak.repository.StreakRecordRepository;
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
    private static final int BASE_XP = 10;
    private static final String MODE_NORMAL = "normal";
    private static final String MODE_REVIEW = "review";
    private static final String ATTEMPT_STATE_SUBMITTED = "submitted";

    private final QuizAttemptRepository quizAttemptRepository;
    private final MissionRepository missionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionAnswerKeyRepository questionAnswerKeyRepository;
    private final MissionAttemptRepository missionAttemptRepository;
    private final MissionDailyProgressRepository missionDailyProgressRepository;
    private final ChildProfileRepository childProfileRepository;
    private final ChildActivityService childActivityService;
    private final TicketRepository ticketRepository;
    private final StreakRecordRepository streakRecordRepository;
    private final FriendStreakRepository friendStreakRepository;
    private final MilestoneRewardRepository milestoneRewardRepository;
    private final DailyQuestService dailyQuestService;
    private final WeeklyQuestService weeklyQuestService;
    private final AchievementService achievementService;
    private final PetGrowthService petGrowthService;
    private final QuizService quizService;
    private final MissionService missionService;
    private final ObjectMapper objectMapper;

    @Transactional
    public SubmitResponse submit(UUID childId, UUID missionId, SubmitRequest request) {
        childActivityService.touchLastActiveAt(childId);
        Mission mission = missionRepository.findById(missionId)
                .filter(Mission::isActive)
                .orElseThrow(() -> new AimongException(ErrorCode.MISSION_NOT_FOUND));

        StageProgressResponse stageProgress = missionService.getMissions(childId).stageProgress();
        if (!missionService.isUnlocked(mission, stageProgress)) {
            throw new AimongException(ErrorCode.MISSION_LOCKED);
        }

        QuizAttempt quizAttempt = quizAttemptRepository.findWithLockById(request.quizAttemptId())
                .orElseThrow(() -> new AimongException(ErrorCode.QUIZ_ATTEMPT_INVALID));

        if (!quizAttempt.getChildId().equals(childId) || !mission.getId().equals(quizAttempt.getMissionId())) {
            throw new AimongException(ErrorCode.FORBIDDEN);
        }
        if (quizAttempt.getSubmittedAt() != null) {
            throw new AimongException(ErrorCode.QUIZ_ATTEMPT_ALREADY_SUBMITTED);
        }
        if (!quizAttempt.getExpiresAt().isAfter(Instant.now())) {
            throw new AimongException(ErrorCode.ATTEMPT_EXPIRED);
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
            UUID questionId = parseQuestionId(answer.questionId());
            QuestionAnswerKey answerKey = answerKeysById.get(questionId);
            boolean isCorrect = answerKey != null && parseAnswerPayload(answerKey.getAnswerPayload()).equals(answer.selected());
            if (isCorrect) {
                score++;
            }

            results.add(new SubmitResponse.ResultResponse(
                    answer.questionId(),
                    isCorrect,
                    answerKey != null ? answerKey.getExplanation() : ""
            ));
        }

        int wrongCount = TOTAL_QUESTIONS - score;
        boolean isPassed = MissionCompletionPolicy.isPassed(score, TOTAL_QUESTIONS);
        boolean isPerfect = score == TOTAL_QUESTIONS;
        LocalDate today = KstDateUtils.today();
        LocalDate weekStart = KstDateUtils.currentWeekStart();
        int attemptNo = Math.toIntExact(missionAttemptRepository.countByChildIdAndMissionIdAndAttemptDate(childId, missionId, today)) + 1;
        boolean isReview = missionDailyProgressRepository.findByChildIdAndMissionIdAndProgressDate(childId, missionId, today)
                .isPresent();
        ChildProfile childProfile = childProfileRepository.findById(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
        Ticket ticket = ticketRepository.findWithLockByChildId(childId)
                .orElseGet(() -> ticketRepository.save(Ticket.create(childId, 0)));
        StreakRecord streakRecord = streakRecordRepository.findWithLockByChildId(childId)
                .orElseGet(() -> streakRecordRepository.save(StreakRecord.create(childId)));

        quizAttempt.markSubmitted(Instant.now());

        if (isReview) {
            final int reviewScore = score;
            missionAttemptRepository.save(MissionAttempt.create(
                    childId,
                    missionId,
                    today,
                    attemptNo,
                    reviewScore,
                    TOTAL_QUESTIONS,
                    0
            ));
            missionDailyProgressRepository.findWithLockByChildIdAndMissionIdAndProgressDate(childId, missionId, today)
                    .ifPresent(progress -> progress.applyReviewAttempt(reviewScore));
            return buildReviewResponse(score, wrongCount, isPassed, isPerfect, childProfile, ticket, streakRecord, results);
        }

        if (!isPassed) {
            missionAttemptRepository.save(MissionAttempt.create(
                    childId,
                    missionId,
                    today,
                    attemptNo,
                    score,
                    TOTAL_QUESTIONS,
                    0
            ));
            return buildFailureResponse(score, wrongCount, isPerfect, childProfile, ticket, streakRecord, results);
        }

        String equippedPetGrade = petGrowthService.findEquippedPetGrade(childId);
        int bonusXp = calculatePetBonusXp(equippedPetGrade, wrongCount);
        boolean streakBonusApplied = hasTodayCompletedPartner(childId, today);
        int xpEarned = calculateEarnedXp(bonusXp, streakBonusApplied);
        int previousLevel = childProfile.getLevel();

        childProfile.applyMissionXp(xpEarned, today, weekStart);
        childProfile.refreshProfileImageType();
        missionDailyProgressRepository.save(MissionDailyProgress.create(
                childId,
                missionId,
                today,
                Instant.now(),
                score,
                TOTAL_QUESTIONS,
                xpEarned
        ));
        try {
            missionAttemptRepository.save(MissionAttempt.create(
                    childId,
                    missionId,
                    today,
                    attemptNo,
                    score,
                    TOTAL_QUESTIONS,
                    xpEarned
            ));
        } catch (RuntimeException exception) {
            throw new AimongException(ErrorCode.SUBMIT_SAVE_FAILED, exception);
        }

        int currentLevel = childProfile.getLevel();
        List<SubmitResponse.RewardResponse> levelRewards = applyLevelRewards(previousLevel, currentLevel, ticket, streakRecord);

        dailyQuestService.updateForMissionSuccess(childId, childProfile, today);
        weeklyQuestService.updateForMissionSuccess(childId, childProfile, weekStart);
        achievementService.unlockByTotalXp(childId, childProfile);

        PetGrowthService.PetGrowthResult petGrowthResult = petGrowthService.applyMissionReward(childId, xpEarned, ticket);

        streakRecord.recordMissionCompletion(today);

        List<SubmitResponse.RewardResponse> rewards = new ArrayList<>(levelRewards);
        rewards.addAll(toRewardResponses(petGrowthResult.rewards()));
        rewards.addAll(applyFixedStreakRewards(childId, streakRecord, ticket));

        return new SubmitResponse(
                MODE_NORMAL,
                true,
                ATTEMPT_STATE_SUBMITTED,
                score,
                TOTAL_QUESTIONS,
                wrongCount,
                true,
                isPerfect,
                petGrowthResult.equippedPetGrade(),
                bonusXp,
                bonusXp > 0 ? "PET_RARITY_BONUS" : null,
                xpEarned,
                petGrowthResult.equippedPetXp(),
                petGrowthResult.petStage(),
                petGrowthResult.petEvolved(),
                petGrowthResult.crownUnlocked(),
                petGrowthResult.crownType(),
                streakRecord.getContinuousDays(),
                streakRecord.getTodayMissionCount(),
                streakBonusApplied,
                rewards,
                toRemainingTickets(ticket),
                childProfile.getProfileImageType().name(),
                childProfile.getProfileImageType() != com.aimong.backend.domain.auth.entity.ProfileImageType.DEFAULT,
                false,
                results
        );
    }

    private SubmitResponse buildReviewResponse(
            int score,
            int wrongCount,
            boolean isPassed,
            boolean isPerfect,
            ChildProfile childProfile,
            Ticket ticket,
            StreakRecord streakRecord,
            List<SubmitResponse.ResultResponse> results
    ) {
        return new SubmitResponse(
                MODE_REVIEW,
                false,
                ATTEMPT_STATE_SUBMITTED,
                score,
                TOTAL_QUESTIONS,
                wrongCount,
                isPassed,
                isPerfect,
                null,
                null,
                null,
                0,
                null,
                null,
                false,
                false,
                null,
                streakRecord.getContinuousDays(),
                streakRecord.getTodayMissionCount(),
                false,
                List.of(),
                toRemainingTickets(ticket),
                childProfile.getProfileImageType().name(),
                childProfile.getProfileImageType() != com.aimong.backend.domain.auth.entity.ProfileImageType.DEFAULT,
                true,
                results
        );
    }

    private SubmitResponse buildFailureResponse(
            int score,
            int wrongCount,
            boolean isPerfect,
            ChildProfile childProfile,
            Ticket ticket,
            StreakRecord streakRecord,
            List<SubmitResponse.ResultResponse> results
    ) {
        return new SubmitResponse(
                MODE_NORMAL,
                false,
                ATTEMPT_STATE_SUBMITTED,
                score,
                TOTAL_QUESTIONS,
                wrongCount,
                false,
                isPerfect,
                null,
                null,
                null,
                0,
                null,
                null,
                false,
                false,
                null,
                streakRecord.getContinuousDays(),
                streakRecord.getTodayMissionCount(),
                false,
                List.of(),
                toRemainingTickets(ticket),
                childProfile.getProfileImageType().name(),
                childProfile.getProfileImageType() != com.aimong.backend.domain.auth.entity.ProfileImageType.DEFAULT,
                false,
                results
        );
    }

    private int calculatePetBonusXp(String equippedPetGrade, int wrongCount) {
        if (equippedPetGrade == null) {
            return 0;
        }
        return switch (PetGrade.valueOf(equippedPetGrade)) {
            case NORMAL -> wrongCount == 0 ? 10 : 0;
            case RARE -> wrongCount <= 1 ? 10 : 0;
            case EPIC -> wrongCount <= 2 ? 10 : 0;
            case LEGEND -> wrongCount <= 2 ? 15 : 0;
        };
    }

    private int calculateEarnedXp(int bonusXp, boolean streakBonusApplied) {
        int xpEarned = BASE_XP + bonusXp;
        if (!streakBonusApplied) {
            return xpEarned;
        }
        return (int) Math.floor(xpEarned * 1.5d);
    }

    private boolean hasTodayCompletedPartner(UUID childId, LocalDate today) {
        return friendStreakRepository.findById(childId)
                .flatMap(friendStreak -> streakRecordRepository.findById(friendStreak.getPartnerChildId()))
                .map(partnerStreak -> isCompletedToday(partnerStreak, today))
                .orElse(false);
    }

    private boolean isCompletedToday(StreakRecord streakRecord, LocalDate today) {
        return today.equals(streakRecord.getLastCompletedDate()) && streakRecord.getTodayMissionCount() > 0;
    }

    private List<SubmitResponse.RewardResponse> applyLevelRewards(int previousLevel, int currentLevel, Ticket ticket, StreakRecord streakRecord) {
        List<SubmitResponse.RewardResponse> rewards = new ArrayList<>();
        for (int level = previousLevel + 1; level <= currentLevel; level++) {
            if (level % 3 == 0) {
                streakRecord.addShield(1);
                rewards.add(new SubmitResponse.RewardResponse(
                        "SHIELD",
                        null,
                        1,
                        null,
                        "LEVEL_REWARD_LV" + level
                ));
            }
            if (level % 5 == 0) {
                ticket.addNormal(1);
                rewards.add(new SubmitResponse.RewardResponse(
                        "TICKET",
                        "NORMAL",
                        1,
                        null,
                        "LEVEL_REWARD_LV" + level
                ));
            }
        }
        return rewards;
    }

    private List<SubmitResponse.RewardResponse> applyFixedStreakRewards(UUID childId, StreakRecord streakRecord, Ticket ticket) {
        List<SubmitResponse.RewardResponse> rewards = new ArrayList<>();
        if (streakRecord.getContinuousDays() == 7 && !milestoneRewardRepository.existsByChildIdAndMilestoneDays(childId, (short) 7)) {
            ticket.addRare(1);
            milestoneRewardRepository.save(MilestoneReward.create(childId, (short) 7));
            rewards.add(new SubmitResponse.RewardResponse(
                    "TICKET",
                    "RARE",
                    1,
                    null,
                    "STREAK_MILESTONE_DAY7"
            ));
        }
        if (streakRecord.getContinuousDays() == 30 && !milestoneRewardRepository.existsByChildIdAndMilestoneDays(childId, (short) 30)) {
            ticket.addEpic(1);
            milestoneRewardRepository.save(MilestoneReward.create(childId, (short) 30));
            rewards.add(new SubmitResponse.RewardResponse(
                    "TICKET",
                    "EPIC",
                    1,
                    null,
                    "STREAK_MILESTONE_DAY30"
            ));
        }
        return rewards;
    }

    private List<SubmitResponse.RewardResponse> toRewardResponses(List<PetGrowthService.PetReward> petRewards) {
        return petRewards.stream()
                .map(reward -> new SubmitResponse.RewardResponse(
                        reward.type(),
                        reward.ticketType(),
                        reward.count(),
                        null,
                        reward.reason()
                ))
                .toList();
    }

    private void validateQuestionIds(List<UUID> expectedQuestionIds, List<SubmitRequest.AnswerRequest> answers) {
        Set<UUID> actualQuestionIds = answers.stream()
                .map(answer -> parseQuestionId(answer.questionId()))
                .collect(Collectors.toSet());

        if (actualQuestionIds.size() != answers.size()) {
            throw new AimongException(ErrorCode.QUIZ_DUPLICATE_QUESTION);
        }
        if (actualQuestionIds.size() != TOTAL_QUESTIONS || !actualQuestionIds.containsAll(expectedQuestionIds)) {
            throw new AimongException(ErrorCode.QUIZ_ANSWERS_REQUIRED);
        }
    }

    private UUID parseQuestionId(String questionId) {
        try {
            return UUID.fromString(questionId);
        } catch (IllegalArgumentException exception) {
            throw new AimongException(ErrorCode.BAD_REQUEST);
        }
    }

    private String parseAnswerPayload(String answerPayload) {
        try {
            return objectMapper.readValue(answerPayload, String.class);
        } catch (JsonProcessingException exception) {
            throw new AimongException(ErrorCode.INTERNAL_SERVER_ERROR, exception);
        }
    }

    private SubmitResponse.RemainingTicketsResponse toRemainingTickets(Ticket ticket) {
        return new SubmitResponse.RemainingTicketsResponse(
                ticket.getNormal(),
                ticket.getRare(),
                ticket.getEpic()
        );
    }
}
