package com.aimong.backend.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.chat.entity.ChatMessage;
import com.aimong.backend.domain.chat.entity.ChatSession;
import com.aimong.backend.domain.chat.entity.ChatUsage;
import com.aimong.backend.domain.chat.repository.ChatMessageRepository;
import com.aimong.backend.domain.chat.repository.ChatSessionRepository;
import com.aimong.backend.domain.chat.repository.ChatUsageRepository;
import com.aimong.backend.domain.pet.service.PetGrowthService;
import com.aimong.backend.domain.privacy.repository.PrivacyEventRepository;
import com.aimong.backend.domain.quest.service.AchievementService;
import com.aimong.backend.domain.quest.service.DailyQuestService;
import com.aimong.backend.domain.quest.service.WeeklyQuestService;
import com.aimong.backend.global.config.OpenAiProperties;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.aimong.backend.global.util.KstDateUtils;
import com.aimong.backend.infra.openai.OpenAiClient;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private ChatUsageRepository chatUsageRepository;
    @Mock private ChatSessionRepository chatSessionRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
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
        stubNewChatSession(childId);
        when(openAiClient.createChatReply(anyString(), anyString(), anyString())).thenReturn("힌트부터 생각해볼까요?");

        var response = service().send(childId, "숙제 힌트", false);

        assertThat(response.reply()).isEqualTo("힌트부터 생각해볼까요?");
        assertThat(response.sessionId()).isNotNull();
        assertThat(response.sessionExpiresAt()).isNotNull();
        assertThat(response.remainingCalls()).isEqualTo(19);
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
        stubNewChatSession(childId);
        when(openAiClient.createChatReply(anyString(), anyString(), anyString())).thenReturn("좋아요");

        service().send(childId, "내 이메일은 child@example.com 이야", false);

        verify(openAiClient).createChatReply(anyString(), anyString(), eq("내 이메일은 [***] 이야"));
        verify(privacyEventRepository).saveAll(any());
    }

    @Test
    void sendIncludesMaskedSessionHistoryForSameSessionOnly() {
        UUID childId = UUID.randomUUID();
        LocalDate today = KstDateUtils.today();
        ChatSession session = ChatSession.create(childId, Instant.now(), Duration.ofHours(1));
        when(childProfileRepository.findWithLockById(childId)).thenReturn(Optional.of(childProfile));
        when(chatUsageRepository.findWithLockByChildIdAndUsageDate(childId, today))
                .thenReturn(Optional.of(ChatUsage.create(childId, today)));
        when(chatSessionRepository.findByIdAndChildIdAndExpiresAtAfter(eq(session.getId()), eq(childId), any()))
                .thenReturn(Optional.of(session));
        when(chatMessageRepository.findTop10BySession_IdOrderByCreatedAtDesc(session.getId()))
                .thenReturn(List.of(
                        ChatMessage.assistant(session, "비밀번호는 공유하면 안 돼요.", Instant.now().minusSeconds(30)),
                        ChatMessage.user(session, "내 이메일은 [***] 이야", Instant.now().minusSeconds(60))
                ));
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(openAiClient.createChatReply(anyString(), anyString(), anyString())).thenReturn("좋아요");

        service().send(childId, "그럼 전화번호도 알려주면 안 돼?", false, session.getId());

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(openAiClient).createChatReply(anyString(), anyString(), promptCaptor.capture());
        assertThat(promptCaptor.getValue()).contains("내 이메일은 [***] 이야");
        assertThat(promptCaptor.getValue()).contains("비밀번호는 공유하면 안 돼요.");
        assertThat(promptCaptor.getValue()).contains("그럼 전화번호도 알려주면 안 돼?");
    }

    @Test
    void sendUsesMockReplyWhenOpenAiMockEnabled() {
        UUID childId = UUID.randomUUID();
        LocalDate today = KstDateUtils.today();
        when(childProfileRepository.findWithLockById(childId)).thenReturn(Optional.of(childProfile));
        when(chatUsageRepository.findWithLockByChildIdAndUsageDate(childId, today))
                .thenReturn(Optional.of(ChatUsage.create(childId, today)));
        stubNewChatSession(childId);

        var response = service(true).send(childId, "광합성이 뭐야?", false);

        assertThat(response.reply()).isNotBlank();
        assertThat(response.sessionId()).isNotNull();
        assertThat(response.remainingCalls()).isEqualTo(19);
        verify(openAiClient, never()).createChatReply(anyString(), anyString(), anyString());
    }

    @Test
    void sendGeneratesImageAndAppliesDailyImageLimit() {
        UUID childId = UUID.randomUUID();
        LocalDate today = KstDateUtils.today();
        when(childProfileRepository.findWithLockById(childId)).thenReturn(Optional.of(childProfile));
        when(chatUsageRepository.findWithLockByChildIdAndUsageDate(childId, today))
                .thenReturn(Optional.of(ChatUsage.create(childId, today)));
        stubNewChatSession(childId);
        when(openAiClient.createImage(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new OpenAiClient.GeneratedImage("base64-image", "png", "1024x1024", "low"));

        var response = service().send(childId, "draw a friendly robot", false, null, true);

        assertThat(response.reply()).isEqualTo("Image generated.");
        assertThat(response.image()).isNotNull();
        assertThat(response.image().b64Json()).isEqualTo("base64-image");
        assertThat(response.remainingImageCalls()).isEqualTo(4);
        assertThat(response.remainingCalls()).isEqualTo(19);
        verify(openAiClient).createImage("gpt-image-1-mini", "draw a friendly robot", "1024x1024", "low");
        verify(openAiClient, never()).createChatReply(anyString(), anyString(), anyString());
    }

    @Test
    void sendRejectsImageWhenDailyImageLimitReachedWithoutCallingOpenAi() {
        UUID childId = UUID.randomUUID();
        LocalDate today = KstDateUtils.today();
        ChatUsage usage = ChatUsage.create(childId, today);
        for (int i = 0; i < 5; i++) {
            usage.incrementImage();
        }
        when(childProfileRepository.findWithLockById(childId)).thenReturn(Optional.of(childProfile));
        when(chatUsageRepository.findWithLockByChildIdAndUsageDate(childId, today)).thenReturn(Optional.of(usage));

        assertThatThrownBy(() -> service().send(childId, "draw this", false, null, true))
                .isInstanceOf(AimongException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TOO_MANY_REQUESTS);

        verify(openAiClient, never()).createImage(anyString(), anyString(), anyString(), anyString());
        verify(openAiClient, never()).createChatReply(anyString(), anyString(), anyString());
    }

    private ChatService service() {
        return service(false);
    }

    private ChatService service(boolean mockEnabled) {
        return new ChatService(
                chatUsageRepository,
                chatSessionRepository,
                chatMessageRepository,
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

    private void stubNewChatSession(UUID childId) {
        when(chatSessionRepository.findFirstByChildIdAndExpiresAtAfterOrderByUpdatedAtDesc(eq(childId), any()))
                .thenReturn(Optional.empty());
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(chatMessageRepository.findTop10BySession_IdOrderByCreatedAtDesc(any())).thenReturn(List.of());
    }
}
