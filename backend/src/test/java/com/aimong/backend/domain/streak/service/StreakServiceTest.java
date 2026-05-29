package com.aimong.backend.domain.streak.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.entity.ParentAccount;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.reward.repository.CurrencyTransactionRepository;
import com.aimong.backend.domain.reward.service.CurrencyService;
import com.aimong.backend.domain.streak.entity.FriendStreak;
import com.aimong.backend.domain.streak.entity.StreakRecord;
import com.aimong.backend.domain.streak.entity.StreakStatus;
import com.aimong.backend.domain.streak.repository.FriendStreakRepository;
import com.aimong.backend.domain.streak.repository.StreakRecordRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.aimong.backend.global.util.KstDateUtils;
import java.lang.reflect.Constructor;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StreakServiceTest {

    @Mock private StreakRecordRepository streakRecordRepository;
    @Mock private FriendStreakRepository friendStreakRepository;
    @Mock private ChildProfileRepository childProfileRepository;
    @Mock private ChildActivityService childActivityService;
    @Mock private CurrencyTransactionRepository currencyTransactionRepository;

    @Test
    void getStreakReturnsZeroTodayMissionCountWhenLastCompletedDateIsNotToday() {
        StreakService service = service();
        UUID childId = UUID.randomUUID();
        LocalDate yesterday = KstDateUtils.today().minusDays(1);
        StreakRecord streak = StreakRecord.create(childId);
        streak.recordMissionCompletion(yesterday);
        ChildProfile child = org.mockito.Mockito.mock(ChildProfile.class);

        when(streakRecordRepository.findById(childId)).thenReturn(Optional.of(streak));
        when(childProfileRepository.findById(childId)).thenReturn(Optional.of(child));
        var response = service.getStreak(childId);

        assertThat(response.continuousDays()).isEqualTo(1);
        assertThat(response.lastCompletedDate()).isEqualTo(yesterday);
        assertThat(response.todaySetCount()).isZero();
        assertThat(response.status()).isEqualTo(StreakStatus.ACTIVE.name());
        assertThat(response.recoveryAvailable()).isFalse();
        assertThat(response.partner()).isNull();
        verify(childActivityService).touchLastActiveAt(childId);
    }

    @Test
    void getStreakReturnsNullPartnerWhenNotConnected() {
        StreakService service = service();
        UUID childId = UUID.randomUUID();
        LocalDate today = KstDateUtils.today();
        StreakRecord streak = StreakRecord.create(childId);
        streak.recordMissionCompletion(today);
        ChildProfile child = org.mockito.Mockito.mock(ChildProfile.class);

        when(streakRecordRepository.findById(childId)).thenReturn(Optional.of(streak));
        when(childProfileRepository.findById(childId)).thenReturn(Optional.of(child));
        when(child.getShieldCount()).thenReturn(2);

        var response = service.getStreak(childId);

        assertThat(response.todaySetCount()).isEqualTo(1);
        assertThat(response.shieldCount()).isEqualTo(2);
        assertThat(response.partner()).isNull();
    }

    @Test
    void recoverableStreakContinuesWhenMissionCompletesBeforeDeadline() {
        UUID childId = UUID.randomUUID();
        LocalDate today = KstDateUtils.today();
        StreakRecord streak = StreakRecord.create(childId);
        streak.recordMissionCompletion(today.minusDays(2));
        streak.markRecoverable(today);

        streak.recordMissionCompletion(today);

        assertThat(streak.getContinuousDays()).isEqualTo(2);
        assertThat(streak.getTodayMissionCount()).isEqualTo(1);
        assertThat(streak.getStatus()).isEqualTo(StreakStatus.ACTIVE);
        assertThat(streak.getRecoveryDeadlineDate()).isNull();
    }

    @Test
    void protectedStreakContinuesWhenMissionCompletesAfterShieldUse() {
        UUID childId = UUID.randomUUID();
        LocalDate today = KstDateUtils.today();
        StreakRecord streak = StreakRecord.create(childId);
        streak.recordMissionCompletion(today.minusDays(2));
        streak.markProtectedByShield(today.minusDays(1));

        streak.recordMissionCompletion(today);

        assertThat(streak.getContinuousDays()).isEqualTo(2);
        assertThat(streak.getTodayMissionCount()).isEqualTo(1);
        assertThat(streak.getStatus()).isEqualTo(StreakStatus.ACTIVE);
    }

    @Test
    void useShieldConsumesShieldAndProtectsRecoverableStreak() {
        StreakService service = serviceWithCurrency();
        ParentAccount parent = ParentAccount.create("parent-id", "parent@example.com");
        ChildProfile child = ChildProfile.create(parent, "child", "123456");
        child.addShield(1);
        LocalDate today = KstDateUtils.today();
        StreakRecord streak = StreakRecord.create(child.getId());
        streak.recordMissionCompletion(today.minusDays(2));
        streak.markRecoverable(today);

        when(streakRecordRepository.findWithLockByChildId(child.getId())).thenReturn(Optional.of(streak));
        when(childProfileRepository.findWithLockById(child.getId())).thenReturn(Optional.of(child));

        var response = service.useShield(child.getId());

        assertThat(response.shieldCount()).isZero();
        assertThat(response.status()).isEqualTo(StreakStatus.PROTECTED.name());
        assertThat(response.continuousDays()).isEqualTo(1);
        assertThat(response.recoveryAvailable()).isFalse();
        assertThat(response.lastShieldUsedDate()).isEqualTo(today.minusDays(1));
        verify(currencyTransactionRepository).save(any());
    }

    @Test
    void getStreakReturnsConnectedPartnerStatus() {
        StreakService service = service();
        UUID childId = UUID.randomUUID();
        UUID partnerChildId = UUID.randomUUID();
        LocalDate today = KstDateUtils.today();
        StreakRecord streak = StreakRecord.create(childId);
        streak.recordMissionCompletion(today);
        StreakRecord partnerStreak = StreakRecord.create(partnerChildId);
        partnerStreak.recordMissionCompletion(today);
        ChildProfile child = org.mockito.Mockito.mock(ChildProfile.class);
        ChildProfile partner = org.mockito.Mockito.mock(ChildProfile.class);

        when(streakRecordRepository.findById(childId)).thenReturn(Optional.of(streak));
        when(streakRecordRepository.findById(partnerChildId)).thenReturn(Optional.of(partnerStreak));
        when(childProfileRepository.findById(childId)).thenReturn(Optional.of(child));
        when(childProfileRepository.findById(partnerChildId)).thenReturn(Optional.of(partner));
        when(friendStreakRepository.findById(childId)).thenReturn(Optional.of(friendStreak(childId, partnerChildId)));
        when(partner.getId()).thenReturn(partnerChildId);
        when(partner.getNickname()).thenReturn("partner");

        var response = service.getStreak(childId);

        assertThat(response.partner()).isNotNull();
        assertThat(response.partner().childId()).isEqualTo(partnerChildId);
        assertThat(response.partner().nickname()).isEqualTo("partner");
        assertThat(response.partner().todayCompleted()).isTrue();
    }

    @Test
    void connectPartnerCreatesSymmetricRows() {
        StreakService service = service();
        UUID childId = UUID.randomUUID();
        UUID partnerChildId = UUID.randomUUID();
        ChildProfile child = org.mockito.Mockito.mock(ChildProfile.class);
        ChildProfile partner = org.mockito.Mockito.mock(ChildProfile.class);

        when(childProfileRepository.findByCode("123456")).thenReturn(Optional.of(partner));
        when(partner.getId()).thenReturn(partnerChildId);
        when(partner.getNickname()).thenReturn("partner");
        when(childProfileRepository.findWithLockById(childId)).thenReturn(Optional.of(child));
        when(childProfileRepository.findWithLockById(partnerChildId)).thenReturn(Optional.of(partner));
        when(friendStreakRepository.existsById(childId)).thenReturn(false);
        when(friendStreakRepository.existsByPartnerChildId(childId)).thenReturn(false);
        when(friendStreakRepository.existsById(partnerChildId)).thenReturn(false);
        when(friendStreakRepository.existsByPartnerChildId(partnerChildId)).thenReturn(false);

        var response = service.connectPartner(childId, "123456");

        assertThat(response.partner().childId()).isEqualTo(partnerChildId);
        assertThat(response.partner().nickname()).isEqualTo("partner");
        verify(friendStreakRepository, times(2)).save(any(FriendStreak.class));
        verify(childActivityService).touchLastActiveAt(childId);
    }

    @Test
    void connectPartnerRejectsOwnCode() {
        StreakService service = service();
        UUID childId = UUID.randomUUID();
        ChildProfile partner = org.mockito.Mockito.mock(ChildProfile.class);

        when(childProfileRepository.findByCode("123456")).thenReturn(Optional.of(partner));
        when(partner.getId()).thenReturn(childId);

        assertThatThrownBy(() -> service.connectPartner(childId, "123456"))
                .isInstanceOf(AimongException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    void disconnectPartnerDeletesSymmetricRows() {
        StreakService service = service();
        UUID childId = UUID.randomUUID();
        UUID partnerChildId = UUID.randomUUID();
        when(friendStreakRepository.findById(childId)).thenReturn(Optional.of(friendStreak(childId, partnerChildId)));

        var response = service.disconnectPartner(childId);

        assertThat(response.disconnected()).isTrue();
        verify(friendStreakRepository).deleteByChildIdOrPartnerChildId(childId, childId);
        verify(childActivityService).touchLastActiveAt(childId);
    }

    @Test
    void purchaseShieldsConsumesGearAndAddsShield() {
        StreakService service = serviceWithCurrency();
        ChildProfile child = ChildProfile.create(ParentAccount.create("parent-id", "parent@example.com"), "child", "123456");
        child.addGear(60);

        when(childProfileRepository.findWithLockById(child.getId())).thenReturn(Optional.of(child));

        var response = service.purchaseShields(child.getId(), 2);

        assertThat(response.shieldCount()).isEqualTo(2);
        assertThat(response.purchasedCount()).isEqualTo(2);
        assertThat(response.unitCost()).isEqualTo(30);
        assertThat(response.gearBalance()).isZero();
        verify(currencyTransactionRepository).save(any());
    }

    private StreakService service() {
        return new StreakService(
                streakRecordRepository,
                friendStreakRepository,
                childProfileRepository,
                childActivityService
        );
    }

    private StreakService serviceWithCurrency() {
        return new StreakService(
                streakRecordRepository,
                friendStreakRepository,
                childProfileRepository,
                childActivityService,
                new CurrencyService(currencyTransactionRepository)
        );
    }

    private FriendStreak friendStreak(UUID childId, UUID partnerChildId) {
        try {
            Constructor<FriendStreak> constructor = FriendStreak.class.getDeclaredConstructor(
                    UUID.class,
                    UUID.class,
                    Instant.class
            );
            constructor.setAccessible(true);
            return constructor.newInstance(childId, partnerChildId, Instant.now());
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
