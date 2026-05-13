package com.aimong.backend.domain.mission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import com.aimong.backend.domain.auth.entity.ParentAccount;
import com.aimong.backend.domain.auth.repository.ChildProfileRepository;
import com.aimong.backend.domain.auth.service.ChildActivityService;
import com.aimong.backend.domain.mission.dto.ReviveAttemptRequest;
import com.aimong.backend.domain.mission.entity.QuizAttempt;
import com.aimong.backend.domain.mission.repository.MissionRepository;
import com.aimong.backend.domain.mission.repository.MissionSetProgressRepository;
import com.aimong.backend.domain.mission.repository.MissionSetRepository;
import com.aimong.backend.domain.mission.repository.QuizAttemptRepository;
import com.aimong.backend.domain.reward.repository.CurrencyTransactionRepository;
import com.aimong.backend.domain.reward.service.CurrencyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QuizAttemptServiceTest {

    @Mock private MissionRepository missionRepository;
    @Mock private MissionSetRepository missionSetRepository;
    @Mock private MissionSetProgressRepository missionSetProgressRepository;
    @Mock private QuizAttemptRepository quizAttemptRepository;
    @Mock private ChildProfileRepository childProfileRepository;
    @Mock private ChildActivityService childActivityService;
    @Mock private MissionService missionService;
    @Mock private QuizService quizService;
    @Mock private CurrencyTransactionRepository currencyTransactionRepository;

    @Test
    void reviveConsumesGearAndRestoresLives() {
        QuizAttemptService service = service();
        UUID childId = UUID.randomUUID();
        UUID missionId = UUID.randomUUID();
        QuizAttempt attempt = QuizAttempt.create(
                childId,
                missionId,
                "S0101-L1",
                1,
                "[]",
                Instant.now().plusSeconds(600),
                false
        );
        attempt.recordWrongAnswer();
        attempt.recordWrongAnswer();
        attempt.recordWrongAnswer();
        ChildProfile child = ChildProfile.create(ParentAccount.create("parent-id", "parent@example.com"), "child", "123456");
        child.addGear(20);

        when(quizAttemptRepository.findWithLockById(attempt.getId())).thenReturn(Optional.of(attempt));
        when(childProfileRepository.findWithLockById(childId)).thenReturn(Optional.of(child));

        var response = service.revive(childId, attempt.getId(), new ReviveAttemptRequest(true));

        assertThat(response.remainingLives()).isEqualTo(3);
        assertThat(response.reviveCount()).isEqualTo(1);
        assertThat(response.reviveCost()).isEqualTo(10);
        assertThat(response.gearBalance()).isEqualTo(10);
        verify(currencyTransactionRepository).save(org.mockito.ArgumentMatchers.any());
        verify(childActivityService).touchLastActiveAt(childId);
    }

    private QuizAttemptService service() {
        return new QuizAttemptService(
                missionRepository,
                missionSetRepository,
                missionSetProgressRepository,
                quizAttemptRepository,
                childProfileRepository,
                childActivityService,
                missionService,
                quizService,
                new CurrencyService(currencyTransactionRepository),
                new ObjectMapper()
        );
    }
}
