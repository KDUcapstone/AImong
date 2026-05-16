package com.aimong.backend.domain.mission.service.question;

import com.aimong.backend.domain.mission.config.MissionQuestionProperties;
import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.domain.mission.repository.MissionAnswerResultRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MissionQuestionSetFactory {

    private final ApprovedQuestionProvider approvedQuestionProvider;
    private final MissionQuestionProperties missionQuestionProperties;
    private final MissionAnswerResultRepository missionAnswerResultRepository;

    @Autowired
    public MissionQuestionSetFactory(
            ApprovedQuestionProvider approvedQuestionProvider,
            MissionQuestionProperties missionQuestionProperties,
            MissionAnswerResultRepository missionAnswerResultRepository
    ) {
        this.approvedQuestionProvider = approvedQuestionProvider;
        this.missionQuestionProperties = missionQuestionProperties;
        this.missionAnswerResultRepository = missionAnswerResultRepository;
    }

    public MissionQuestionSetFactory(
            ApprovedQuestionProvider approvedQuestionProvider,
            MissionQuestionProperties missionQuestionProperties
    ) {
        this.approvedQuestionProvider = approvedQuestionProvider;
        this.missionQuestionProperties = missionQuestionProperties;
        this.missionAnswerResultRepository = null;
    }

    public List<QuestionBank> create(String setId, UUID missionId, UUID childId, boolean isReview) {
        return create(setId, missionId, 1, childId, isReview);
    }

    public List<QuestionBank> create(String setId, UUID missionId, int starLevel, UUID childId, boolean isReview) {
        return create(missionId, childId, starLevel, isReview);
    }

    public List<QuestionBank> create(UUID missionId, UUID childId, boolean isReview) {
        return create(missionId, childId, 1, isReview);
    }

    public List<QuestionBank> create(UUID missionId, UUID childId, int starLevel, boolean isReview) {
        List<QuestionBank> lowPool = approvedQuestionProvider.findActiveQuestionsByMissionIdAndDifficulty(missionId, DifficultyBand.LOW);
        List<QuestionBank> mediumPool = approvedQuestionProvider.findActiveQuestionsByMissionIdAndDifficulty(missionId, DifficultyBand.MEDIUM);
        List<QuestionBank> highPool = approvedQuestionProvider.findActiveQuestionsByMissionIdAndDifficulty(missionId, DifficultyBand.HIGH);
        return selectQuestionSet(missionId, childId, starLevel, lowPool, mediumPool, highPool);
    }

    private List<QuestionBank> selectQuestionSet(
            UUID missionId,
            UUID childId,
            int starLevel,
            List<QuestionBank> lowPool,
            List<QuestionBank> mediumPool,
            List<QuestionBank> highPool
    ) {
        DifficultyQuota quota = DifficultyQuota.forStarLevel(starLevel);
        Set<UUID> attemptedQuestionIds = attemptedQuestionIds(childId, missionId);
        Set<String> selectedPromptKeys = new HashSet<>();
        List<QuestionBank> selected = new java.util.ArrayList<>(missionQuestionProperties.setSize());
        selected.addAll(selectByQuota(lowPool, attemptedQuestionIds, selectedPromptKeys, quota.low()));
        selected.addAll(selectByQuota(mediumPool, attemptedQuestionIds, selectedPromptKeys, quota.medium()));
        selected.addAll(selectByQuota(highPool, attemptedQuestionIds, selectedPromptKeys, quota.high()));
        return shuffleFinalSet(selected);
    }

    private Set<UUID> attemptedQuestionIds(UUID childId, UUID missionId) {
        if (missionAnswerResultRepository == null) {
            return new HashSet<>();
        }
        return new HashSet<>(missionAnswerResultRepository.findNormalAttemptedQuestionIds(childId, missionId));
    }

    private List<QuestionBank> selectByQuota(
            List<QuestionBank> pool,
            Set<UUID> attemptedQuestionIds,
            Set<String> selectedPromptKeys,
            int quota
    ) {
        List<QuestionBank> available = pool.stream()
                .filter(question -> !selectedPromptKeys.contains(promptKey(question)))
                .toList();
        if (available.size() < quota) {
            throw new AimongException(ErrorCode.MISSION_SET_NOT_READY);
        }
        List<QuestionBank> unattempted = available.stream()
                .filter(question -> !attemptedQuestionIds.contains(question.getId()))
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        List<QuestionBank> attempted = available.stream()
                .filter(question -> attemptedQuestionIds.contains(question.getId()))
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));

        Collections.shuffle(unattempted);
        Collections.shuffle(attempted);

        List<QuestionBank> selected = new java.util.ArrayList<>(quota);
        addUniquePrompts(selected, selectedPromptKeys, unattempted, quota);
        addUniquePrompts(selected, selectedPromptKeys, attempted, quota);
        if (selected.size() != quota) {
            throw new AimongException(ErrorCode.MISSION_SET_NOT_READY);
        }
        return selected;
    }

    private void addUniquePrompts(
            List<QuestionBank> selected,
            Set<String> selectedPromptKeys,
            List<QuestionBank> candidates,
            int quota
    ) {
        for (QuestionBank candidate : candidates) {
            if (selected.size() == quota) {
                return;
            }
            String promptKey = promptKey(candidate);
            if (selectedPromptKeys.add(promptKey)) {
                selected.add(candidate);
            }
        }
    }

    private String promptKey(QuestionBank question) {
        String prompt = question.getPrompt();
        if (prompt == null || prompt.isBlank()) {
            return "question:" + question.getId();
        }
        return prompt.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private List<QuestionBank> shuffleFinalSet(List<QuestionBank> questionSet) {
        List<QuestionBank> shuffled = new java.util.ArrayList<>(questionSet);
        Collections.shuffle(shuffled);
        if (shuffled.size() != missionQuestionProperties.setSize()) {
            throw new AimongException(ErrorCode.MISSION_SET_NOT_READY);
        }
        return List.copyOf(shuffled);
    }

    private record DifficultyQuota(int low, int medium, int high) {
        private static DifficultyQuota forStarLevel(int starLevel) {
            return switch (starLevel) {
                case 1 -> new DifficultyQuota(7, 2, 1);
                case 2 -> new DifficultyQuota(3, 5, 2);
                case 3 -> new DifficultyQuota(2, 3, 5);
                default -> throw new AimongException(ErrorCode.INVALID_STAR_LEVEL);
            };
        }
    }
}
