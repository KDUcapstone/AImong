package com.aimong.backend.global.scheduler;

import com.aimong.backend.domain.chat.repository.ChatSessionRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ChatSessionCleanupScheduler {

    private final ChatSessionRepository chatSessionRepository;

    @Transactional
    @Scheduled(cron = "0 */30 * * * *", zone = "Asia/Seoul")
    public void deleteExpiredChatSessions() {
        chatSessionRepository.deleteByExpiresAtBefore(Instant.now());
    }
}
