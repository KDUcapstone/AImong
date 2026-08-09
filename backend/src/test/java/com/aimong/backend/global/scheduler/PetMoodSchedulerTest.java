package com.aimong.backend.global.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.aimong.backend.domain.pet.entity.Pet;
import com.aimong.backend.domain.pet.entity.PetGrade;
import com.aimong.backend.domain.pet.entity.PetMood;
import com.aimong.backend.domain.pet.repository.PetRepository;
import com.aimong.backend.domain.streak.entity.StreakRecord;
import com.aimong.backend.domain.streak.repository.StreakRecordRepository;
import com.aimong.backend.global.util.KstDateUtils;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PetMoodSchedulerTest {

    @Mock
    private PetRepository petRepository;

    @Mock
    private StreakRecordRepository streakRecordRepository;

    @InjectMocks
    private PetMoodScheduler petMoodScheduler;

    @Test
    void updatePetMoodsUsesLastCompletedDate() {
        UUID happyChildId = UUID.randomUUID();
        UUID sadChildId = UUID.randomUUID();
        Pet happyPet = Pet.create(happyChildId, "RABBIT", PetGrade.NORMAL);
        Pet sadPet = Pet.create(sadChildId, "FOX", PetGrade.RARE);
        LocalDate today = KstDateUtils.today();
        StreakRecord happyRecord = StreakRecord.create(happyChildId);
        happyRecord.recordMissionCompletion(today);
        StreakRecord sadRecord = StreakRecord.create(sadChildId);
        sadRecord.recordMissionCompletion(today.minusDays(2));

        when(petRepository.findAll()).thenReturn(List.of(happyPet, sadPet));
        when(streakRecordRepository.findById(happyChildId)).thenReturn(Optional.of(happyRecord));
        when(streakRecordRepository.findById(sadChildId)).thenReturn(Optional.of(sadRecord));

        petMoodScheduler.updatePetMoods();

        assertThat(happyPet.getMood()).isEqualTo(PetMood.HAPPY);
        assertThat(sadPet.getMood()).isEqualTo(PetMood.SAD_DEEP);
    }
}
