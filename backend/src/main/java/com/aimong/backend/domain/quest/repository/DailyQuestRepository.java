package com.aimong.backend.domain.quest.repository;

import com.aimong.backend.domain.quest.entity.DailyQuest;
import com.aimong.backend.domain.quest.entity.DailyQuestType;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyQuestRepository extends JpaRepository<DailyQuest, UUID> {

    Optional<DailyQuest> findByChildIdAndQuestDateAndQuestType(UUID childId, LocalDate questDate, DailyQuestType questType);
}
