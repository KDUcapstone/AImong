package com.aimong.backend.domain.customquest.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomQuestScheduler {

    private final CustomQuestService customQuestService;

    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    public void autoConfirmPendingQuests() {
        customQuestService.autoConfirmPendingQuests(Instant.now());
    }

    @Scheduled(cron = "0 1 0 * * *", zone = "Asia/Seoul")
    public void expireActiveQuests() {
        customQuestService.expireActiveQuests(Instant.now());
    }
}
