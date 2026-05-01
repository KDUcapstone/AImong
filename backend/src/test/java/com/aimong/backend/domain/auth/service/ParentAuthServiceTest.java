package com.aimong.backend.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.auth.dto.ParentRegisterRequest;
import com.aimong.backend.domain.auth.dto.ParentRegisterResponse;
import com.aimong.backend.domain.auth.entity.ParentAccount;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.auth.repository.ParentAccountRepository;
import com.aimong.backend.domain.gacha.repository.TicketRepository;
import com.aimong.backend.domain.gacha.service.GachaPullService;
import com.aimong.backend.domain.streak.repository.StreakRecordRepository;
import com.aimong.backend.global.exception.AimongException;
import com.aimong.backend.global.exception.ErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParentAuthServiceTest {

    @Mock
    private FirebaseAuth firebaseAuth;

    @Mock
    private ParentAccountRepository parentAccountRepository;

    @Mock
    private ChildProfileRepository childProfileRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private StreakRecordRepository streakRecordRepository;

    @Mock
    private GachaPullService gachaPullService;

    @Mock
    private FirebaseToken firebaseToken;

    @InjectMocks
    private ParentAuthService parentAuthService;

    @Test
    void registerCreatesChildAndStarterResources() throws Exception {
        when(firebaseAuth.verifyIdToken("valid-token")).thenReturn(firebaseToken);
        when(firebaseToken.getUid()).thenReturn("firebase-uid");
        when(firebaseToken.getEmail()).thenReturn("parent@example.com");
        when(firebaseToken.getClaims()).thenReturn(Map.of("firebase", Map.of("sign_in_provider", "google.com")));
        when(parentAccountRepository.findByFirebaseUid("firebase-uid")).thenReturn(Optional.empty());
        when(parentAccountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(childProfileRepository.existsByCode(any())).thenReturn(false);
        when(childProfileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(ticketRepository.saveAll(any(Iterable.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(streakRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ParentRegisterResponse response = parentAuthService.register(
                "Bearer valid-token",
                new ParentRegisterRequest("민준")
        );

        assertThat(response.nickname()).isEqualTo("민준");
        assertThat(response.code()).matches("\\d{6}");
        assertThat(response.starterTickets()).isEqualTo(3);
        verify(ticketRepository).saveAll(any(Iterable.class));
        verify(streakRecordRepository).save(any());
        verify(gachaPullService).initializeStarterOnboarding(any());
    }

    @Test
    void regenerateCodeRejectsInvalidChildIdFormat() throws Exception {
        when(firebaseAuth.verifyIdToken("valid-token")).thenReturn(firebaseToken);
        when(firebaseToken.getUid()).thenReturn("firebase-uid");
        when(firebaseToken.getClaims()).thenReturn(Map.of("firebase", Map.of("sign_in_provider", "google.com")));
        when(parentAccountRepository.findByFirebaseUid("firebase-uid"))
                .thenReturn(Optional.of(ParentAccount.create("firebase-uid", "parent@example.com")));

        assertThatThrownBy(() -> parentAuthService.regenerateCode("Bearer valid-token", "not-a-uuid"))
                .isInstanceOf(AimongException.class)
                .extracting(exception -> ((AimongException) exception).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }
}
