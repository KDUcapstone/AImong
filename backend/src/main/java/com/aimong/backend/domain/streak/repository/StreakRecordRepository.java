package com.aimong.backend.domain.streak.repository;

import com.aimong.backend.domain.streak.entity.StreakRecord;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StreakRecordRepository extends JpaRepository<StreakRecord, UUID> {
}
