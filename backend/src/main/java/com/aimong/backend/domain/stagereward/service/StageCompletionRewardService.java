package com.aimong.backend.domain.stagereward.service;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.gacha.entity.Ticket;
import com.aimong.backend.domain.gacha.entity.TicketType;
import com.aimong.backend.domain.gacha.repository.TicketRepository;
import com.aimong.backend.domain.mission.repository.MissionSetProgressRepository;
import com.aimong.backend.domain.mission.repository.MissionSetRepository;
import com.aimong.backend.domain.reward.entity.CurrencyTransactionReason;
import com.aimong.backend.domain.reward.service.CurrencyService;
import com.aimong.backend.domain.stagereward.dto.CreateStageRewardRequest;
import com.aimong.backend.domain.stagereward.dto.StageCompletionRewardResponse;
import com.aimong.backend.domain.stagereward.dto.StageRewardListResponse;
import com.aimong.backend.domain.stagereward.dto.StageRewardResponse;
import com.aimong.backend.domain.stagereward.dto.UpdateStageRewardRequest;
import com.aimong.backend.domain.stagereward.entity.StageCompletionReward;
import com.aimong.backend.domain.stagereward.repository.StageCompletionRewardRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StageCompletionRewardService {

    private static final String REF_TYPE_STAGE_COMPLETION = "STAGE_COMPLETION";

    private final ChildProfileRepository childProfileRepository;
    private final StageCompletionRewardRepository stageCompletionRewardRepository;
    private final MissionSetRepository missionSetRepository;
    private final MissionSetProgressRepository missionSetProgressRepository;
    private final CurrencyService currencyService;
    private final TicketRepository ticketRepository;

    @Transactional
    public StageRewardResponse createStageReward(String parentId, UUID childId, CreateStageRewardRequest request) {
        int stageNumber = requireStageNumber(request.stageNumber());
        ChildProfile childProfile = validateOwnership(parentId, childId);
        StageCompletionReward reward = stageCompletionRewardRepository
                .findWithLockByChildProfileIdAndStageNumber(childId, stageNumber)
                .orElseGet(() -> stageCompletionRewardRepository.save(StageCompletionReward.create(
                        childProfile.getParentAccount(),
                        childProfile,
                        stageNumber,
                        null
                )));
        if (reward.isTriggered()) {
            throw new AimongException(ErrorCode.ALREADY_TRIGGERED);
        }
        if (reward.getRewardText() != null) {
            throw new AimongException(ErrorCode.STAGE_REWARD_ALREADY_SET);
        }
        reward.updateRewardText(request.rewardText());
        return toResponse(reward);
    }

    @Transactional
    public StageRewardResponse updateStageReward(
            String parentId,
            UUID childId,
            int stageNumber,
            UpdateStageRewardRequest request
    ) {
        requireStageNumber(stageNumber);
        ChildProfile childProfile = validateOwnership(parentId, childId);
        StageCompletionReward reward = stageCompletionRewardRepository
                .findWithLockByChildProfileIdAndStageNumber(childId, stageNumber)
                .orElseGet(() -> stageCompletionRewardRepository.save(StageCompletionReward.create(
                        childProfile.getParentAccount(),
                        childProfile,
                        stageNumber,
                        null
                )));
        if (reward.isTriggered()) {
            throw new AimongException(ErrorCode.ALREADY_TRIGGERED);
        }
        reward.updateRewardText(request.rewardText());
        return toResponse(reward);
    }

    @Transactional
    public StageRewardListResponse getStageRewards(String parentId, UUID childId) {
        ChildProfile childProfile = validateOwnership(parentId, childId);
        Map<Integer, StageCompletionReward> rewardsByStage = ensureStageRewards(childProfile).stream()
                .collect(Collectors.toMap(StageCompletionReward::getStageNumber, Function.identity()));
        return new StageRewardListResponse(IntStream.rangeClosed(1, 3)
                .mapToObj(stage -> toResponse(rewardsByStage.get(stage)))
                .toList());
    }

    @Transactional
    public StageCompletionRewardResponse triggerIfStageCompleted(
            ChildProfile childProfile,
            int stageNumber,
            UUID attemptId
    ) {
        requireStageNumber(stageNumber);
        long total = totalMissionCount(stageNumber);
        if (total <= 0 || completedMissionCount(childProfile.getId(), stageNumber) < total) {
            return null;
        }

        StageCompletionReward reward = stageCompletionRewardRepository
                .findWithLockByChildProfileIdAndStageNumber(childProfile.getId(), stageNumber)
                .orElseGet(() -> stageCompletionRewardRepository.save(StageCompletionReward.create(
                        childProfile.getParentAccount(),
                        childProfile,
                        stageNumber,
                        null
                )));
        if (reward.isTriggered()) {
            return null;
        }

        int gearReward = defaultGearReward(stageNumber);
        int ticketReward = normalTicketReward(stageNumber);
        currencyService.grantGear(
                childProfile,
                gearReward,
                CurrencyTransactionReason.STAGE_REWARD_GEAR,
                REF_TYPE_STAGE_COMPLETION,
                attemptId.toString()
        );
        if (ticketReward > 0) {
            ticketRepository.saveAll(IntStream.range(0, ticketReward)
                    .mapToObj(index -> Ticket.issue(childProfile.getId(), TicketType.NORMAL))
                    .toList());
        }

        Instant now = Instant.now();
        reward.markTriggered(now);
        return new StageCompletionRewardResponse(
                stageNumber,
                reward.getRewardText(),
                gearReward,
                ticketReward,
                now
        );
    }

    private List<StageCompletionReward> ensureStageRewards(ChildProfile childProfile) {
        Map<Integer, StageCompletionReward> existing = stageCompletionRewardRepository
                .findAllByChildProfileIdOrderByStageNumberAsc(childProfile.getId())
                .stream()
                .collect(Collectors.toMap(StageCompletionReward::getStageNumber, Function.identity()));
        for (int stage = 1; stage <= 3; stage++) {
            existing.computeIfAbsent(stage, missingStage -> stageCompletionRewardRepository.save(StageCompletionReward.create(
                    childProfile.getParentAccount(),
                    childProfile,
                    missingStage,
                    null
            )));
        }
        return existing.values().stream()
                .sorted(java.util.Comparator.comparingInt(StageCompletionReward::getStageNumber))
                .toList();
    }

    private ChildProfile validateOwnership(String parentId, UUID childId) {
        ChildProfile childProfile = childProfileRepository.findByIdAndDeletedAtIsNull(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
        if (!childProfile.getParentAccount().getParentId().equals(parentId)) {
            throw new AimongException(ErrorCode.CHILD_NOT_FOUND);
        }
        return childProfile;
    }

    private StageRewardResponse toResponse(StageCompletionReward reward) {
        int stageNumber = reward.getStageNumber();
        return new StageRewardResponse(
                reward.getId(),
                stageNumber,
                reward.getRewardText(),
                reward.isTriggered(),
                reward.getTriggeredAt(),
                defaultGearReward(stageNumber),
                normalTicketReward(stageNumber),
                new StageRewardResponse.MissionProgressResponse(
                        completedMissionCount(reward.getChildProfile().getId(), stageNumber),
                        totalMissionCount(stageNumber)
                )
        );
    }

    private long completedMissionCount(UUID childId, int stageNumber) {
        return missionSetProgressRepository.countCompletedStageStarOneMissionIds(childId, stageNumber);
    }

    private long totalMissionCount(int stageNumber) {
        return missionSetRepository.countActiveStageStarOneMissionIds((short) stageNumber);
    }

    private int defaultGearReward(int stageNumber) {
        return switch (stageNumber) {
            case 1 -> 30;
            case 2 -> 50;
            case 3 -> 80;
            default -> throw new AimongException(ErrorCode.INVALID_STAGE_NUMBER);
        };
    }

    private int normalTicketReward(int stageNumber) {
        return stageNumber == 3 ? 3 : 0;
    }

    private int requireStageNumber(Integer stageNumber) {
        if (stageNumber == null || stageNumber < 1 || stageNumber > 3) {
            throw new AimongException(ErrorCode.INVALID_STAGE_NUMBER);
        }
        return stageNumber;
    }
}
