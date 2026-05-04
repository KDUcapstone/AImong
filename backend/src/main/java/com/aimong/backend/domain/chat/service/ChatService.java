package com.aimong.backend.domain.chat.service;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.chat.dto.ChatResponse;
import com.aimong.backend.domain.chat.entity.ChatUsage;
import com.aimong.backend.domain.chat.repository.ChatUsageRepository;
import com.aimong.backend.domain.pet.service.PetGrowthService;
import com.aimong.backend.domain.privacy.entity.PrivacyEvent;
import com.aimong.backend.domain.privacy.repository.PrivacyEventRepository;
import com.aimong.backend.domain.quest.service.AchievementService;
import com.aimong.backend.domain.quest.service.DailyQuestService;
import com.aimong.backend.domain.quest.service.WeeklyQuestService;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.aimong.backend.global.config.OpenAiProperties;
import com.aimong.backend.global.util.KstDateUtils;
import com.aimong.backend.infra.openai.OpenAiClient;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int DAILY_LIMIT = 20;
    private static final int FIRST_CHAT_XP = 5;
    private static final int GPT_TIMEOUT_SECONDS = 15;
    private static final String CHAT_MODEL = "gpt-5-mini";
    private static final String HINT_SUGGESTION = "스스로 생각해보는 건 어때요? 힌트만 받아보세요!";
    private static final List<String> HINT_TRIGGER_WORDS = List.of("숙제", "해줘", "대신", "써줘");
    private static final String DEVELOPER_PROMPT = """
            너는 초등학생이 AI를 안전하고 비판적으로 연습하도록 돕는 AImong의 AI 친구다.
            한국어로 2~4문장 안에서 쉽고 친절하게 답한다.
            숙제나 글쓰기를 대신 완성해 달라는 요청에는 정답 전체를 대신 작성하지 말고 힌트, 생각 순서, 확인 방법을 안내한다.
            개인정보를 묻거나 저장하려 하지 말고, 위험한 개인정보가 보이면 공유하지 말라고 부드럽게 알려준다.
            """;

    private final ChatUsageRepository chatUsageRepository;
    private final ChildProfileRepository childProfileRepository;
    private final ChildActivityService childActivityService;
    private final PrivacyMaskingService privacyMaskingService;
    private final PrivacyEventRepository privacyEventRepository;
    private final OpenAiClient openAiClient;
    private final OpenAiProperties openAiProperties;
    private final PetGrowthService petGrowthService;
    private final DailyQuestService dailyQuestService;
    private final WeeklyQuestService weeklyQuestService;
    private final AchievementService achievementService;

    @Transactional
    public ChatResponse send(UUID childId, String message, boolean masked) {
        childActivityService.touchLastActiveAt(childId);
        ChildProfile childProfile = childProfileRepository.findWithLockById(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));

        ChatUsage usage = chatUsageRepository.findWithLockByChildIdAndUsageDate(childId, KstDateUtils.today())
                .orElseGet(() -> chatUsageRepository.save(ChatUsage.create(childId, KstDateUtils.today())));

        if (usage.getCount() >= DAILY_LIMIT) {
            throw new AimongException(ErrorCode.TOO_MANY_REQUESTS, "오늘은 충분히 이야기했어요! 내일 또 만나요");
        }

        PrivacyMaskingService.MaskingResult maskingResult = privacyMaskingService.mask(message);
        savePrivacyEvents(childId, maskingResult, masked);
        boolean hintTriggered = isHintTriggered(maskingResult.sanitizedMessage());
        String reply = requestGptReply(maskingResult.sanitizedMessage());

        boolean firstSuccessToday = usage.getCount() == 0;
        usage.increment();

        if (firstSuccessToday) {
            childProfile.applyMissionXp(FIRST_CHAT_XP, KstDateUtils.today(), KstDateUtils.currentWeekStart());
            childProfile.refreshProfileImageType();
            petGrowthService.applyMissionReward(childId, FIRST_CHAT_XP);
        }

        dailyQuestService.updateForChatSuccess(childId);
        weeklyQuestService.updateForChatSuccess(childId);
        achievementService.unlockByTotalXp(childId, childProfile);

        return new ChatResponse(
                reply,
                DAILY_LIMIT - usage.getCount(),
                hintTriggered ? HINT_SUGGESTION : null
        );
    }

    private String requestGptReply(String sanitizedMessage) {
        if (openAiProperties.mockEnabled()) {
            return createMockReply(sanitizedMessage);
        }

        try {
            return CompletableFuture
                    .supplyAsync(() -> openAiClient.createChatReply(CHAT_MODEL, DEVELOPER_PROMPT, sanitizedMessage))
                    .get(GPT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException exception) {
            throw new AimongException(ErrorCode.GATEWAY_TIMEOUT, "AI 친구가 생각 중이에요. 다시 시도해볼까요?");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AimongException(ErrorCode.GATEWAY_TIMEOUT, "AI 친구가 생각 중이에요. 다시 시도해볼까요?");
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof AimongException aimongException) {
                throw aimongException;
            }
            throw new AimongException(ErrorCode.INTERNAL_SERVER_ERROR, "AI 친구가 지금 쉬고 있어요. 잠시 후 다시 시도해주세요");
        }
    }

    private String createMockReply(String sanitizedMessage) {
        if (isHintTriggered(sanitizedMessage)) {
            return "테스트 응답이에요. 대신 완성해주기보다는 먼저 네 생각을 한 문장으로 적고, 그다음 필요한 힌트를 물어보면 좋아요.";
        }
        return "테스트 응답이에요. 실제 OpenAI 호출 없이 챗봇 흐름, 사용량, 퀘스트 진행도만 확인하고 있어요.";
    }

    private void savePrivacyEvents(UUID childId, PrivacyMaskingService.MaskingResult maskingResult, boolean requestMasked) {
        if (maskingResult.detectedTypes().isEmpty()) {
            return;
        }

        boolean masked = requestMasked || !maskingResult.sanitizedMessage().isBlank();
        privacyEventRepository.saveAll(maskingResult.detectedTypes().stream()
                .map(type -> PrivacyEvent.create(childId, type, masked))
                .toList());
    }

    private boolean isHintTriggered(String sanitizedMessage) {
        return HINT_TRIGGER_WORDS.stream().anyMatch(sanitizedMessage::contains);
    }
}
