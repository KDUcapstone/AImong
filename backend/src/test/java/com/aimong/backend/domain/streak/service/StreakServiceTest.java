package com.aimong.backend.domain.streak.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.streak.entity.FriendStreak;
import com.aimong.backend.domain.streak.entity.StreakRecord;
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
        assertThat(response.todayMissionCount()).isZero();
        assertThat(response.partner()).isNull();
        verify(childActivityService).touchLastActiveAt(childId);
    }

    @Test
    void getStreakReturnsNullPartnerInMvp() {
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

        assertThat(response.todayMissionCount()).isEqualTo(1);
        assertThat(response.shieldCount()).isEqualTo(2);
        assertThat(response.partner()).isNull();
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

    private StreakService service() {
        return new StreakService(
                streakRecordRepository,
                friendStreakRepository,
                childProfileRepository,
                childActivityService
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
