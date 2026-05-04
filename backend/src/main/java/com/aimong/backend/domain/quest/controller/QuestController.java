package com.aimong.backend.domain.quest.controller;

import com.aimong.backend.domain.quest.dto.AchievementsResponse;
import com.aimong.backend.domain.quest.dto.ClaimRequest;
import com.aimong.backend.domain.quest.dto.ClaimResponse;
import com.aimong.backend.domain.quest.dto.DailyQuestResponse;
import com.aimong.backend.domain.quest.dto.WeeklyQuestResponse;
import com.aimong.backend.domain.quest.service.AchievementService;
import com.aimong.backend.domain.quest.service.DailyQuestService;
import com.aimong.backend.domain.quest.service.QuestClaimService;
import com.aimong.backend.domain.quest.service.WeeklyQuestService;
import com.aimong.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class QuestController {

    private final DailyQuestService dailyQuestService;
    private final WeeklyQuestService weeklyQuestService;
    private final QuestClaimService questClaimService;
    private final AchievementService achievementService;

    @GetMapping("/quests/daily")
    public ApiResponse<DailyQuestResponse> getDailyQuests(Authentication authentication) {
        return ApiResponse.success(dailyQuestService.getDailyQuests(extractChildId(authentication)));
    }

    @GetMapping("/quests/weekly")
    public ApiResponse<WeeklyQuestResponse> getWeeklyQuests(Authentication authentication) {
        return ApiResponse.success(weeklyQuestService.getWeeklyQuests(extractChildId(authentication)));
    }

    @PostMapping("/quests/claim")
    public ApiResponse<ClaimResponse> claim(
            @Valid @RequestBody ClaimRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(questClaimService.claim(
                extractChildId(authentication),
                request.questType(),
                request.period()
        ));
    }

    @GetMapping("/achievements")
    public ApiResponse<AchievementsResponse> getAchievements(Authentication authentication) {
        return ApiResponse.success(achievementService.getAchievements(extractChildId(authentication)));
    }

    private UUID extractChildId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
