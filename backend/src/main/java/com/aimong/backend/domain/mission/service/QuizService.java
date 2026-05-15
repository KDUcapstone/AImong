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
import java.util.stream.IntStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuizService {

    private final MissionRepository missionRepository;
    private final MissionSetRepository missionSetRepository;
    private final MissionSetProgressRepository missionSetProgressRepository;
    private final ChildProfileRepository childProfileRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final MissionDailyProgressRepository missionDailyProgressRepository;
    private final ChildActivityService childActivityService;
    private final MissionService missionService;
    private final MissionQuestionSetFactory missionQuestionSetFactory;
    private final QuestionServingQualityGuard questionServingQualityGuard;
    private final MissionQuestionProperties missionQuestionProperties;
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
            QuestionServingQualityGuard questionServingQualityGuard,
            MissionQuestionProperties missionQuestionProperties,
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
        this.questionServingQualityGuard = questionServingQualityGuard;
        this.missionQuestionProperties = missionQuestionProperties;
        this.objectMapper = objectMapper;
    }

    public QuizService(
            MissionRepository missionRepository,
            QuizAttemptRepository quizAttemptRepository,
            MissionDailyProgressRepository missionDailyProgressRepository,
            ChildActivityService childActivityService,
            MissionService missionService,
            MissionQuestionSetFactory missionQuestionSetFactory,
            QuestionServingQualityGuard questionServingQualityGuard,
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
        this.questionServingQualityGuard = questionServingQualityGuard;
        this.missionQuestionProperties = missionQuestionProperties;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public MissionQuestionsResponse getQuestions(UUID childId, UUID missionId) {
        return getQuestions(childId, missionId, 1);
    }

    @Transactional
    public MissionQuestionsResponse getQuestions(UUID childId, UUID missionId, int starLevel) {
        childActivityService.touchLastActiveAt(childId);
        if (starLevel < 1 || starLevel > 3) {
            throw new AimongException(ErrorCode.INVALID_STAR_LEVEL);
        }
        Mission mission = missionRepository.findById(missionId)
                .filter(Mission::isActive)
                .orElseThrow(() -> new AimongException(ErrorCode.MISSION_NOT_FOUND));
        if (missionSetRepository == null || missionSetProgressRepository == null) {
            return getLegacyQuestions(childId, mission);
        }
        MissionSet missionSet = missionService.resolvePlayableSet(childId, mission.getId(), starLevel);
        return getQuestionsForSet(childId, missionSet);
    }

    @Transactional
    public MissionQuestionsResponse getQuestions(UUID childId, String setId) {
        childActivityService.touchLastActiveAt(childId);
        MissionSet missionSet = missionSetRepository.findBySetIdAndActiveTrue(setId)
                .orElseThrow(() -> new AimongException(ErrorCode.MISSION_SET_NOT_FOUND));
        if (!missionService.isUnlocked(childId, missionSet)) {
            throw new AimongException(ErrorCode.MISSION_SET_LOCKED);
        }
        return getQuestionsForSet(childId, missionSet);
    }

    private MissionQuestionsResponse getQuestionsForSet(UUID childId, MissionSet missionSet) {
        Mission mission = missionRepository.findById(missionSet.getMissionId())
                .filter(Mission::isActive)
                .orElseThrow(() -> new AimongException(ErrorCode.MISSION_NOT_FOUND));
        boolean isReview = missionSetProgressRepository.existsByChildIdAndSetId(childId, missionSet.getSetId());
        List<QuestionBank> selectedQuestions = createServingReadyQuestionSet(missionSet, mission, childId, isReview);
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

        return new MissionQuestionsResponse(
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
        if (missionQuestionProperties.servingAutoQuarantineEnabled()) {
            QuestionServingQualityGuard.ServingValidationResult validationResult =
                    questionServingQualityGuard.validateForServing(mission, selectedQuestions);
            if (validationResult.validQuestions().size() != missionQuestionProperties.setSize()) {
                throw new AimongException(ErrorCode.MISSION_SET_NOT_READY);
            }
            selectedQuestions = validationResult.validQuestions();
        }
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
        if (!missionQuestionProperties.servingAutoQuarantineEnabled()) {
            return missionQuestionSetFactory.create(missionSet.getSetId(), mission.getId(), missionSet.getStarLevel(), childId, isReview);
        }

        for (int attempt = 0; attempt < 2; attempt++) {
            List<QuestionBank> selectedQuestions = missionQuestionSetFactory.create(
                    missionSet.getSetId(),
                    mission.getId(),
                    missionSet.getStarLevel(),
                    childId,
                    isReview
            );
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
                QuestionResponse.answerFormatFor(choices)
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
