package com.aimong.backend.domain.mission.service.question;

import com.aimong.backend.domain.mission.config.MissionQuestionProperties;
import com.aimong.backend.domain.mission.entity.DifficultyBand;
import com.aimong.backend.domain.mission.entity.QuestionBank;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MissionQuestionSetFactory {

    private static final Pattern SET_PACK_NO_PATTERN = Pattern.compile(".*-L(\\d+)$");

    private final ApprovedQuestionProvider approvedQuestionProvider;
    private final MissionQuestionProperties missionQuestionProperties;
    private final SimilarityDeduplicator similarityDeduplicator;

    @Autowired
    public MissionQuestionSetFactory(
            ApprovedQuestionProvider approvedQuestionProvider,
            MissionQuestionProperties missionQuestionProperties,
            SimilarityDeduplicator similarityDeduplicator
    ) {
        this.approvedQuestionProvider = approvedQuestionProvider;
        this.missionQuestionProperties = missionQuestionProperties;
        this.similarityDeduplicator = similarityDeduplicator;
    }

    public MissionQuestionSetFactory(
            ApprovedQuestionProvider approvedQuestionProvider,
            MissionQuestionProperties missionQuestionProperties
    ) {
        this.approvedQuestionProvider = approvedQuestionProvider;
        this.missionQuestionProperties = missionQuestionProperties;
        this.similarityDeduplicator = new SimilarityDeduplicator();
    }

    public List<QuestionBank> create(String setId, UUID missionId, UUID childId, boolean isReview) {
        return create(setId, missionId, 1, childId, isReview);
    }

    public List<QuestionBank> create(String setId, UUID missionId, int starLevel, UUID childId, boolean isReview) {
        validateStarLevel(starLevel);

        List<QuestionBank> setPool = safeList(approvedQuestionProvider.findActiveQuestionsBySetIdAndMissionId(setId, missionId));
        if (!setPool.isEmpty()) {
            return selectFixedQuestionSet(setPool);
        }

        Short packNo = packNoFromSetId(setId);
        if (packNo != null && packNo == starLevel) {
            List<QuestionBank> packPool = safeList(approvedQuestionProvider.findActiveQuestionsByMissionIdAndPackNo(missionId, packNo));
            if (!packPool.isEmpty()) {
                return selectFixedQuestionSet(packPool);
            }
        }

        return create(missionId, childId, starLevel, isReview);
    }

    public List<QuestionBank> create(UUID missionId, UUID childId, boolean isReview) {
        return create(missionId, childId, 1, isReview);
    }

    public List<QuestionBank> create(UUID missionId, UUID childId, int starLevel, boolean isReview) {
        DifficultyBand difficulty = difficultyForStarLevel(starLevel);
        List<QuestionBank> pool = safeList(approvedQuestionProvider.findActiveQuestionsByMissionIdAndDifficulty(missionId, difficulty));
        return selectFixedQuestionSet(pool);
    }

    private List<QuestionBank> selectFixedQuestionSet(List<QuestionBank> candidates) {
        Set<String> selectedPromptKeys = new HashSet<>();
        List<QuestionBank> selected = new java.util.ArrayList<>(missionQuestionProperties.setSize());
        addUniquePrompts(selected, selectedPromptKeys, candidates, missionQuestionProperties.setSize());
        if (selected.size() != missionQuestionProperties.setSize()) {
            throw new AimongException(ErrorCode.MISSION_SET_NOT_READY);
        }
        return shuffleWithinDifficultyBands(selected);
    }

    private List<QuestionBank> safeList(List<QuestionBank> questions) {
        return questions == null ? List.of() : questions;
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
            if (isPromptOriginal(candidate, selectedPromptKeys) && selectedPromptKeys.add(promptKey)) {
                selected.add(candidate);
            }
        }
    }

    private boolean isPromptOriginal(QuestionBank question, Set<String> selectedPromptKeys) {
        String promptKey = promptKey(question);
        if (selectedPromptKeys.contains(promptKey)) {
            return false;
        }
        if (!containsHangul(promptKey) || promptKey.length() < 10) {
            return true;
        }
        return selectedPromptKeys.stream()
                .filter(selectedKey -> containsHangul(selectedKey) && selectedKey.length() >= 10)
                .noneMatch(selectedKey -> similarityDeduplicator.isDuplicateOrNearDuplicate(promptKey, selectedKey));
    }

    private boolean containsHangul(String value) {
        return value != null && value.codePoints()
                .anyMatch(codePoint -> codePoint >= 0xAC00 && codePoint <= 0xD7A3);
    }

    private String promptKey(QuestionBank question) {
        String prompt = question.getPrompt();
        if (prompt == null || prompt.isBlank()) {
            return "question:" + question.getId();
        }
        return similarityDeduplicator.nearDuplicateKey(prompt);
    }

    private List<QuestionBank> shuffleWithinDifficultyBands(List<QuestionBank> questionSet) {
        if (questionSet.size() != missionQuestionProperties.setSize()) {
            throw new AimongException(ErrorCode.MISSION_SET_NOT_READY);
        }
        List<QuestionBank> ordered = new java.util.ArrayList<>(questionSet.size());
        for (DifficultyBand difficulty : DifficultyBand.values()) {
            List<QuestionBank> bandQuestions = questionSet.stream()
                    .filter(question -> question.getDifficulty() == difficulty)
                    .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
            Collections.shuffle(bandQuestions);
            ordered.addAll(bandQuestions);
        }
        if (ordered.size() != questionSet.size()) {
            throw new AimongException(ErrorCode.MISSION_SET_NOT_READY);
        }
        return List.copyOf(ordered);
    }

    private DifficultyBand difficultyForStarLevel(int starLevel) {
        return switch (starLevel) {
            case 1 -> DifficultyBand.LOW;
            case 2 -> DifficultyBand.MEDIUM;
            case 3 -> DifficultyBand.HIGH;
            default -> throw new AimongException(ErrorCode.INVALID_STAR_LEVEL);
        };
    }

    private void validateStarLevel(int starLevel) {
        difficultyForStarLevel(starLevel);
    }

    private Short packNoFromSetId(String setId) {
        if (setId == null || setId.isBlank()) {
            return null;
        }
        Matcher matcher = SET_PACK_NO_PATTERN.matcher(setId);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return Short.valueOf(matcher.group(1));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
