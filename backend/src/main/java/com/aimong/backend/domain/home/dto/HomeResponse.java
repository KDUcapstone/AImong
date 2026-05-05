package com.aimong.backend.domain.home.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record HomeResponse(
        LocalDate serverDate,
        TopStatusResponse topStatus,
        ProfileResponse profile,
        EquippedPetResponse equippedPet,
        MissionSummaryResponse missionSummary,
        StreakSummaryResponse streak,
        DailyQuestSummaryResponse dailyQuestSummary,
        ReturnRewardResponse returnReward,
        TicketSummaryResponse tickets
) {

    public record TopStatusResponse(
            int heartCount,
            int xp,
            int ticketCount,
            int streakDays
    ) {
    }

    public record ProfileResponse(
            UUID childId,
            String nickname,
            String profileImageType,
            int totalXp,
            int todayXp,
            int weeklyXp
    ) {
    }

    public record EquippedPetResponse(
            UUID id,
            String petType,
            String grade,
            int xp,
            String stage,
            boolean crownUnlocked,
            String crownType
    ) {
    }

    public record MissionSummaryResponse(
            long todayCompletedCount,
            int todayTargetCount,
            boolean canStartMission,
            RecommendedMissionResponse recommendedMission
    ) {
    }

    public record RecommendedMissionResponse(
            UUID id,
            short stage,
            String title,
            String description,
            boolean isReviewable
    ) {
    }

    public record StreakSummaryResponse(
            int continuousDays,
            LocalDate lastCompletedDate,
            int todayMissionCount,
            int shieldCount,
            PartnerResponse partner
    ) {
    }

    public record PartnerResponse(
            UUID childId,
            String nickname,
            boolean todayCompleted
    ) {
    }

    public record DailyQuestSummaryResponse(
            long completedCount,
            int totalCount,
            long claimableCount,
            List<DailyQuestItemResponse> quests
    ) {
    }

    public record DailyQuestItemResponse(
            String questType,
            String label,
            String claimType,
            boolean completed,
            boolean rewardClaimed,
            ProgressResponse progress
    ) {
    }

    public record ProgressResponse(
            int current,
            int required
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ReturnRewardResponse(
            boolean hasReward,
            Long daysMissed,
            Integer ticketCount,
            String message
    ) {
    }

    public record TicketSummaryResponse(
            int normal,
            int rare,
            int epic
    ) {
        public int totalCount() {
            return normal + rare + epic;
        }
    }
}
