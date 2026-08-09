package com.aimong.backend.domain.mission.service;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.gacha.entity.Ticket;
import com.aimong.backend.domain.gacha.entity.TicketType;
import com.aimong.backend.domain.gacha.repository.TicketRepository;
import com.aimong.backend.domain.mission.MissionCompletionPolicy;
import com.aimong.backend.domain.mission.dto.SubmitRequest;
import com.aimong.backend.domain.mission.dto.SubmitResponse;
import com.aimong.backend.domain.mission.dto.StageProgressResponse;
import com.aimong.backend.domain.mission.entity.Mission;
import com.aimong.backend.domain.mission.entity.MissionAnswerResult;
import com.aimong.backend.domain.mission.entity.MissionAttempt;
import com.aimong.backend.domain.mission.entity.MissionDailyProgress;
import com.aimong.backend.domain.mission.entity.MissionSet;
import com.aimong.backend.domain.mission.entity.MissionSetProgress;
import com.aimong.backend.domain.mission.entity.QuestionAnswerKey;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.entity.QuizAttempt;
import com.aimong.backend.domain.mission.entity.QuizAttemptStatus;
import com.aimong.backend.domain.mission.repository.MissionAnswerResultRepository;
import com.aimong.backend.domain.mission.repository.MissionAttemptRepository;
import com.aimong.backend.domain.mission.repository.MissionDailyProgressRepository;
import com.aimong.backend.domain.mission.repository.MissionRepository;
import com.aimong.backend.domain.mission.repository.MissionSetProgressRepository;
import com.aimong.backend.domain.mission.repository.MissionSetRepository;
import com.aimong.backend.domain.mission.repository.QuestionAnswerKeyRepository;
import com.aimong.backend.domain.mission.repository.QuestionBankRepository;
import com.aimong.backend.domain.mission.repository.QuizAttemptRepository;
import com.aimong.backend.domain.pet.entity.PetGrade;
import com.aimong.backend.domain.pet.service.PetGrowthService;
import com.aimong.backend.domain.quest.service.AchievementService;
import com.aimong.backend.domain.quest.service.DailyQuestService;
import com.aimong.backend.domain.quest.service.WeeklyQuestService;
import com.aimong.backend.domain.reward.entity.CurrencyTransactionReason;
import com.aimong.backend.domain.reward.service.CurrencyService;
import com.aimong.backend.domain.stagereward.dto.StageCompletionRewardResponse;
import com.aimong.backend.domain.stagereward.service.StageCompletionRewardService;
import com.aimong.backend.domain.streak.entity.StreakRecord;
import com.aimong.backend.domain.streak.repository.FriendStreakRepository;
import com.aimong.backend.domain.streak.repository.StreakRecordRepository;
import com.aimong.backend.domain.streak.service.MilestoneService;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.aimong.backend.global.util.KstDateUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubmitService {

    private static final int TOTAL_QUESTIONS = 10;
    private static final int BASE_XP = 10;
    private static final int PERFECT_BONUS_XP = 10;
    private static final String MODE_NORMAL = "normal";
    private static final String MODE_REVIEW = "review";
    private static final String ATTEMPT_STATE_SUBMITTED = "submitted";

    private final QuizAttemptRepository quizAttemptRepository;
    private final MissionRepository missionRepository;
    private final MissionSetRepository missionSetRepository;
    private final MissionSetProgressRepository missionSetProgressRepository;
    private final QuestionBankRepository questionBankRepository;
    private final QuestionAnswerKeyRepository questionAnswerKeyRepository;
    private final MissionAnswerResultRepository missionAnswerResultRepository;
    private final MissionAttemptRepository missionAttemptRepository;
    private final MissionDailyProgressRepository missionDailyProgressRepository;
    private final ChildProfileRepository childProfileRepository;
    private final ChildActivityService childActivityService;
    private final TicketRepository ticketRepository;
    private final StreakRecordRepository streakRecordRepository;
    private final FriendStreakRepository friendStreakRepository;
    private final MilestoneService milestoneService;
    private final DailyQuestService dailyQuestService;
    private final WeeklyQuestService weeklyQuestService;
    private final AchievementService achievementService;
    private final PetGrowthService petGrowthService;
    private final CurrencyService currencyService;
    private final StageCompletionRewardService stageCompletionRewardService;
    private final QuizService quizService;
    private final MissionService missionService;
    private final QuestionAnswerMatcher questionAnswerMatcher;
    private final ObjectMapper objectMapper;

    @Autowired
    public SubmitService(
            QuizAttemptRepository quizAttemptRepository,
            MissionRepository missionRepository,
            MissionSetRepository missionSetRepository,
            MissionSetProgressRepository missionSetProgressRepository,
            QuestionBankRepository questionBankRepository,
            QuestionAnswerKeyRepository questionAnswerKeyRepository,
            MissionAnswerResultRepository missionAnswerResultRepository,
            MissionAttemptRepository missionAttemptRepository,
            MissionDailyProgressRepository missionDailyProgressRepository,
            ChildProfileRepository childProfileRepository,
            ChildActivityService childActivityService,
            TicketRepository ticketRepository,
            StreakRecordRepository streakRecordRepository,
            FriendStreakRepository friendStreakRepository,
            MilestoneService milestoneService,
            DailyQuestService dailyQuestService,
            WeeklyQuestService weeklyQuestService,
            AchievementService achievementService,
            PetGrowthService petGrowthService,
            CurrencyService currencyService,
            StageCompletionRewardService stageCompletionRewardService,
            QuizService quizService,
            MissionService missionService,
            QuestionAnswerMatcher questionAnswerMatcher,
            ObjectMapper objectMapper
    ) {
        this.quizAttemptRepository = quizAttemptRepository;
        this.missionRepository = missionRepository;
        this.missionSetRepository = missionSetRepository;
        this.missionSetProgressRepository = missionSetProgressRepository;
        this.questionBankRepository = questionBankRepository;
        this.questionAnswerKeyRepository = questionAnswerKeyRepository;
        this.missionAnswerResultRepository = missionAnswerResultRepository;
        this.missionAttemptRepository = missionAttemptRepository;
        this.missionDailyProgressRepository = missionDailyProgressRepository;
        this.childProfileRepository = childProfileRepository;
        this.childActivityService = childActivityService;
        this.ticketRepository = ticketRepository;
        this.streakRecordRepository = streakRecordRepository;
        this.friendStreakRepository = friendStreakRepository;
        this.milestoneService = milestoneService;
        this.dailyQuestService = dailyQuestService;
        this.weeklyQuestService = weeklyQuestService;
        this.achievementService = achievementService;
        this.petGrowthService = petGrowthService;
        this.currencyService = currencyService;
        this.stageCompletionRewardService = stageCompletionRewardService;
        this.quizService = quizService;
        this.missionService = missionService;
        this.questionAnswerMatcher = questionAnswerMatcher;
        this.objectMapper = objectMapper;
    }

    public SubmitService(
            QuizAttemptRepository quizAttemptRepository,
            MissionRepository missionRepository,
            QuestionBankRepository questionBankRepository,
            QuestionAnswerKeyRepository questionAnswerKeyRepository,
            MissionAnswerResultRepository missionAnswerResultRepository,
            MissionAttemptRepository missionAttemptRepository,
            MissionDailyProgressRepository missionDailyProgressRepository,
            ChildProfileRepository childProfileRepository,
            ChildActivityService childActivityService,
            TicketRepository ticketRepository,
            StreakRecordRepository streakRecordRepository,
            FriendStreakRepository friendStreakRepository,
            MilestoneService milestoneService,
            DailyQuestService dailyQuestService,
            WeeklyQuestService weeklyQuestService,
            AchievementService achievementService,
            PetGrowthService petGrowthService,
            QuizService quizService,
            MissionService missionService,
            ObjectMapper objectMapper
    ) {
        this.quizAttemptRepository = quizAttemptRepository;
        this.missionRepository = missionRepository;
        this.missionSetRepository = null;
        this.missionSetProgressRepository = null;
        this.questionBankRepository = questionBankRepository;
        this.questionAnswerKeyRepository = questionAnswerKeyRepository;
        this.missionAnswerResultRepository = missionAnswerResultRepository;
        this.missionAttemptRepository = missionAttemptRepository;
        this.missionDailyProgressRepository = missionDailyProgressRepository;
        this.childProfileRepository = childProfileRepository;
        this.childActivityService = childActivityService;
        this.ticketRepository = ticketRepository;
        this.streakRecordRepository = streakRecordRepository;
        this.friendStreakRepository = friendStreakRepository;
        this.milestoneService = milestoneService;
        this.dailyQuestService = dailyQuestService;
        this.weeklyQuestService = weeklyQuestService;
        this.achievementService = achievementService;
        this.petGrowthService = petGrowthService;
        this.currencyService = null;
        this.stageCompletionRewardService = null;
        this.quizService = quizService;
        this.missionService = missionService;
        this.questionAnswerMatcher = new QuestionAnswerMatcher(objectMapper);
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SubmitResponse submit(UUID childId, UUID missionId, SubmitRequest request) {
        childActivityService.touchLastActiveAt(childId);
        Mission mission = missionRepository.findById(missionId)
                .filter(Mission::isActive)
                .orElseThrow(() -> new AimongException(ErrorCode.MISSION_NOT_FOUND));

        StageProgressResponse stageProgress = missionService.stageProgressForLegacy(childId);
        if (!missionService.isUnlockedForChild(childId, mission, stageProgress)) {
            throw new AimongException(ErrorCode.MISSION_LOCKED);
        }

        QuizAttempt quizAttempt = resolveQuizAttempt(childId, missionId, request.quizAttemptId());

        if (!quizAttempt.getChildId().equals(childId) || !mission.getId().equals(quizAttempt.getMissionId())) {
            throw new AimongException(ErrorCode.FORBIDDEN);
        }
        MissionSet missionSet = quizAttempt.getSetId() == null ? null : missionSetRepository.findById(quizAttempt.getSetId()).orElse(null);
        return submitValidated(childId, mission, missionSet, quizAttempt, request);
    }

    @Transactional
    public SubmitResponse submit(UUID childId, String setId, SubmitRequest request) {
        childActivityService.touchLastActiveAt(childId);
        MissionService.MissionSetAvailability availabilityBeforeSubmit = missionService.missionSetAvailability(childId);
        MissionSet missionSet = availabilityBeforeSubmit.activeSets()
                .stream()
                .filter(set -> setId.equals(set.getSetId()))
                .findFirst()
                .orElseThrow(() -> new AimongException(ErrorCode.MISSION_SET_NOT_FOUND));
        if (availabilityBeforeSubmit.playableSets()
                .stream()
                .noneMatch(set -> setId.equals(set.getSetId()))) {
            throw new AimongException(ErrorCode.MISSION_SET_LOCKED);
        }
        Mission mission = missionRepository.findById(missionSet.getMissionId())
                .filter(Mission::isActive)
                .orElseThrow(() -> new AimongException(ErrorCode.MISSION_NOT_FOUND));
        QuizAttempt quizAttempt = resolveQuizAttempt(childId, setId, request.quizAttemptId());
        if (!quizAttempt.getChildId().equals(childId)) {
            throw new AimongException(ErrorCode.FORBIDDEN);
        }
        if (quizAttempt.getSetId() == null || !quizAttempt.getSetId().equals(setId)) {
            throw new AimongException(ErrorCode.MISSION_SET_MISMATCH);
        }
        return submitValidated(childId, mission, missionSet, quizAttempt, request, availabilityBeforeSubmit);
    }

    private QuizAttempt resolveQuizAttempt(UUID childId, UUID missionId, UUID quizAttemptId) {
        if (quizAttemptId != null) {
            return quizAttemptRepository.findWithLockById(quizAttemptId)
                    .orElseThrow(() -> new AimongException(ErrorCode.QUIZ_ATTEMPT_INVALID));
        }
        return quizAttemptRepository.findFirstByChildIdAndMissionIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                        childId,
                        missionId,
                        QuizAttemptStatus.IN_PROGRESS,
                        Instant.now()
                )
                .orElseThrow(() -> new AimongException(ErrorCode.QUIZ_ATTEMPT_INVALID));
    }

    private QuizAttempt resolveQuizAttempt(UUID childId, String setId, UUID quizAttemptId) {
        if (quizAttemptId != null) {
            return quizAttemptRepository.findWithLockById(quizAttemptId)
                    .orElseThrow(() -> new AimongException(ErrorCode.QUIZ_ATTEMPT_INVALID));
        }
        return quizAttemptRepository.findFirstByChildIdAndSetIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
                        childId,
                        setId,
                        QuizAttemptStatus.IN_PROGRESS,
                        Instant.now()
                )
                .orElseThrow(() -> new AimongException(ErrorCode.QUIZ_ATTEMPT_INVALID));
    }

    private SubmitResponse submitValidated(
            UUID childId,
            Mission mission,
            MissionSet missionSet,
            QuizAttempt quizAttempt,
            SubmitRequest request
    ) {
        return submitValidated(childId, mission, missionSet, quizAttempt, request, null);
    }

    private SubmitResponse submitValidated(
            UUID childId,
            Mission mission,
            MissionSet missionSet,
            QuizAttempt quizAttempt,
            SubmitRequest request,
            MissionService.MissionSetAvailability availabilityBeforeSubmit
    ) {
        if (quizAttempt.getStatus() == QuizAttemptStatus.SUBMITTED || quizAttempt.getSubmittedAt() != null) {
            throw new AimongException(ErrorCode.QUIZ_ATTEMPT_ALREADY_SUBMITTED);
        }
        if (quizAttempt.getStatus() == QuizAttemptStatus.ABANDONED) {
            throw new AimongException(ErrorCode.ATTEMPT_ABANDONED);
        }
        if (quizAttempt.getStatus() == QuizAttemptStatus.EXPIRED) {
            throw new AimongException(ErrorCode.ATTEMPT_EXPIRED);
        }
        if (!quizAttempt.getExpiresAt().isAfter(Instant.now())) {
            quizAttempt.markExpired();
            throw new AimongException(ErrorCode.ATTEMPT_EXPIRED);
        }
        List<UUID> questionIds = quizService.parseQuestionIds(quizAttempt.getQuestionIdsJson());
        validateQuestionIds(questionIds, request.answers());

        Map<UUID, QuestionAnswerKey> answerKeysById = questionAnswerKeyRepository.findAllByQuestionIdIn(questionIds)
                .stream()
                .collect(LinkedHashMap::new, (map, key) -> map.put(key.getQuestionId(), key), Map::putAll);
        Map<UUID, QuestionBank> questionsById = questionBankRepository.findAllByIdIn(questionIds)
                .stream()
                .collect(LinkedHashMap::new, (map, question) -> map.put(question.getId(), question), Map::putAll);

        if (questionsById.size() != TOTAL_QUESTIONS || answerKeysById.size() != TOTAL_QUESTIONS) {
            throw new AimongException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        int score = 0;
        List<SubmitResponse.ResultResponse> results = new ArrayList<>();
        for (SubmitRequest.AnswerRequest answer : request.answers()) {
            UUID questionId = parseQuestionId(answer.questionId());
            QuestionAnswerKey answerKey = answerKeysById.get(questionId);
            QuestionBank question = questionsById.get(questionId);
            boolean isCorrect = answerKey != null
                    && question != null
                    && questionAnswerMatcher.matches(question, answerKey.getAnswerPayload(), answer.answer());
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
        UUID missionId = mission.getId();
        String setId = missionSet == null ? quizAttempt.getSetId() : missionSet.getSetId();
        Integer starLevel = missionSet != null ? Integer.valueOf(missionSet.getStarLevel()) : quizAttempt.getStarLevel();
        if (starLevel == null) {
            starLevel = 1;
        }
        Integer variantNo = missionSet == null ? null : missionSet.getVariantNo();
        int attemptNo = Math.toIntExact(missionAttemptRepository.countByChildIdAndMissionIdAndAttemptDate(childId, missionId, today)) + 1;
        boolean isReview = quizAttempt.isReview();
        ChildProfile childProfile = findChildProfileForSubmit(childId);
        StreakRecord streakRecord = streakRecordRepository.findWithLockByChildId(childId)
                .orElseGet(() -> streakRecordRepository.save(StreakRecord.create(childId)));
        String equippedPetGrade = petGrowthService.findEquippedPetGrade(childId);
        int bonusXp = calculatePetBonusXp(equippedPetGrade, wrongCount);
        int normalModeBaseXp = calculateNormalModeBaseXp(isPerfect, bonusXp);

        quizAttempt.markSubmitted(Instant.now());

        if (isReview) {
            int reviewXp = 0;
            final int reviewScore = score;
            MissionAttempt reviewAttempt = missionAttemptRepository.save(MissionAttempt.create(
                    childId,
                    missionId,
                    setId,
                    starLevel,
                    today,
                    attemptNo,
                    reviewScore,
                    TOTAL_QUESTIONS,
                    true,
                    isPassed,
                    reviewXp
            ));
            saveAnswerResults(reviewAttempt.getId(), childId, missionId, setId, true, request.answers(), answerKeysById, questionsById);
            return buildReviewResponse(
                    childId,
                    mission,
                    missionSet,
                    quizAttempt.getId(),
                    score,
                    wrongCount,
                    isPassed,
                    isPerfect,
                    equippedPetGrade,
                    bonusXp,
                    reviewXp,
                    childProfile,
                    streakRecord,
                    results
            );
        }

        if (!isPassed) {
            MissionAttempt failedAttempt = missionAttemptRepository.save(MissionAttempt.create(
                    childId,
                    missionId,
                    setId,
                    starLevel,
                    today,
                    attemptNo,
                    score,
                    TOTAL_QUESTIONS,
                    false,
                    false,
                    0
            ));
            saveAnswerResults(failedAttempt.getId(), childId, missionId, setId, false, request.answers(), answerKeysById, questionsById);
            return buildFailureResponse(childId, mission, missionSet, quizAttempt.getId(), score, wrongCount, isPerfect, childProfile, streakRecord, results);
        }

        boolean streakBonusApplied = isPartnerCompletedToday(childId, today);
        int xpEarned = calculateEarnedXp(normalModeBaseXp, streakBonusApplied);
        int previousLevel = childProfile.getLevel();
        Set<String> unlockedBeforeCompletion = unlockedIncompleteSetIds(childId, availabilityBeforeSubmit);

        childProfile.applyMissionXp(xpEarned, today, weekStart);
        childProfile.refreshProfileImageType();
        boolean firstSetCompletion = setId == null
                || missionSetProgressRepository == null
                || missionSetProgressRepository.findWithLockByChildIdAndSetId(childId, setId).isEmpty();
        final int passedScore = score;
        MissionDailyProgress todayProgress = null;
        if (firstSetCompletion) {
            todayProgress = missionDailyProgressRepository.findWithLockByChildIdAndProgressDate(childId, today)
                    .map(progress -> {
                        progress.applySetCompletion(missionId, Instant.now(), passedScore, TOTAL_QUESTIONS, xpEarned);
                        return progress;
                    })
                    .orElseGet(() -> missionDailyProgressRepository.save(MissionDailyProgress.create(
                            childId,
                            missionId,
                            today,
                            Instant.now(),
                            passedScore,
                            TOTAL_QUESTIONS,
                            xpEarned
                    )));
        } else {
            todayProgress = missionDailyProgressRepository.findByChildIdAndProgressDate(childId, today).orElse(null);
        }
        MissionAttempt passedAttempt;
        try {
            passedAttempt = missionAttemptRepository.save(MissionAttempt.create(
                    childId,
                    missionId,
                    setId,
                    starLevel,
                    today,
                    attemptNo,
                    score,
                    TOTAL_QUESTIONS,
                    false,
                    true,
                    xpEarned
            ));
            saveAnswerResults(passedAttempt.getId(), childId, missionId, setId, false, request.answers(), answerKeysById, questionsById);
            if (setId != null) {
                final Integer progressStarLevel = missionSet == null ? starLevel : missionSet.getStarLevel();
                final Integer progressVariantNo = missionSet == null ? variantNo : missionSet.getVariantNo();
                missionSetProgressRepository.findWithLockByChildIdAndSetId(childId, setId)
                        .ifPresentOrElse(
                                progress -> progress.improveBestScore(passedScore),
                                () -> missionSetProgressRepository.save(MissionSetProgress.create(
                                        childId,
                                        setId,
                                        missionId,
                                        missionSet == null ? null : Integer.valueOf(missionSet.getStage()),
                                        progressStarLevel,
                                        progressVariantNo,
                                        passedAttempt.getId(),
                                        passedScore,
                                        TOTAL_QUESTIONS
                ))
                        );
            }
        } catch (RuntimeException exception) {
            throw new AimongException(ErrorCode.SUBMIT_SAVE_FAILED, exception);
        }
        grantMissionClearGear(childProfile, passedAttempt.getId());
        StageCompletionRewardResponse stageCompletionReward = triggerStageCompletionReward(
                childProfile,
                missionSet,
                firstSetCompletion,
                passedAttempt.getId()
        );

        int currentLevel = childProfile.getLevel();
        List<SubmitResponse.RewardResponse> levelRewards = applyLevelRewards(childId, previousLevel, currentLevel, childProfile);

        dailyQuestService.updateForMissionSuccess(childId, childProfile, today);
        weeklyQuestService.updateForMissionSuccess(childId, childProfile, weekStart);
        achievementService.unlockByTotalXp(childId, childProfile);

        PetGrowthService.PetGrowthResult petGrowthResult = petGrowthService.applyMissionReward(childId, xpEarned);

        streakRecord.recordMissionCompletion(today);

        List<SubmitResponse.RewardResponse> rewards = new ArrayList<>(levelRewards);
        rewards.addAll(toRewardResponses(petGrowthResult.rewards()));
        rewards.addAll(milestoneService.applyStreakRewards(childId, streakRecord));
        Set<String> unlockedAfterCompletion = new LinkedHashSet<>(unlockedIncompleteSetIds(childId));
        unlockedAfterCompletion.removeAll(unlockedBeforeCompletion);

        return new SubmitResponse(
                MODE_NORMAL,
                true,
                ATTEMPT_STATE_SUBMITTED,
                quizAttempt.getId(),
                responseScore(score, TOTAL_QUESTIONS),
                TOTAL_QUESTIONS,
                score,
                TOTAL_QUESTIONS,
                firstSetCompletion,
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
                rewardsEnvelope(CurrencyService.MISSION_CLEAR_GEAR, xpEarned, rewards),
                toRemainingTickets(childId),
                childProfile.getProfileImageType().name(),
                childProfile.getProfileImageType() != com.aimong.backend.domain.auth.entity.ProfileImageType.DEFAULT,
                false,
                results,
                setId,
                missionId.toString(),
                starLevel,
                variantNo,
                completedSetCount(childId),
                missionSet == null ? 0 : starLevelCompletedSetCount(childId, missionSet.getStarLevel()),
                unlockedAfterCompletion.stream().toList(),
                todayProgress == null ? streakRecord.getTodayMissionCount() : todayProgress.getCompletedSetCount(),
                stageCompletionReward
        );
    }

    private SubmitResponse buildReviewResponse(
            UUID childId,
            Mission mission,
            MissionSet missionSet,
            UUID attemptId,
            int score,
            int wrongCount,
            boolean isPassed,
            boolean isPerfect,
            String equippedPetGrade,
            int bonusXp,
            int xpEarned,
            ChildProfile childProfile,
            StreakRecord streakRecord,
            List<SubmitResponse.ResultResponse> results
    ) {
        return new SubmitResponse(
                MODE_REVIEW,
                false,
                ATTEMPT_STATE_SUBMITTED,
                attemptId,
                responseScore(score, TOTAL_QUESTIONS),
                TOTAL_QUESTIONS,
                score,
                TOTAL_QUESTIONS,
                false,
                wrongCount,
                isPassed,
                isPerfect,
                isPassed ? equippedPetGrade : null,
                isPassed ? bonusXp : null,
                isPassed && bonusXp > 0 ? "PET_RARITY_BONUS" : null,
                xpEarned,
                null,
                null,
                false,
                false,
                null,
                streakRecord.getContinuousDays(),
                streakRecord.getTodayMissionCount(),
                false,
                rewardsEnvelope(0, 0, List.of()),
                toRemainingTickets(childId),
                childProfile.getProfileImageType().name(),
                childProfile.getProfileImageType() != com.aimong.backend.domain.auth.entity.ProfileImageType.DEFAULT,
                true,
                results,
                missionSet == null ? null : missionSet.getSetId(),
                mission.getId().toString(),
                missionSet == null ? null : missionSet.getStarLevel(),
                missionSet == null ? null : missionSet.getVariantNo(),
                completedSetCount(childId),
                missionSet == null ? 0 : starLevelCompletedSetCount(childId, missionSet.getStarLevel()),
                List.of(),
                todaySetCount(childId, streakRecord),
                null
        );
    }

    private SubmitResponse buildFailureResponse(
            UUID childId,
            Mission mission,
            MissionSet missionSet,
            UUID attemptId,
            int score,
            int wrongCount,
            boolean isPerfect,
            ChildProfile childProfile,
            StreakRecord streakRecord,
            List<SubmitResponse.ResultResponse> results
    ) {
        return new SubmitResponse(
                MODE_NORMAL,
                false,
                ATTEMPT_STATE_SUBMITTED,
                attemptId,
                responseScore(score, TOTAL_QUESTIONS),
                TOTAL_QUESTIONS,
                score,
                TOTAL_QUESTIONS,
                false,
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
                rewardsEnvelope(0, 0, List.of()),
                toRemainingTickets(childId),
                childProfile.getProfileImageType().name(),
                childProfile.getProfileImageType() != com.aimong.backend.domain.auth.entity.ProfileImageType.DEFAULT,
                false,
                results,
                missionSet == null ? null : missionSet.getSetId(),
                mission.getId().toString(),
                missionSet == null ? null : missionSet.getStarLevel(),
                missionSet == null ? null : missionSet.getVariantNo(),
                completedSetCount(childId),
                missionSet == null ? 0 : starLevelCompletedSetCount(childId, missionSet.getStarLevel()),
                List.of(),
                todaySetCount(childId, streakRecord),
                null
        );
    }

    private StageCompletionRewardResponse triggerStageCompletionReward(
            ChildProfile childProfile,
            MissionSet missionSet,
            boolean firstSetCompletion,
            UUID attemptId
    ) {
        if (stageCompletionRewardService == null
                || missionSet == null
                || !firstSetCompletion
                || missionSet.getStarLevel() != 1) {
            return null;
        }
        return stageCompletionRewardService.triggerIfStageCompleted(
                childProfile,
                missionSet.getStage(),
                attemptId
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

    private int responseScore(int correctCount, int total) {
        if (total <= 0) {
            return 0;
        }
        return correctCount * 100 / total;
    }

    private SubmitResponse.RewardsResponse rewardsEnvelope(int gearEarned, int xpEarned, List<SubmitResponse.RewardResponse> rewards) {
        List<SubmitResponse.FragmentResponse> fragments = rewards.stream()
                .filter(reward -> "FRAGMENT".equals(reward.type()))
                .map(reward -> new SubmitResponse.FragmentResponse(
                        reward.ticketType(),
                        reward.count() == null ? 0 : reward.count()
                ))
                .toList();
        return new SubmitResponse.RewardsResponse(gearEarned, xpEarned, fragments);
    }

    private void grantMissionClearGear(ChildProfile childProfile, UUID attemptId) {
        if (currencyService == null) {
            childProfile.addGear(CurrencyService.MISSION_CLEAR_GEAR);
            return;
        }
        currencyService.grantGear(
                childProfile,
                CurrencyService.MISSION_CLEAR_GEAR,
                CurrencyTransactionReason.MISSION_CLEAR,
                "MISSION_ATTEMPT",
                attemptId.toString()
        );
    }

    private ChildProfile findChildProfileForSubmit(UUID childId) {
        return childProfileRepository.findWithLockById(childId)
                .or(() -> childProfileRepository.findById(childId))
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
    }

    private int calculateNormalModeBaseXp(boolean isPerfect, int bonusXp) {
        return BASE_XP + (isPerfect ? PERFECT_BONUS_XP : 0) + bonusXp;
    }

    private int calculateEarnedXp(int normalModeBaseXp, boolean streakBonusApplied) {
        int xpEarned = normalModeBaseXp;
        if (!streakBonusApplied) {
            return xpEarned;
        }
        return (int) Math.floor(xpEarned * 1.5d);
    }

    private boolean isPartnerCompletedToday(UUID childId, LocalDate today) {
        return friendStreakRepository.findById(childId)
                .flatMap(friendStreak -> streakRecordRepository.findById(friendStreak.getPartnerChildId()))
                .map(partnerStreak -> today.equals(partnerStreak.getLastCompletedDate())
                        && partnerStreak.getTodayMissionCount() > 0)
                .orElse(false);
    }

    private void saveAnswerResults(
            UUID attemptId,
            UUID childId,
            UUID missionId,
            String setId,
            boolean isReview,
            List<SubmitRequest.AnswerRequest> answers,
            Map<UUID, QuestionAnswerKey> answerKeysById,
            Map<UUID, QuestionBank> questionsById
    ) {
        List<MissionAnswerResult> results = new ArrayList<>();
        for (SubmitRequest.AnswerRequest answer : answers) {
            UUID questionId = parseQuestionId(answer.questionId());
            QuestionAnswerKey answerKey = answerKeysById.get(questionId);
            QuestionBank question = questionsById.get(questionId);
            boolean isCorrect = answerKey != null
                    && question != null
                    && questionAnswerMatcher.matches(question, answerKey.getAnswerPayload(), answer.answer());
            results.add(MissionAnswerResult.create(
                    attemptId,
                    childId,
                    missionId,
                    setId,
                    questionId,
                    answer.answer(),
                    isReview,
                    isCorrect
            ));
        }
        missionAnswerResultRepository.saveAll(results);
    }

    private List<SubmitResponse.RewardResponse> applyLevelRewards(UUID childId, int previousLevel, int currentLevel, ChildProfile childProfile) {
        List<SubmitResponse.RewardResponse> rewards = new ArrayList<>();
        for (int level = previousLevel + 1; level <= currentLevel; level++) {
            if (level % 3 == 0) {
                childProfile.addShield(1);
                rewards.add(new SubmitResponse.RewardResponse(
                        "SHIELD",
                        null,
                        1,
                        null,
                        "LEVEL_REWARD_LV" + level
                ));
            }
            if (level % 5 == 0) {
                grantTickets(childId, TicketType.NORMAL, 2);
                rewards.add(new SubmitResponse.RewardResponse(
                        "TICKET",
                        "NORMAL",
                        2,
                        null,
                        "LEVEL_REWARD_LV" + level
                ));
            }
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

    private void grantTickets(UUID childId, TicketType ticketType, int count) {
        ticketRepository.saveAll(java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> Ticket.issue(childId, ticketType))
                .toList());
    }

    private long completedSetCount(UUID childId) {
        if (missionSetProgressRepository != null) {
            return missionSetProgressRepository.countByChildId(childId);
        }
        return missionAttemptRepository.countCompletedMission(childId);
    }

    private long starLevelCompletedSetCount(UUID childId, int starLevel) {
        if (missionSetRepository == null || missionSetProgressRepository == null) {
            return 0;
        }
        return missionSetProgressRepository.countByChildIdAndStarLevelAndCompletedTrue(childId, starLevel);
    }

    private Set<String> unlockedIncompleteSetIds(UUID childId) {
        return unlockedIncompleteSetIds(childId, null);
    }

    private Set<String> unlockedIncompleteSetIds(UUID childId, MissionService.MissionSetAvailability availability) {
        if (missionSetRepository == null || missionSetProgressRepository == null) {
            return Set.of();
        }
        MissionService.MissionSetAvailability resolvedAvailability =
                availability == null ? missionService.missionSetAvailability(childId) : availability;
        return resolvedAvailability.playableSets()
                .stream()
                .filter(set -> !resolvedAvailability.progressBySetId().containsKey(set.getSetId()))
                .map(MissionSet::getSetId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private int todaySetCount(UUID childId, StreakRecord streakRecord) {
        return missionDailyProgressRepository.findByChildIdAndProgressDate(childId, KstDateUtils.today())
                .map(MissionDailyProgress::getCompletedSetCount)
                .orElse(streakRecord.getTodayMissionCount());
    }

    private SubmitResponse.RemainingTicketsResponse toRemainingTickets(UUID childId) {
        return new SubmitResponse.RemainingTicketsResponse(
                Math.toIntExact(ticketRepository.countByChildIdAndTicketTypeAndUsedAtIsNull(childId, TicketType.NORMAL))
        );
    }
}
