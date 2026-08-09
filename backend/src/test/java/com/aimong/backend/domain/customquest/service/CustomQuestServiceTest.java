package com.aimong.backend.domain.customquest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.entity.ParentAccount;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.customquest.dto.CustomQuestListResponse;
import com.aimong.backend.domain.customquest.entity.ParentCustomQuest;
import com.aimong.backend.domain.customquest.repository.ParentCustomQuestRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.aimong.backend.infra.fcm.FcmNotificationService;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class CustomQuestServiceTest {

    @Mock
    private ChildProfileRepository childProfileRepository;

    @Mock
    private ParentCustomQuestRepository parentCustomQuestRepository;

    @Mock
    private ChildActivityService childActivityService;

    @Mock
    private FcmNotificationService fcmNotificationService;

    @Test
    void parentPastQuestListParsesStatusesAndReturnsHasNext() {
        CustomQuestService service = service();
        String parentId = "parent-1";
        UUID childId = UUID.randomUUID();
        ChildProfile childProfile = childProfile(parentId);

        when(childProfileRepository.findByIdAndDeletedAtIsNull(childId)).thenReturn(Optional.of(childProfile));
        when(parentCustomQuestRepository.findParentQuests(
                eq(parentId),
                eq(childId),
                any(),
                eq(PageRequest.of(0, 10))
        )).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 11));

        CustomQuestListResponse response = service.getParentQuests(
                parentId,
                childId,
                "COMPLETED,CANCELLED",
                0,
                10
        );

        ArgumentCaptor<Collection<String>> statusesCaptor = ArgumentCaptor.captor();
        verify(parentCustomQuestRepository).findParentQuests(
                eq(parentId),
                eq(childId),
                statusesCaptor.capture(),
                eq(PageRequest.of(0, 10))
        );
        assertThat(statusesCaptor.getValue())
                .containsExactlyInAnyOrder("COMPLETED", "CANCELLED");
        assertThat(response.totalCount()).isEqualTo(11);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(10);
    }

    @Test
    void completingNonActiveQuestFails() {
        CustomQuestService service = service();
        UUID childId = UUID.randomUUID();
        UUID questId = UUID.randomUUID();
        ParentCustomQuest quest = ParentCustomQuest.create(
                ParentAccount.create("parent-1", "parent@example.com"),
                childProfile("parent-1"),
                "Quest",
                null,
                "Reward",
                java.time.Instant.now().plusSeconds(3600)
        );
        quest.requestCompletion(java.time.Instant.now());

        when(parentCustomQuestRepository.findByIdAndChildProfileId(questId, childId)).thenReturn(Optional.of(quest));

        assertThatThrownBy(() -> service.completeChildQuest(childId, questId))
                .isInstanceOf(AimongException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.QUEST_NOT_ACTIVE);
    }

    private ChildProfile childProfile(String parentId) {
        return ChildProfile.create(ParentAccount.create(parentId, "parent@example.com"), "child", "123456");
    }

    private CustomQuestService service() {
        return new CustomQuestService(
                childProfileRepository,
                parentCustomQuestRepository,
                childActivityService,
                fcmNotificationService
        );
    }
}
