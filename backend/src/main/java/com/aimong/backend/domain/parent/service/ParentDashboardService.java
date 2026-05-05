package com.aimong.backend.domain.parent.service;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.mission.repository.MissionAnswerResultRepository;
import com.aimong.backend.domain.mission.repository.MissionAttemptRepository;
import com.aimong.backend.domain.mission.repository.MissionDailyProgressRepository;
import com.aimong.backend.domain.parent.dto.ParentChildSummaryResponse;
import com.aimong.backend.domain.parent.dto.ParentDailyProgressStat;
import com.aimong.backend.domain.parent.dto.ParentPrivacyLogResponse;
import com.aimong.backend.domain.parent.dto.ParentWeakPointResponse;
import com.aimong.backend.domain.parent.dto.ParentWeakPointsResponse;
import com.aimong.backend.domain.parent.dto.ParentWeeklyStatsResponse;
import com.aimong.backend.domain.privacy.entity.PrivacyEvent;
import com.aimong.backend.domain.privacy.repository.PrivacyEventRepository;
import com.aimong.backend.domain.streak.entity.StreakRecord;
import com.aimong.backend.domain.streak.repository.StreakRecordRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.aimong.backend.global.util.KstDateUtils;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ParentDashboardService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final ChildProfileRepository childProfileRepository;
    private final StreakRecordRepository streakRecordRepository;
    private final MissionAttemptRepository missionAttemptRepository;
    private final MissionDailyProgressRepository missionDailyProgressRepository;
    private final MissionAnswerResultRepository missionAnswerResultRepository;
    private final PrivacyEventRepository privacyEventRepository;

    @Transactional(readOnly = true)
    public ParentChildSummaryResponse getSummary(String parentId, UUID childId) {
        ChildProfile childProfile = validateOwnership(parentId, childId);
        StreakRecord streakRecord = streakRecordRepository.findById(childId)
                .orElseGet(() -> StreakRecord.create(childId));
        LocalDate today = KstDateUtils.today();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);

        return new ParentChildSummaryResponse(
                childProfile.getNickname(),
                childProfile.getProfileImageType().name(),
                childProfile.getTotalXp(),
                streakRecord.getContinuousDays(),
                childProfile.getShieldCount(),
                missionAttemptRepository.countFirstCompletedMissionBetween(childId, weekStart, today),
                missionAttemptRepository.countCompletedMission(childId),
                childProfile.getLastActiveAt()
        );
    }

    @Transactional(readOnly = true)
    public ParentWeeklyStatsResponse getWeeklyStats(String parentId, UUID childId) {
        validateOwnership(parentId, childId);
        LocalDate today = KstDateUtils.today();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);
        Map<LocalDate, ParentDailyProgressStat> statsByDate = missionDailyProgressRepository
                .findDailyProgressStats(childId, weekStart, weekEnd)
                .stream()
                .collect(Collectors.toMap(ParentDailyProgressStat::date, Function.identity()));

        List<ParentWeeklyStatsResponse.DailyStatResponse> dailyStats = weekStart.datesUntil(weekEnd.plusDays(1))
                .map(date -> toDailyStat(date, statsByDate.get(date)))
                .toList();

        return new ParentWeeklyStatsResponse(
                weekStart,
                weekEnd,
                dailyStats.stream().mapToInt(ParentWeeklyStatsResponse.DailyStatResponse::xpEarned).sum(),
                dailyStats.stream().mapToInt(ParentWeeklyStatsResponse.DailyStatResponse::missionCount).sum(),
                dailyStats
        );
    }

    @Transactional(readOnly = true)
    public ParentPrivacyLogResponse getPrivacyLog(String parentId, UUID childId, int page, int size) {
        validateOwnership(parentId, childId);
        int safePage = Math.max(0, page);
        int safeSize = normalizePageSize(size);
        PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "detectedAt"));
        Page<PrivacyEvent> eventPage = privacyEventRepository.findByChildId(childId, pageRequest);
        Instant weekStart = KstDateUtils.today()
                .with(DayOfWeek.MONDAY)
                .atStartOfDay(KST)
                .toInstant();

        return new ParentPrivacyLogResponse(
                safePage,
                safeSize,
                eventPage.getTotalElements(),
                eventPage.hasNext(),
                privacyEventRepository.countByChildIdAndDetectedAtGreaterThanEqual(childId, weekStart),
                eventPage.getContent().stream()
                        .map(this::toPrivacyEventResponse)
                        .toList()
        );
    }

    @Transactional(readOnly = true)
    public ParentWeakPointsResponse getWeakPoints(String parentId, UUID childId, int page, int size) {
        validateOwnership(parentId, childId);
        int safePage = Math.max(0, page);
        int safeSize = normalizePageSize(size);
        Instant since = Instant.now().minus(30, ChronoUnit.DAYS);
        Page<ParentWeakPointResponse> weakPointPage = missionAnswerResultRepository.findWeakPointsByChildId(
                childId,
                since,
                PageRequest.of(safePage, safeSize)
        );

        return new ParentWeakPointsResponse(
                safePage,
                safeSize,
                weakPointPage.getTotalElements(),
                weakPointPage.hasNext(),
                "최근 30일",
                weakPointPage.getContent()
        );
    }

    private ChildProfile validateOwnership(String parentId, UUID childId) {
        ChildProfile childProfile = childProfileRepository.findById(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
        if (!childProfile.getParentAccount().getParentId().equals(parentId)) {
            throw new AimongException(ErrorCode.FORBIDDEN);
        }
        return childProfile;
    }

    private ParentWeeklyStatsResponse.DailyStatResponse toDailyStat(LocalDate date, ParentDailyProgressStat stat) {
        return new ParentWeeklyStatsResponse.DailyStatResponse(
                date,
                date.getDayOfWeek().name().substring(0, 3),
                stat == null ? 0 : Math.toIntExact(stat.missionCount()),
                stat == null ? 0 : Math.toIntExact(stat.xpEarned())
        );
    }

    private ParentPrivacyLogResponse.PrivacyEventResponse toPrivacyEventResponse(PrivacyEvent privacyEvent) {
        return new ParentPrivacyLogResponse.PrivacyEventResponse(
                privacyEvent.getDetectedType().name(),
                privacyEvent.isMasked(),
                privacyEvent.getDetectedAt()
        );
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
