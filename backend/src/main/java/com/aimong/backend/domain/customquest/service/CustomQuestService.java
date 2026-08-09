package com.aimong.backend.domain.customquest.service;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.customquest.dto.ChildCustomQuestListResponse;
import com.aimong.backend.domain.customquest.dto.CreateCustomQuestRequest;
import com.aimong.backend.domain.customquest.dto.CustomQuestItemResponse;
import com.aimong.backend.domain.customquest.dto.CustomQuestListResponse;
import com.aimong.backend.domain.customquest.dto.CustomQuestStatusResponse;
import com.aimong.backend.domain.customquest.entity.CustomQuestStatus;
import com.aimong.backend.domain.customquest.entity.ParentCustomQuest;
import com.aimong.backend.domain.customquest.repository.ParentCustomQuestRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.aimong.backend.infra.fcm.FcmNotificationService;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomQuestService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_VISIBLE_QUEST_COUNT = 3;
    private static final Set<CustomQuestStatus> DEFAULT_PARENT_STATUSES = EnumSet.of(
            CustomQuestStatus.ACTIVE,
            CustomQuestStatus.PENDING_CONFIRM
    );
    private static final Set<CustomQuestStatus> CHILD_OPEN_STATUSES = EnumSet.of(
            CustomQuestStatus.ACTIVE,
            CustomQuestStatus.PENDING_CONFIRM
    );
    private static final Set<CustomQuestStatus> CHILD_CONFIRMED_STATUSES = EnumSet.of(
            CustomQuestStatus.COMPLETED,
            CustomQuestStatus.AUTO_CONFIRMED
    );

    private final ChildProfileRepository childProfileRepository;
    private final ParentCustomQuestRepository parentCustomQuestRepository;
    private final ChildActivityService childActivityService;
    private final FcmNotificationService fcmNotificationService;

    @Transactional
    public CustomQuestItemResponse createParentQuest(String parentId, UUID childId, CreateCustomQuestRequest request) {
        Instant now = Instant.now();
        processDueStatusTransitions(now);
        ChildProfile childProfile = validateOwnership(parentId, childId);
        long visibleCount = parentCustomQuestRepository.countByChildIdAndStatusNames(
                childId,
                statusNames(CHILD_OPEN_STATUSES)
        );
        if (visibleCount >= MAX_VISIBLE_QUEST_COUNT) {
            throw new AimongException(ErrorCode.MAX_QUEST_LIMIT);
        }

        ParentCustomQuest quest = ParentCustomQuest.create(
                childProfile.getParentAccount(),
                childProfile,
                request.title().trim(),
                trimToNull(request.description()),
                request.rewardText().trim(),
                request.expiresAt()
        );
        return toItem(parentCustomQuestRepository.save(quest));
    }

    @Transactional(readOnly = true)
    public CustomQuestListResponse getParentQuests(
            String parentId,
            UUID childId,
            String statusQuery,
            int page,
            int size
    ) {
        validateOwnership(parentId, childId);
        int safePage = Math.max(0, page);
        int safeSize = normalizePageSize(size);
        Set<CustomQuestStatus> statuses = parseStatuses(statusQuery);
        Page<ParentCustomQuest> questPage = parentCustomQuestRepository.findParentQuests(
                parentId,
                childId,
                statusNames(statuses),
                PageRequest.of(safePage, safeSize)
        );

        return new CustomQuestListResponse(
                safePage,
                safeSize,
                questPage.getTotalElements(),
                questPage.hasNext(),
                questPage.getContent().stream()
                        .map(this::toItem)
                        .toList()
        );
    }

    @Transactional
    public CustomQuestStatusResponse confirmParentQuest(String parentId, UUID questId) {
        processDueStatusTransitions(Instant.now());
        ParentCustomQuest quest = parentCustomQuestRepository.findByIdAndParentAccountParentId(questId, parentId)
                .orElseThrow(() -> new AimongException(ErrorCode.QUEST_NOT_FOUND));
        if (quest.getStatus() != CustomQuestStatus.PENDING_CONFIRM) {
            throw new AimongException(ErrorCode.QUEST_NOT_PENDING);
        }
        quest.confirm(Instant.now());
        return toStatusResponse(quest);
    }

    @Transactional
    public CustomQuestStatusResponse cancelParentQuest(String parentId, UUID questId) {
        processDueStatusTransitions(Instant.now());
        ParentCustomQuest quest = parentCustomQuestRepository.findByIdAndParentAccountParentId(questId, parentId)
                .orElseThrow(() -> new AimongException(ErrorCode.QUEST_NOT_FOUND));
        if (quest.getStatus() != CustomQuestStatus.ACTIVE) {
            throw new AimongException(ErrorCode.QUEST_NOT_CANCELLABLE);
        }
        quest.cancel(Instant.now());
        return toStatusResponse(quest);
    }

    @Transactional
    public ChildCustomQuestListResponse getChildQuests(UUID childId) {
        childActivityService.touchLastActiveAt(childId);
        processDueStatusTransitions(Instant.now());
        ensureChildExists(childId);
        Instant confirmedSince = Instant.now().minus(Duration.ofHours(24));
        List<ParentCustomQuest> quests = parentCustomQuestRepository.findVisibleChildQuests(
                childId,
                statusNames(CHILD_OPEN_STATUSES),
                statusNames(CHILD_CONFIRMED_STATUSES),
                confirmedSince
        );
        boolean hasPendingConfirm = quests.stream()
                .anyMatch(quest -> quest.getStatus() == CustomQuestStatus.PENDING_CONFIRM);
        return new ChildCustomQuestListResponse(
                quests.stream().map(this::toItem).toList(),
                hasPendingConfirm
        );
    }

    @Transactional
    public CustomQuestStatusResponse completeChildQuest(UUID childId, UUID questId) {
        childActivityService.touchLastActiveAt(childId);
        processDueStatusTransitions(Instant.now());
        ParentCustomQuest quest = parentCustomQuestRepository.findByIdAndChildProfileId(questId, childId)
                .orElseThrow(() -> new AimongException(ErrorCode.QUEST_NOT_FOUND));
        Instant now = Instant.now();
        if (quest.getStatus() != CustomQuestStatus.ACTIVE) {
            throw new AimongException(ErrorCode.QUEST_NOT_ACTIVE);
        }
        if (quest.getExpiresAt().isBefore(now)) {
            throw new AimongException(ErrorCode.QUEST_EXPIRED);
        }
        quest.requestCompletion(now);
        fcmNotificationService.sendQuestCompleteRequest(quest.getChildProfile(), quest.getId(), quest.getTitle());
        return toStatusResponse(quest);
    }

    @Transactional
    public void processDueStatusTransitions(Instant now) {
        autoConfirmPendingQuests(now);
        expireActiveQuests(now);
    }

    @Transactional
    public int autoConfirmPendingQuests(Instant now) {
        return parentCustomQuestRepository.autoConfirmPendingBefore(now.minus(Duration.ofHours(24)), now);
    }

    @Transactional
    public int expireActiveQuests(Instant now) {
        return parentCustomQuestRepository.expireActiveBefore(now);
    }

    private ChildProfile validateOwnership(String parentId, UUID childId) {
        ChildProfile childProfile = childProfileRepository.findByIdAndDeletedAtIsNull(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
        if (!childProfile.getParentAccount().getParentId().equals(parentId)) {
            throw new AimongException(ErrorCode.CHILD_NOT_FOUND);
        }
        return childProfile;
    }

    private void ensureChildExists(UUID childId) {
        if (!childProfileRepository.existsByIdAndDeletedAtIsNull(childId)) {
            throw new AimongException(ErrorCode.CHILD_NOT_FOUND);
        }
    }

    private Set<CustomQuestStatus> parseStatuses(String statusQuery) {
        if (statusQuery == null || statusQuery.isBlank()) {
            return DEFAULT_PARENT_STATUSES;
        }
        Set<CustomQuestStatus> statuses = EnumSet.noneOf(CustomQuestStatus.class);
        Arrays.stream(statusQuery.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .forEach(token -> statuses.add(parseStatus(token)));
        if (statuses.isEmpty()) {
            throw new AimongException(ErrorCode.BAD_REQUEST, "status must include at least one valid value");
        }
        return statuses;
    }

    private CustomQuestStatus parseStatus(String token) {
        try {
            return CustomQuestStatus.valueOf(token);
        } catch (IllegalArgumentException exception) {
            throw new AimongException(ErrorCode.BAD_REQUEST, "invalid custom quest status: " + token);
        }
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private List<String> statusNames(Set<CustomQuestStatus> statuses) {
        return statuses.stream()
                .map(Enum::name)
                .toList();
    }

    private CustomQuestItemResponse toItem(ParentCustomQuest quest) {
        return new CustomQuestItemResponse(
                quest.getId(),
                quest.getTitle(),
                quest.getDescription(),
                quest.getRewardText(),
                quest.getStatus().name(),
                quest.getExpiresAt(),
                quest.getCompletedAt(),
                quest.getConfirmedAt(),
                quest.getCreatedAt()
        );
    }

    private CustomQuestStatusResponse toStatusResponse(ParentCustomQuest quest) {
        return new CustomQuestStatusResponse(
                quest.getId(),
                quest.getStatus().name(),
                quest.getCompletedAt(),
                quest.getConfirmedAt()
        );
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
