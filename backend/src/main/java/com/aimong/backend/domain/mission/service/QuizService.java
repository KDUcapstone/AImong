package com.aimong.backend.domain.mission.service;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.mission.dto.MissionQuestionsResponse;
import com.aimong.backend.domain.mission.dto.QuestionResponse;
import com.aimong.backend.domain.mission.dto.StageProgressResponse;
import com.aimong.backend.domain.mission.config.MissionQuestionProperties;
import com.aimong.backend.domain.mission.entity.Mission;
import com.aimong.backend.domain.mission.entity.MissionSet;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.entity.QuizAttempt;
import com.aimong.backend.domain.mission.repository.MissionDailyProgressRepository;
import com.aimong.backend.domain.mission.repository.MissionRepository;
import com.aimong.backend.domain.mission.repository.MissionSetProgressRepository;
import com.aimong.backend.domain.mission.repository.MissionSetRepository;
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
import java.util.stream.IntStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuizService {

    private static final Logger log = LoggerFactory.getLogger(QuizService.class);

    private final MissionRepository missionRepository;
    private final MissionSetRepository missionSetRepository;
    private final MissionSetProgressRepository missionSetProgressRepository;
    private final ChildProfileRepository childProfileRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final MissionDailyProgressRepository missionDailyProgressRepository;
    private final ChildActivityService childActivityService;
    private final MissionService missionService;
    private final MissionQuestionSetFactory missionQuestionSetFactory;
    private final MissionQuestionProperties missionQuestionProperties;
    private final QuestionTermHintService questionTermHintService;
    private final ObjectMapper objectMapper;

    @Autowired
    public QuizService(
            MissionRepository missionRepository,
            MissionSetRepository missionSetRepository,
            MissionSetProgressRepository missionSetProgressRepository,
            ChildProfileRepository childProfileRepository,
            QuizAttemptRepository quizAttemptRepository,
            MissionDailyProgressRepository missionDailyProgressRepository,
            ChildActivityService childActivityService,
            MissionService missionService,
            MissionQuestionSetFactory missionQuestionSetFactory,
            MissionQuestionProperties missionQuestionProperties,
            QuestionTermHintService questionTermHintService,
            ObjectMapper objectMapper
    ) {
        this.missionRepository = missionRepository;
        this.missionSetRepository = missionSetRepository;
        this.missionSetProgressRepository = missionSetProgressRepository;
        this.childProfileRepository = childProfileRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.missionDailyProgressRepository = missionDailyProgressRepository;
        this.childActivityService = childActivityService;
        this.missionService = missionService;
        this.missionQuestionSetFactory = missionQuestionSetFactory;
        this.missionQuestionProperties = missionQuestionProperties;
        this.questionTermHintService = questionTermHintService;
        this.objectMapper = objectMapper;
    }

    public QuizService(
            MissionRepository missionRepository,
            QuizAttemptRepository quizAttemptRepository,
            MissionDailyProgressRepository missionDailyProgressRepository,
            ChildActivityService childActivityService,
            MissionService missionService,
            MissionQuestionSetFactory missionQuestionSetFactory,
            MissionQuestionProperties missionQuestionProperties,
            ObjectMapper objectMapper
    ) {
        this.missionRepository = missionRepository;
        this.missionSetRepository = null;
        this.missionSetProgressRepository = null;
        this.childProfileRepository = null;
        this.quizAttemptRepository = quizAttemptRepository;
        this.missionDailyProgressRepository = missionDailyProgressRepository;
        this.childActivityService = childActivityService;
        this.missionService = missionService;
        this.missionQuestionSetFactory = missionQuestionSetFactory;
        this.missionQuestionProperties = missionQuestionProperties;
        this.questionTermHintService = new QuestionTermHintService();
        this.objectMapper = objectMapper;
    }

    @Transactional
    public MissionQuestionsResponse getQuestions(UUID childId, UUID missionId) {
        return getQuestions(childId, missionId, 1);
    }

    @Transactional
    public MissionQuestionsResponse getQuestions(UUID childId, UUID missionId, int starLevel) {
        long startedAt = System.nanoTime();
        childActivityService.touchLastActiveAt(childId);
        long activityTouchedAt = System.nanoTime();
        if (starLevel < 1 || starLevel > 3) {
            throw new AimongException(ErrorCode.INVALID_STAR_LEVEL);
        }
        Mission mission = missionRepository.findById(missionId)
                .filter(Mission::isActive)
                .orElseThrow(() -> new AimongException(ErrorCode.MISSION_NOT_FOUND));
        long missionLoadedAt = System.nanoTime();
        if (missionSetRepository == null || missionSetProgressRepository == null) {
            MissionQuestionsResponse response = getLegacyQuestions(childId, mission);
            log.info(
                    "mission-questions served mode=legacy childId={} missionId={} starLevel={} touchMs={} missionLoadMs={} totalMs={}",
                    childId,
                    missionId,
                    starLevel,
                    elapsedMillis(startedAt, activityTouchedAt),
                    elapsedMillis(activityTouchedAt, missionLoadedAt),
                    elapsedMillis(startedAt, System.nanoTime())
            );
            return response;
        }
        MissionSet missionSet = missionService.resolvePlayableSet(childId, mission.getId(), starLevel);
        long setResolvedAt = System.nanoTime();
        MissionQuestionsResponse response = getQuestionsForSet(childId, missionSet);
        log.info(
                "mission-questions served mode=mission-set childId={} missionId={} setId={} starLevel={} touchMs={} missionLoadMs={} setResolveMs={} totalMs={}",
                childId,
                missionId,
                missionSet.getSetId(),
                starLevel,
                elapsedMillis(startedAt, activityTouchedAt),
                elapsedMillis(activityTouchedAt, missionLoadedAt),
                elapsedMillis(missionLoadedAt, setResolvedAt),
                elapsedMillis(startedAt, System.nanoTime())
        );
        return response;
    }

    @Transactional
    public MissionQuestionsResponse getQuestions(UUID childId, String setId) {
        childActivityService.touchLastActiveAt(childId);
        MissionSet missionSet = missionSetRepository.findBySetIdAndActiveTrue(setId)
                .orElseThrow(() -> new AimongException(ErrorCode.MISSION_SET_NOT_FOUND));
        if (!missionService.isStarLevelPlayable(childId, missionSet.getMissionId(), missionSet.getStarLevel())) {
            throw new AimongException(ErrorCode.MISSION_SET_LOCKED);
        }
        return getQuestionsForSet(childId, missionSet);
    }

    private MissionQuestionsResponse getQuestionsForSet(UUID childId, MissionSet missionSet) {
        long startedAt = System.nanoTime();
        Mission mission = missionRepository.findById(missionSet.getMissionId())
                .filter(Mission::isActive)
                .orElseThrow(() -> new AimongException(ErrorCode.MISSION_NOT_FOUND));
        long missionLoadedAt = System.nanoTime();
        boolean isReview = missionSetProgressRepository.existsByChildIdAndSetId(childId, missionSet.getSetId());
        long progressCheckedAt = System.nanoTime();
        List<QuestionBank> selectedQuestions = createServingReadyQuestionSet(missionSet, mission, childId, isReview);
        long questionsSelectedAt = System.nanoTime();
        if (selectedQuestions.size() != missionQuestionProperties.setSize()) {
            throw new AimongException(ErrorCode.MISSION_SET_NOT_READY);
        }

        int energyCost = isReview ? 0 : ChildProfile.MISSION_ENERGY_COST;
        Integer energyBefore = null;
        Integer energyAfter = null;
        if (!isReview) {
            ChildProfile childProfile = childProfileRepository.findWithLockById(childId)
                    .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
            childProfile.recoverEnergy(Instant.now());
            energyBefore = childProfile.getEnergy();
            if (!childProfile.consumeMissionEnergy(Instant.now())) {
                throw new AimongException(ErrorCode.INSUFFICIENT_ENERGY);
            }
            energyAfter = childProfile.getEnergy();
        }
        long energyCheckedAt = System.nanoTime();

        List<UUID> selectedQuestionIds = selectedQuestions.stream()
                .map(QuestionBank::getId)
                .toList();

        QuizAttempt quizAttempt = QuizAttempt.create(
                childId,
                missionSet.getMissionId(),
                missionSet.getSetId(),
                missionSet.getStarLevel(),
                writeQuestionIds(selectedQuestionIds),
                Instant.now().plus(missionQuestionProperties.attemptTtlMinutes(), ChronoUnit.MINUTES),
                isReview
        );
        quizAttemptRepository.save(quizAttempt);
        long attemptSavedAt = System.nanoTime();

        MissionQuestionsResponse response = new MissionQuestionsResponse(
                missionSet.getSetId(),
                mission.getId(),
                missionSet.getMissionCode(),
                missionSet.getStarLevel(),
                missionSet.getVariantNo(),
                missionSet.starLabel(),
                isReview,
                energyCost,
                energyBefore,
                energyAfter,
                quizAttempt.getId(),
                missionQuestionProperties.setSize(),
                toQuestionResponses(selectedQuestions)
        );
        log.info(
                "mission-question-set served childId={} missionId={} setId={} starLevel={} review={} selected={} missionLoadMs={} progressMs={} selectMs={} energyMs={} attemptSaveMs={} responseMapMs={} totalMs={}",
                childId,
                mission.getId(),
                missionSet.getSetId(),
                missionSet.getStarLevel(),
                isReview,
                selectedQuestions.size(),
                elapsedMillis(startedAt, missionLoadedAt),
                elapsedMillis(missionLoadedAt, progressCheckedAt),
                elapsedMillis(progressCheckedAt, questionsSelectedAt),
                elapsedMillis(questionsSelectedAt, energyCheckedAt),
                elapsedMillis(energyCheckedAt, attemptSavedAt),
                elapsedMillis(attemptSavedAt, System.nanoTime()),
                elapsedMillis(startedAt, System.nanoTime())
        );
        return response;
    }

    private MissionQuestionsResponse getLegacyQuestions(UUID childId, Mission mission) {
        StageProgressResponse stageProgress = missionService.stageProgressForLegacy(childId);
        if (!missionService.isUnlockedForChild(childId, mission, stageProgress)) {
            throw new AimongException(ErrorCode.MISSION_QUESTIONS_LOCKED);
        }
        boolean isReview = missionDailyProgressRepository.findByChildIdAndMissionIdAndProgressDate(
                childId,
                mission.getId(),
                KstDateUtils.today()
        ).isPresent();
        List<QuestionBank> selectedQuestions = missionQuestionSetFactory.create(mission.getId(), childId, isReview);
        ensureQuestionSetReady(selectedQuestions);
        QuizAttempt quizAttempt = QuizAttempt.create(
                childId,
                mission.getId(),
                null,
                1,
                writeQuestionIds(selectedQuestions.stream().map(QuestionBank::getId).toList()),
                Instant.now().plus(missionQuestionProperties.attemptTtlMinutes(), ChronoUnit.MINUTES),
                isReview
        );
        quizAttemptRepository.save(quizAttempt);
        return new MissionQuestionsResponse(
                null,
                mission.getId(),
                null,
                1,
                1,
                MissionQuestionsResponse.labelForStar(1),
                isReview,
                0,
                null,
                null,
                quizAttempt.getId(),
                missionQuestionProperties.setSize(),
                toQuestionResponses(selectedQuestions)
        );
    }

    private List<QuestionBank> createServingReadyQuestionSet(MissionSet missionSet, Mission mission, UUID childId, boolean isReview) {
        List<QuestionBank> selectedQuestions = missionQuestionSetFactory.create(
                missionSet.getSetId(),
                mission.getId(),
                missionSet.getStarLevel(),
                childId,
                isReview
        );
        ensureQuestionSetReady(selectedQuestions);
        return selectedQuestions;
    }

    private void ensureQuestionSetReady(List<QuestionBank> selectedQuestions) {
        if (selectedQuestions.size() != missionQuestionProperties.setSize()) {
            throw new AimongException(ErrorCode.MISSION_SET_NOT_READY);
        }
    }

    private long elapsedMillis(long fromNanos, long toNanos) {
        return (toNanos - fromNanos) / 1_000_000;
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

    private List<QuestionResponse> toQuestionResponses(List<QuestionBank> questions) {
        return IntStream.range(0, questions.size())
                .mapToObj(index -> toQuestionResponse(questions.get(index), index + 1))
                .toList();
    }

    private QuestionResponse toQuestionResponse(QuestionBank question, int questionNo) {
        List<String> choices = readOptions(question.getOptionsJson());
        return new QuestionResponse(
                question.getId(),
                questionNo,
                question.getQuestionType().name(),
                question.getDifficulty() == null ? null : question.getDifficulty().name(),
                question.getPrompt(),
                choices,
                QuestionResponse.answerFormatFor(choices),
                questionTermHintService.findHints(question.getPrompt(), choices)
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
