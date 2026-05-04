package com.aimong.backend.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.chat.entity.ChatUsage;
import com.aimong.backend.domain.chat.repository.ChatUsageRepository;
import com.aimong.backend.domain.pet.service.PetGrowthService;
import com.aimong.backend.domain.privacy.repository.PrivacyEventRepository;
import com.aimong.backend.domain.quest.service.AchievementService;
import com.aimong.backend.domain.quest.service.DailyQuestService;
import com.aimong.backend.domain.quest.service.WeeklyQuestService;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.aimong.backend.global.config.OpenAiProperties;
import com.aimong.backend.global.util.KstDateUtils;
import com.aimong.backend.infra.openai.OpenAiClient;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private ChatUsageRepository chatUsageRepository;
    @Mock private ChildProfileRepository childProfileRepository;
    @Mock private ChildActivityService childActivityService;
    @Mock private PrivacyEventRepository privacyEventRepository;
    @Mock private OpenAiClient openAiClient;
    @Mock private PetGrowthService petGrowthService;
    @Mock private DailyQuestService dailyQuestService;
    @Mock private WeeklyQuestService weeklyQuestService;
    @Mock private AchievementService achievementService;
    @Mock private ChildProfile childProfile;

    @Test
    void sendIncrementsUsageAndGrantsFirstChatXp() {
        UUID childId = UUID.randomUUID();
        LocalDate today = KstDateUtils.today();
        when(childProfileRepository.findWithLockById(childId)).thenReturn(Optional.of(childProfile));
        when(chatUsageRepository.findWithLockByChildIdAndUsageDate(childId, today)).thenReturn(Optional.empty());
        when(chatUsageRepository.save(any(ChatUsage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(openAiClient.createChatReply(anyString(), anyString(), anyString())).thenReturn("힌트부터 생각해볼까요?");

        var response = service().send(childId, "숙제 대신 써줘", false);

        assertThat(response.reply()).isEqualTo("힌트부터 생각해볼까요?");
        assertThat(response.remainingCalls()).isEqualTo(19);
        assertThat(response.hintSuggestion()).isEqualTo("스스로 생각해보는 건 어때요? 힌트만 받아보세요!");
        verify(childProfile).applyMissionXp(5, today, KstDateUtils.currentWeekStart());
        verify(childProfile).refreshProfileImageType();
        verify(petGrowthService).applyMissionReward(childId, 5);
        verify(dailyQuestService).updateForChatSuccess(childId);
        verify(weeklyQuestService).updateForChatSuccess(childId);
        verify(achievementService).unlockByTotalXp(childId, childProfile);
    }

    @Test
    void sendRejectsWhenDailyLimitReachedWithoutCallingOpenAi() {
        UUID childId = UUID.randomUUID();
        LocalDate today = KstDateUtils.today();
        ChatUsage usage = ChatUsage.create(childId, today);
        for (int i = 0; i < 20; i++) {
            usage.increment();
        }
        when(childProfileRepository.findWithLockById(childId)).thenReturn(Optional.of(childProfile));
        when(chatUsageRepository.findWithLockByChildIdAndUsageDate(childId, today)).thenReturn(Optional.of(usage));

        assertThatThrownBy(() -> service().send(childId, "안녕", false))
                .isInstanceOf(AimongException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOO_MANY_REQUESTS);

        verify(openAiClient, never()).createChatReply(anyString(), anyString(), anyString());
        verify(dailyQuestService, never()).updateForChatSuccess(any());
    }

    @Test
    void sendMasksPrivacyBeforeOpenAiAndStoresDetectedTypeOnly() {
        UUID childId = UUID.randomUUID();
        LocalDate today = KstDateUtils.today();
        when(childProfileRepository.findWithLockById(childId)).thenReturn(Optional.of(childProfile));
        when(chatUsageRepository.findWithLockByChildIdAndUsageDate(childId, today))
                .thenReturn(Optional.of(ChatUsage.create(childId, today)));
        when(openAiClient.createChatReply(anyString(), anyString(), anyString())).thenReturn("좋아요");

        service().send(childId, "내 이메일은 child@example.com 이야", false);

        verify(openAiClient).createChatReply(anyString(), anyString(), eq("내 이메일은 [***] 이야"));
        verify(privacyEventRepository).saveAll(any());
    }

    @Test
    void sendUsesMockReplyWhenOpenAiMockEnabled() {
        UUID childId = UUID.randomUUID();
        LocalDate today = KstDateUtils.today();
        when(childProfileRepository.findWithLockById(childId)).thenReturn(Optional.of(childProfile));
        when(chatUsageRepository.findWithLockByChildIdAndUsageDate(childId, today))
                .thenReturn(Optional.of(ChatUsage.create(childId, today)));

        var response = service(true).send(childId, "광합성이 뭐야?", false);

        assertThat(response.reply()).contains("테스트 응답");
        assertThat(response.remainingCalls()).isEqualTo(19);
        verify(openAiClient, never()).createChatReply(anyString(), anyString(), anyString());
    }

    private ChatService service() {
        return service(false);
    }

    private ChatService service(boolean mockEnabled) {
        return new ChatService(
                chatUsageRepository,
                childProfileRepository,
                childActivityService,
                new PrivacyMaskingService(),
                privacyEventRepository,
                openAiClient,
                new OpenAiProperties("", "", "", "https://api.openai.com/v1", "/responses", mockEnabled),
                petGrowthService,
                dailyQuestService,
                weeklyQuestService,
                achievementService
        );
    }
}
