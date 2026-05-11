package com.aimong.backend.domain.auth.repository;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ChildProfileRepository extends JpaRepository<ChildProfile, UUID> {

    Optional<ChildProfile> findByCodeAndDeletedAtIsNull(String code);

    default Optional<ChildProfile> findByCode(String code) {
        return findByCodeAndDeletedAtIsNull(code);
    }

    boolean existsByCodeAndDeletedAtIsNull(String code);

    default boolean existsByCode(String code) {
        return existsByCodeAndDeletedAtIsNull(code);
    }

    long countByParentAccountParentIdAndDeletedAtIsNull(String parentId);

    default long countByParentAccountParentId(String parentId) {
        return countByParentAccountParentIdAndDeletedAtIsNull(parentId);
    }

    List<ChildProfile> findAllByParentAccountParentIdAndDeletedAtIsNullOrderByCreatedAtAsc(String parentId);

    default List<ChildProfile> findAllByParentAccountParentIdOrderByCreatedAtAsc(String parentId) {
        return findAllByParentAccountParentIdAndDeletedAtIsNullOrderByCreatedAtAsc(parentId);
    }

    List<ChildProfile> findAllByDeletedAtIsNull();

    Optional<ChildProfile> findByIdAndDeletedAtIsNull(UUID id);

    List<ChildProfile> findAllByParentAccountParentId(String parentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ChildProfile> findWithLockById(UUID id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ChildProfile c
            set c.todayXp = 0,
                c.todayXpDate = :today
            where c.todayXp <> 0
               or c.todayXpDate is null
               or c.todayXpDate <> :today
            """)
    int resetDailyXp(java.time.LocalDate today);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ChildProfile c
            set c.weeklyXp = 0,
                c.weeklyXpWeekStart = :weekStart
            where c.weeklyXp <> 0
               or c.weeklyXpWeekStart is null
               or c.weeklyXpWeekStart <> :weekStart
            """)
    int resetWeeklyXp(java.time.LocalDate weekStart);
}
