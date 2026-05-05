package com.aimong.backend.domain.mission.repository;

import com.aimong.backend.domain.mission.entity.MissionAnswerResult;
import com.aimong.backend.domain.parent.dto.ParentWeakPointResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MissionAnswerResultRepository extends JpaRepository<MissionAnswerResult, UUID> {

    @Query(
            value = """
                    select new com.aimong.backend.domain.parent.dto.ParentWeakPointResponse(
                        m.id,
                        m.title,
                        m.stage,
                        (sum(case when r.correct = false then 1 else 0 end) * 1.0) / count(r.id),
                        count(distinct r.attemptId)
                    )
                    from MissionAnswerResult r
                    join Mission m on m.id = r.missionId
                    where r.childId = :childId
                      and r.createdAt >= :since
                    group by m.id, m.title, m.stage
                    having count(r.id) > 0
                    order by (sum(case when r.correct = false then 1 else 0 end) * 1.0) / count(r.id) desc,
                             count(distinct r.attemptId) desc
                    """,
            countQuery = """
                    select count(distinct r.missionId)
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
}
