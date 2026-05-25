package com.aimong.backend.domain.customquest.repository;

import com.aimong.backend.domain.customquest.entity.ParentCustomQuest;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParentCustomQuestRepository extends JpaRepository<ParentCustomQuest, UUID> {

    @Query(value = """
            select count(*)
            from public.parent_custom_quests q
            where q.child_id = :childId
              and cast(q.status as text) in (:statuses)
            """, nativeQuery = true)
    long countByChildIdAndStatusNames(
            @Param("childId") UUID childId,
            @Param("statuses") Collection<String> statuses
    );

    Optional<ParentCustomQuest> findByIdAndParentAccountParentId(UUID id, String parentId);

    Optional<ParentCustomQuest> findByIdAndChildProfileId(UUID id, UUID childId);

    @Query(
            value = """
                    select *
                    from public.parent_custom_quests q
                    where q.parent_id = :parentId
                      and q.child_id = :childId
                      and cast(q.status as text) in (:statuses)
                    order by coalesce(q.confirmed_at, q.completed_at, q.created_at) desc,
                             q.created_at desc,
                             q.id desc
                    """,
            countQuery = """
                    select count(*)
                    from public.parent_custom_quests q
                    where q.parent_id = :parentId
                      and q.child_id = :childId
                      and cast(q.status as text) in (:statuses)
                    """,
            nativeQuery = true
    )
    Page<ParentCustomQuest> findParentQuests(
            @Param("parentId") String parentId,
            @Param("childId") UUID childId,
            @Param("statuses") Collection<String> statuses,
            Pageable pageable
    );

    @Query(value = """
            select *
            from public.parent_custom_quests q
            where q.child_id = :childId
              and (
                cast(q.status as text) in (:openStatuses)
                or (cast(q.status as text) in (:confirmedStatuses) and q.confirmed_at >= :confirmedSince)
              )
            order by coalesce(q.confirmed_at, q.completed_at, q.created_at) desc,
                     q.created_at desc,
                     q.id desc
            """, nativeQuery = true)
    List<ParentCustomQuest> findVisibleChildQuests(
            @Param("childId") UUID childId,
            @Param("openStatuses") Collection<String> openStatuses,
            @Param("confirmedStatuses") Collection<String> confirmedStatuses,
            @Param("confirmedSince") Instant confirmedSince
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update public.parent_custom_quests
            set status = 'EXPIRED',
                updated_at = :now
            where status = 'ACTIVE'
              and expires_at < :now
            """, nativeQuery = true)
    int expireActiveBefore(@Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update public.parent_custom_quests
            set status = 'AUTO_CONFIRMED',
                confirmed_at = :now,
                updated_at = :now
            where status = 'PENDING_CONFIRM'
              and completed_at <= :threshold
            """, nativeQuery = true)
    int autoConfirmPendingBefore(
            @Param("threshold") Instant threshold,
            @Param("now") Instant now
    );
}
