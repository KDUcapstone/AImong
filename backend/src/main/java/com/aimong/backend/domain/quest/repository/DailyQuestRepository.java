package com.aimong.backend.domain.quest.repository;

import com.aimong.backend.domain.quest.entity.DailyQuest;
import com.aimong.backend.domain.quest.entity.DailyQuestType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface DailyQuestRepository extends JpaRepository<DailyQuest, UUID> {

    Optional<DailyQuest> findByChildIdAndQuestDateAndQuestType(UUID childId, LocalDate questDate, DailyQuestType questType);

    List<DailyQuest> findAllByChildIdAndQuestDate(UUID childId, LocalDate questDate);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DailyQuest> findWithLockByChildIdAndQuestDateAndQuestType(UUID childId, LocalDate questDate, DailyQuestType questType);
}
