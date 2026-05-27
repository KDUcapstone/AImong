package com.aimong.backend.domain.streak.service;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.streak.dto.PartnerConnectResponse;
import com.aimong.backend.domain.streak.dto.PartnerDisconnectResponse;
import com.aimong.backend.domain.streak.dto.ShieldPurchaseResponse;
import com.aimong.backend.domain.streak.dto.ShieldUseResponse;
import com.aimong.backend.domain.streak.dto.StreakResponse;
import com.aimong.backend.domain.streak.entity.FriendStreak;
import com.aimong.backend.domain.streak.entity.StreakRecord;
import com.aimong.backend.domain.streak.repository.FriendStreakRepository;
import com.aimong.backend.domain.streak.repository.StreakRecordRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.aimong.backend.global.util.KstDateUtils;
import com.aimong.backend.domain.reward.entity.CurrencyTransactionReason;
import com.aimong.backend.domain.reward.service.CurrencyService;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StreakService {

    private final StreakRecordRepository streakRecordRepository;
    private final FriendStreakRepository friendStreakRepository;
    private final ChildProfileRepository childProfileRepository;
    private final ChildActivityService childActivityService;
    private final CurrencyService currencyService;

    @Autowired
    public StreakService(
            StreakRecordRepository streakRecordRepository,
            FriendStreakRepository friendStreakRepository,
            ChildProfileRepository childProfileRepository,
            ChildActivityService childActivityService,
            CurrencyService currencyService
    ) {
        this.streakRecordRepository = streakRecordRepository;
        this.friendStreakRepository = friendStreakRepository;
        this.childProfileRepository = childProfileRepository;
        this.childActivityService = childActivityService;
        this.currencyService = currencyService;
    }

    public StreakService(
            StreakRecordRepository streakRecordRepository,
            FriendStreakRepository friendStreakRepository,
            ChildProfileRepository childProfileRepository,
            ChildActivityService childActivityService
    ) {
        this.streakRecordRepository = streakRecordRepository;
        this.friendStreakRepository = friendStreakRepository;
        this.childProfileRepository = childProfileRepository;
        this.childActivityService = childActivityService;
        this.currencyService = null;
    }

    @Transactional
    public StreakResponse getStreak(UUID childId) {
        childActivityService.touchLastActiveAt(childId);
        StreakRecord streak = streakRecordRepository.findById(childId)
                .orElseGet(() -> streakRecordRepository.save(StreakRecord.create(childId)));
        ChildProfile profile = childProfileRepository.findById(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
        LocalDate today = KstDateUtils.today();
        streak.expireRecoveryIfPast(today);

        return new StreakResponse(
                streak.getContinuousDays(),
                streak.getLastCompletedDate(),
                todayMissionCountForToday(streak, today),
                profile.getShieldCount(),
                streak.getStatus().name(),
                streak.isRecoveryAvailable(today),
                streak.getRecoveryDeadlineDate(),
                streak.getLastShieldUsedDate(),
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

    @Transactional
    public ShieldPurchaseResponse purchaseShields(UUID childId, int count) {
        childActivityService.touchLastActiveAt(childId);
        if (count < 1) {
            throw new AimongException(ErrorCode.BAD_REQUEST);
        }
        ChildProfile childProfile = childProfileRepository.findWithLockById(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
        int cost = CurrencyService.STREAK_SHIELD_COST * count;
        boolean consumed = currencyService == null
                ? childProfile.consumeGear(cost)
                : currencyService.consumeGear(
                        childProfile,
                        cost,
                        CurrencyTransactionReason.STREAK_SHIELD_PURCHASE,
                        "STREAK_SHIELD",
                        childId.toString()
                );
        if (!consumed) {
            throw new AimongException(ErrorCode.GEAR_NOT_ENOUGH);
        }
        childProfile.addShield(count);
        return new ShieldPurchaseResponse(
                childProfile.getShieldCount(),
                count,
                CurrencyService.STREAK_SHIELD_COST,
                childProfile.getGear()
        );
    }

    @Transactional
    public ShieldUseResponse useShield(UUID childId) {
        childActivityService.touchLastActiveAt(childId);
        LocalDate today = KstDateUtils.today();
        StreakRecord streak = streakRecordRepository.findWithLockByChildId(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.STREAK_NOT_RECOVERABLE));
        if (streak.expireRecoveryIfPast(today)) {
            throw new AimongException(ErrorCode.RECOVERY_EXPIRED);
        }
        if (!streak.isRecoveryAvailable(today)) {
            throw new AimongException(ErrorCode.STREAK_NOT_RECOVERABLE);
        }

        ChildProfile childProfile = childProfileRepository.findWithLockById(childId)
                .orElseThrow(() -> new AimongException(ErrorCode.CHILD_NOT_FOUND));
        if (!childProfile.consumeShieldIfAvailable()) {
            throw new AimongException(ErrorCode.SHIELD_NOT_ENOUGH);
        }

        LocalDate protectedDate = streak.getRecoveryDeadlineDate().minusDays(1);
        streak.markProtectedByShield(protectedDate);
        if (currencyService != null) {
            currencyService.recordZeroAmountEvent(
                    childProfile,
                    CurrencyTransactionReason.STREAK_SHIELD_USE,
                    "STREAK_SHIELD",
                    protectedDate.toString()
            );
        }
        return new ShieldUseResponse(
                childProfile.getShieldCount(),
                streak.getStatus().name(),
                streak.getContinuousDays(),
                streak.getLastShieldUsedDate(),
                streak.isRecoveryAvailable(today),
                streak.getRecoveryDeadlineDate()
        );
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
