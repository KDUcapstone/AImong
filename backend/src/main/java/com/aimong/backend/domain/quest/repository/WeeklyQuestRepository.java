package com.aimong.backend.domain.quest.repository;

import com.aimong.backend.domain.quest.entity.WeeklyQuest;
import com.aimong.backend.domain.quest.entity.WeeklyQuestType;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WeeklyQuestRepository extends JpaRepository<WeeklyQuest, UUID> {

    Optional<WeeklyQuest> findByChildIdAndWeekStartAndQuestType(UUID childId, LocalDate weekStart, WeeklyQuestType questType);
}
