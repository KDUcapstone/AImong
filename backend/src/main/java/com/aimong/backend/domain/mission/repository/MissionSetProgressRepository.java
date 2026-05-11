package com.aimong.backend.domain.mission.repository;

import com.aimong.backend.domain.mission.entity.MissionSetProgress;
import com.aimong.backend.domain.mission.entity.MissionSetProgress.MissionSetProgressId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface MissionSetProgressRepository extends JpaRepository<MissionSetProgress, MissionSetProgressId> {

    boolean existsByChildIdAndSetId(UUID childId, String setId);

    Optional<MissionSetProgress> findByChildIdAndSetId(UUID childId, String setId);

    List<MissionSetProgress> findAllByChildIdAndSetIdIn(UUID childId, Collection<String> setIds);

    long countByChildId(UUID childId);

    long countByChildIdAndSetIdIn(UUID childId, Collection<String> setIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MissionSetProgress> findWithLockByChildIdAndSetId(UUID childId, String setId);
}
