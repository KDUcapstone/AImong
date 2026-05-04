package com.aimong.backend.domain.quest.repository;

import com.aimong.backend.domain.quest.entity.WeeklyQuest;
import com.aimong.backend.domain.quest.entity.WeeklyQuestType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface WeeklyQuestRepository extends JpaRepository<WeeklyQuest, UUID> {

    Optional<WeeklyQuest> findByChildIdAndWeekStartAndQuestType(UUID childId, LocalDate weekStart, WeeklyQuestType questType);

    List<WeeklyQuest> findAllByChildIdAndWeekStart(UUID childId, LocalDate weekStart);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<WeeklyQuest> findWithLockByChildIdAndWeekStartAndQuestType(UUID childId, LocalDate weekStart, WeeklyQuestType questType);
}
