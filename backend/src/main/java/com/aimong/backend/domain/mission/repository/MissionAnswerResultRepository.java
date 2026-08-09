package com.aimong.backend.domain.mission.repository;

import com.aimong.backend.domain.mission.entity.MissionAnswerResult;
import com.aimong.backend.domain.parent.dto.ParentWeakPointResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MissionAnswerResultRepository extends JpaRepository<MissionAnswerResult, UUID> {

    List<MissionAnswerResult> findAllByChildIdAndAttemptIdOrderByCreatedAtAsc(UUID childId, UUID attemptId);

    @Query(
            value = """
                    select new com.aimong.backend.domain.parent.dto.ParentWeakPointResponse(
                        m.id,
                        m.title,
                        coalesce(ms.stage, m.stage),
                        (sum(case when r.correct = false then 1 else 0 end) * 1.0) / count(r.id),
                        count(distinct r.attemptId),
                        r.setId,
                        ms.title,
                        ms.starLevel,
                        case
                            when ms.starLevel = 1 then 'LOW'
                            when ms.starLevel = 2 then 'MEDIUM'
                            when ms.starLevel = 3 then 'HIGH'
                            else null
                        end,
                        ms.starLevel
                    )
                    from MissionAnswerResult r
                    join Mission m on m.id = r.missionId
                    left join MissionSet ms on ms.setId = r.setId
                    where r.childId = :childId
                      and r.createdAt >= :since
                    group by m.id, m.title, m.stage, r.setId, ms.title, ms.stage, ms.starLevel
                    having count(r.id) > 0
                    order by (sum(case when r.correct = false then 1 else 0 end) * 1.0) / count(r.id) desc,
                             count(distinct r.attemptId) desc
                    """,
            countQuery = """
                    select count(distinct coalesce(r.setId, cast(r.missionId as string)))
                    from MissionAnswerResult r
                    where r.childId = :childId
                      and r.createdAt >= :since
                    """
    )
    Page<ParentWeakPointResponse> findWeakPointsByChildId(
            @Param("childId") UUID childId,
            @Param("since") Instant since,
            Pageable pageable
    );

    @Query("""
            select distinct r.questionId
            from MissionAnswerResult r
            where r.childId = :childId
              and r.missionId = :missionId
              and r.review = false
            """)
    List<UUID> findNormalAttemptedQuestionIds(
            @Param("childId") UUID childId,
            @Param("missionId") UUID missionId
    );
}
