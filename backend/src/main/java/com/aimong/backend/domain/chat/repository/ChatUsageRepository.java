package com.aimong.backend.domain.chat.repository;

import com.aimong.backend.domain.chat.entity.ChatUsage;
import com.aimong.backend.domain.chat.entity.ChatUsageId;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatUsageRepository extends JpaRepository<ChatUsage, ChatUsageId> {

    Optional<ChatUsage> findByChildIdAndUsageDate(UUID childId, LocalDate usageDate);

    @Query("""
            select coalesce(sum(c.count), 0)
            from ChatUsage c
            where c.childId = :childId
              and c.usageDate between :startDate and :endDate
            """)
    long sumCountByChildIdAndUsageDateBetween(
            @Param("childId") UUID childId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
