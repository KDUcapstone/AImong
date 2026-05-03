package com.aimong.backend.domain.streak.service;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.streak.dto.PartnerConnectResponse;
import com.aimong.backend.domain.streak.dto.PartnerDisconnectResponse;
import com.aimong.backend.domain.streak.dto.StreakResponse;
import com.aimong.backend.domain.streak.entity.FriendStreak;
import com.aimong.backend.domain.streak.entity.StreakRecord;
import com.aimong.backend.domain.streak.repository.FriendStreakRepository;
import com.aimong.backend.domain.streak.repository.StreakRecordRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.aimong.backend.global.util.KstDateUtils;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StreakService {

    private final StreakRecordRepository streakRecordRepository;
    private final FriendStreakRepository friendStreakRepository;
    private final ChildProfileRepository childProfileRepository;
    private final ChildActivityService childActivityService;

    @Transactional
    public StreakResponse getStreak(UUID childId) {
        childActivityService.touchLastActiveAt(childId);
        StreakRecord streak = streakRecordRepository.findById(childId)
                .orElseGet(() -> streakRecordRepository.save(StreakRecord.create(childId)));
        ChildProfile profile = childProfileRepository.findById(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
        LocalDate today = KstDateUtils.today();

        return new StreakResponse(
                streak.getContinuousDays(),
                streak.getLastCompletedDate(),
                todayMissionCountForToday(streak, today),
                profile.getShieldCount(),
                findPartner(childId, today)
        );
    }

    @Transactional
    public PartnerConnectResponse connectPartner(UUID childId, String partnerCode) {
        childActivityService.touchLastActiveAt(childId);

        ChildProfile partner = childProfileRepository.findByCode(partnerCode)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_CODE_NOT_FOUND));
        UUID partnerChildId = partner.getId();
        if (childId.equals(partnerChildId)) {
            throw new AimongException(ErrorCode.BAD_REQUEST, "본인의 코드는 입력할 수 없어요");
        }

        lockProfilesInStableOrder(childId, partnerChildId);

        if (friendStreakRepository.existsById(childId) || friendStreakRepository.existsByPartnerChildId(childId)) {
            throw new AimongException(ErrorCode.CONFLICT, "이미 친구와 연결되어 있어요");
        }
        if (friendStreakRepository.existsById(partnerChildId) || friendStreakRepository.existsByPartnerChildId(partnerChildId)) {
            throw new AimongException(ErrorCode.CONFLICT, "친구가 이미 다른 친구와 연결되어 있어요");
        }

        friendStreakRepository.save(FriendStreak.create(childId, partnerChildId));
        friendStreakRepository.save(FriendStreak.create(partnerChildId, childId));

        return new PartnerConnectResponse(new PartnerConnectResponse.PartnerResponse(
                partner.getId(),
                partner.getNickname()
        ));
    }

    @Transactional
    public PartnerDisconnectResponse disconnectPartner(UUID childId) {
        childActivityService.touchLastActiveAt(childId);
        friendStreakRepository.findById(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.NOT_FOUND, "연결된 친구가 없어요"));

        friendStreakRepository.deleteByChildIdOrPartnerChildId(childId, childId);
        return new PartnerDisconnectResponse(true);
    }

    private int todayMissionCountForToday(StreakRecord streak, LocalDate today) {
        if (!today.equals(streak.getLastCompletedDate())) {
            return 0;
        }
        return streak.getTodayMissionCount();
    }

    private StreakResponse.PartnerResponse findPartner(UUID childId, LocalDate today) {
        return friendStreakRepository.findById(childId)
                .map(FriendStreak::getPartnerChildId)
                .map(partnerChildId -> toPartnerResponse(partnerChildId, today))
                .orElse(null);
    }

    private StreakResponse.PartnerResponse toPartnerResponse(UUID partnerChildId, LocalDate today) {
        ChildProfile partner = childProfileRepository.findById(partnerChildId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
        boolean todayCompleted = streakRecordRepository.findById(partnerChildId)
                .map(partnerStreak -> today.equals(partnerStreak.getLastCompletedDate())
                        && partnerStreak.getTodayMissionCount() > 0)
                .orElse(false);
        return new StreakResponse.PartnerResponse(
                partner.getId(),
                partner.getNickname(),
                todayCompleted
        );
    }

    private void lockProfilesInStableOrder(UUID firstChildId, UUID secondChildId) {
        List.of(firstChildId, secondChildId).stream()
                .sorted(Comparator.comparing(UUID::toString))
                .forEach(childId -> childProfileRepository.findWithLockById(childId)
                        .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND)));
    }
}
