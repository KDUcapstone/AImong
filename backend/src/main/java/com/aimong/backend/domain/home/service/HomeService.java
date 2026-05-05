package com.aimong.backend.domain.home.service;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.gacha.entity.TicketType;
import com.aimong.backend.domain.gacha.repository.TicketRepository;
import com.aimong.backend.domain.home.dto.HomeResponse;
import com.aimong.backend.domain.home.dto.StreakCalendarResponse;
import com.aimong.backend.domain.mission.dto.StageProgressResponse;
import com.aimong.backend.domain.mission.entity.Mission;
import com.aimong.backend.domain.mission.entity.MissionDailyProgress;
import com.aimong.backend.domain.mission.repository.MissionAttemptRepository;
import com.aimong.backend.domain.mission.repository.MissionDailyProgressRepository;
import com.aimong.backend.domain.mission.repository.MissionRepository;
import com.aimong.backend.domain.mission.service.MissionService;
import com.aimong.backend.domain.pet.entity.Pet;
import com.aimong.backend.domain.pet.repository.PetRepository;
import com.aimong.backend.domain.quest.entity.DailyQuest;
import com.aimong.backend.domain.quest.entity.DailyQuestType;
import com.aimong.backend.domain.quest.repository.DailyQuestRepository;
import com.aimong.backend.domain.quest.service.DailyQuestService;
import com.aimong.backend.domain.reward.repository.ReturnRewardClaimRepository;
import com.aimong.backend.domain.streak.entity.FriendStreak;
import com.aimong.backend.domain.streak.entity.StreakRecord;
import com.aimong.backend.domain.streak.repository.FriendStreakRepository;
import com.aimong.backend.domain.streak.repository.StreakRecordRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.aimong.backend.global.util.KstDateUtils;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class HomeService {

    private static final int TODAY_TARGET_COUNT = 3;

    private final ChildProfileRepository childProfileRepository;
    private final ChildActivityService childActivityService;
    private final PetRepository petRepository;
    private final MissionRepository missionRepository;
    private final MissionAttemptRepository missionAttemptRepository;
    private final MissionDailyProgressRepository missionDailyProgressRepository;
    private final MissionService missionService;
    private final StreakRecordRepository streakRecordRepository;
    private final FriendStreakRepository friendStreakRepository;
    private final DailyQuestRepository dailyQuestRepository;
    private final ReturnRewardClaimRepository returnRewardClaimRepository;
    private final TicketRepository ticketRepository;

    @Transactional
    public HomeResponse getHome(UUID childId) {
        childActivityService.touchLastActiveAt(childId);
        LocalDate today = KstDateUtils.today();
        ChildProfile childProfile = childProfileRepository.findById(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
        HomeResponse.TicketSummaryResponse tickets = ticketSummary(childId);
        HomeResponse.StreakSummaryResponse streak = streakSummary(childId, childProfile, today);
        HomeResponse.EquippedPetResponse equippedPet = equippedPet(childProfile);
        HomeResponse.RecommendedMissionResponse recommendedMission = recommendedMission(childId, today);

        return new HomeResponse(
                today,
                new HomeResponse.TopStatusResponse(
                        childProfile.getShieldCount(),
                        childProfile.getTotalXp(),
                        tickets.totalCount(),
                        streak.continuousDays()
                ),
                new HomeResponse.ProfileResponse(
                        childProfile.getId(),
                        childProfile.getNickname(),
                        childProfile.getProfileImageType().name(),
                        childProfile.getTotalXp(),
                        childProfile.getTodayXp(),
                        childProfile.getWeeklyXp()
                ),
                equippedPet,
                new HomeResponse.MissionSummaryResponse(
                        missionAttemptRepository.countByChildIdAndAttemptDateAndReviewFalseAndPassedTrue(childId, today),
                        TODAY_TARGET_COUNT,
                        equippedPet != null && recommendedMission != null,
                        recommendedMission
                ),
                streak,
                dailyQuestSummary(childId, today),
                returnReward(childId, streak.lastCompletedDate(), today),
                tickets
        );
    }

    @Transactional
    public StreakCalendarResponse getStreakCalendar(UUID childId, String yearMonth) {
        childActivityService.touchLastActiveAt(childId);
        childProfileRepository.findById(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
        YearMonth targetMonth = StringUtils.hasText(yearMonth)
                ? YearMonth.parse(yearMonth)
                : YearMonth.from(KstDateUtils.today());
        LocalDate startDate = targetMonth.atDay(1);
        LocalDate endDate = targetMonth.atEndOfMonth();
        List<LocalDate> completedDates = missionDailyProgressRepository
                .findAllByChildIdAndProgressDateBetweenOrderByProgressDateAsc(childId, startDate, endDate)
                .stream()
                .map(MissionDailyProgress::getProgressDate)
                .distinct()
                .toList();
        int continuousDays = streakRecordRepository.findById(childId)
                .map(StreakRecord::getContinuousDays)
                .orElse(0);

        return new StreakCalendarResponse(
                targetMonth.toString(),
                continuousDays,
                completedDates,
                KstDateUtils.today()
        );
    }

    private HomeResponse.EquippedPetResponse equippedPet(ChildProfile childProfile) {
        if (childProfile.getEquippedPetId() == null) {
            return null;
        }
        return petRepository.findByIdAndChildId(childProfile.getEquippedPetId(), childProfile.getId())
                .map(pet -> new HomeResponse.EquippedPetResponse(
                        pet.getId(),
                        pet.getPetType(),
                        pet.getGrade().name(),
                        pet.getXp(),
                        pet.getStage().name(),
                        pet.isCrownUnlocked(),
                        pet.getCrownType() == null ? null : pet.getCrownType().name()
                ))
                .orElse(null);
    }

    private HomeResponse.RecommendedMissionResponse recommendedMission(UUID childId, LocalDate today) {
        StageProgressResponse stageProgress = new StageProgressResponse(
                missionAttemptRepository.countCompletedMissionByStage(childId, (short) 1),
                missionAttemptRepository.countCompletedMissionByStage(childId, (short) 2),
                missionAttemptRepository.countCompletedMissionByStage(childId, (short) 3)
        );
        List<Mission> unlockedMissions = missionRepository.findAllByIsActiveTrueOrderByStageAscMissionCodeAscIdAsc()
                .stream()
                .filter(mission -> missionService.isUnlockedForChild(childId, mission, stageProgress))
                .toList();
        Set<UUID> todayCompletedMissionIds = missionDailyProgressRepository.findAllByChildIdAndProgressDate(childId, today)
                .stream()
                .map(MissionDailyProgress::getMissionId)
                .collect(Collectors.toSet());

        return unlockedMissions.stream()
                .filter(mission -> !todayCompletedMissionIds.contains(mission.getId()))
                .findFirst()
                .map(mission -> toRecommendedMission(mission, false))
                .orElseGet(() -> unlockedMissions.stream()
                        .filter(mission -> missionAttemptRepository.findLatestCompletedAt(childId, mission.getId()).isPresent())
                        .findFirst()
                        .map(mission -> toRecommendedMission(mission, true))
                        .orElse(null));
    }

    private HomeResponse.RecommendedMissionResponse toRecommendedMission(Mission mission, boolean reviewable) {
        return new HomeResponse.RecommendedMissionResponse(
                mission.getId(),
                mission.getStage(),
                mission.getTitle(),
                mission.getDescription(),
                reviewable
        );
    }

    private HomeResponse.StreakSummaryResponse streakSummary(UUID childId, ChildProfile childProfile, LocalDate today) {
        StreakRecord streakRecord = streakRecordRepository.findById(childId)
                .orElseGet(() -> StreakRecord.create(childId));
        int todayMissionCount = today.equals(streakRecord.getLastCompletedDate())
                ? streakRecord.getTodayMissionCount()
                : 0;
        return new HomeResponse.StreakSummaryResponse(
                streakRecord.getContinuousDays(),
                streakRecord.getLastCompletedDate(),
                todayMissionCount,
                childProfile.getShieldCount(),
                partner(childId, today)
        );
    }

    private HomeResponse.PartnerResponse partner(UUID childId, LocalDate today) {
        return friendStreakRepository.findById(childId)
                .map(FriendStreak::getPartnerChildId)
                .flatMap(partnerChildId -> childProfileRepository.findById(partnerChildId)
                        .map(partnerProfile -> new HomeResponse.PartnerResponse(
                                partnerProfile.getId(),
                                partnerProfile.getNickname(),
                                streakRecordRepository.findById(partnerChildId)
                                        .map(partnerStreak -> today.equals(partnerStreak.getLastCompletedDate())
                                                && partnerStreak.getTodayMissionCount() > 0)
                                        .orElse(false)
                        )))
                .orElse(null);
    }

    private HomeResponse.DailyQuestSummaryResponse dailyQuestSummary(UUID childId, LocalDate today) {
        List<DailyQuest> quests = dailyQuestRepository.findAllByChildIdAndQuestDate(childId, today)
                .stream()
                .sorted(Comparator.comparingInt(quest -> quest.getQuestType().ordinal()))
                .toList();
        long completedCount = quests.stream().filter(DailyQuest::isCompleted).count();
        long claimableCount = quests.stream()
                .filter(quest -> quest.isCompleted() && !quest.isRewardClaimed())
                .filter(quest -> Objects.equals(DailyQuestService.claimType(quest.getQuestType()), "MANUAL"))
                .count();
        return new HomeResponse.DailyQuestSummaryResponse(
                completedCount,
                DailyQuestType.values().length,
                claimableCount,
                quests.stream().map(this::toDailyQuestItem).toList()
        );
    }

    private HomeResponse.DailyQuestItemResponse toDailyQuestItem(DailyQuest quest) {
        DailyQuestType type = quest.getQuestType();
        int requiredValue = DailyQuestService.requiredValue(type);
        return new HomeResponse.DailyQuestItemResponse(
                type.name(),
                DailyQuestService.label(type),
                DailyQuestService.claimType(type),
                quest.isCompleted(),
                quest.isRewardClaimed(),
                new HomeResponse.ProgressResponse(Math.min(quest.getCurrentValue(), requiredValue), requiredValue)
        );
    }

    private HomeResponse.ReturnRewardResponse returnReward(UUID childId, LocalDate lastCompletedDate, LocalDate today) {
        if (lastCompletedDate == null) {
            return new HomeResponse.ReturnRewardResponse(false, null, null, null);
        }
        long daysMissed = ChronoUnit.DAYS.between(lastCompletedDate, today);
        if (daysMissed <= 2
                || returnRewardClaimRepository.existsByChildIdAndBaseLastCompletedDate(childId, lastCompletedDate)) {
            return new HomeResponse.ReturnRewardResponse(false, null, null, null);
        }
        int ticketCount = (int) Math.min(daysMissed - 2, 3);
        return new HomeResponse.ReturnRewardResponse(
                true,
                daysMissed,
                ticketCount,
                daysMissed + "일 만에 돌아왔어요! 티켓 " + ticketCount + "장 드릴게요!"
        );
    }

    private HomeResponse.TicketSummaryResponse ticketSummary(UUID childId) {
        return new HomeResponse.TicketSummaryResponse(
                Math.toIntExact(ticketRepository.countByChildIdAndTicketTypeAndUsedAtIsNull(childId, TicketType.NORMAL)),
                Math.toIntExact(ticketRepository.countByChildIdAndTicketTypeAndUsedAtIsNull(childId, TicketType.RARE)),
                Math.toIntExact(ticketRepository.countByChildIdAndTicketTypeAndUsedAtIsNull(childId, TicketType.EPIC))
        );
    }
}
