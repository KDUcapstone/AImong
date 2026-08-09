package com.aimong.backend.global.scheduler;

import com.aimong.backend.domain.pet.entity.Pet;
import com.aimong.backend.domain.pet.entity.PetMood;
import com.aimong.backend.domain.pet.repository.PetRepository;
import com.aimong.backend.domain.streak.repository.StreakRecordRepository;
import com.aimong.backend.global.util.KstDateUtils;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PetMoodScheduler {

    private final PetRepository petRepository;
    private final StreakRecordRepository streakRecordRepository;

    @Scheduled(cron = "0 1 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void updatePetMoods() {
        LocalDate today = KstDateUtils.today();
        for (Pet pet : petRepository.findAll()) {
            PetMood mood = streakRecordRepository.findById(pet.getChildId())
                    .map(record -> moodOf(record.getLastCompletedDate(), today))
                    .orElse(PetMood.IDLE);
            pet.updateMood(mood);
        }
    }

    private PetMood moodOf(LocalDate lastCompletedDate, LocalDate today) {
        if (lastCompletedDate == null) {
            return PetMood.IDLE;
        }
        if (lastCompletedDate.equals(today)) {
            return PetMood.HAPPY;
        }
        if (lastCompletedDate.equals(today.minusDays(1))) {
            return PetMood.SAD_LIGHT;
        }
        return PetMood.SAD_DEEP;
    }
}
