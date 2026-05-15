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

    boolean existsByChildIdAndSetIdAndCompletedTrue(UUID childId, String setId);

    default boolean existsByChildIdAndSetId(UUID childId, String setId) {
        return existsByChildIdAndSetIdAndCompletedTrue(childId, setId);
    }

    Optional<MissionSetProgress> findByChildIdAndSetIdAndCompletedTrue(UUID childId, String setId);

    default Optional<MissionSetProgress> findByChildIdAndSetId(UUID childId, String setId) {
        return findByChildIdAndSetIdAndCompletedTrue(childId, setId);
    }

    List<MissionSetProgress> findAllByChildIdAndSetIdInAndCompletedTrue(UUID childId, Collection<String> setIds);

    default List<MissionSetProgress> findAllByChildIdAndSetIdIn(UUID childId, Collection<String> setIds) {
        return findAllByChildIdAndSetIdInAndCompletedTrue(childId, setIds);
    }

    long countByChildIdAndCompletedTrue(UUID childId);

    default long countByChildId(UUID childId) {
        return countByChildIdAndCompletedTrue(childId);
    }

    long countByChildIdAndSetIdInAndCompletedTrue(UUID childId, Collection<String> setIds);

    default long countByChildIdAndSetIdIn(UUID childId, Collection<String> setIds) {
        return countByChildIdAndSetIdInAndCompletedTrue(childId, setIds);
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MissionSetProgress> findWithLockByChildIdAndSetIdAndCompletedTrue(UUID childId, String setId);

    default Optional<MissionSetProgress> findWithLockByChildIdAndSetId(UUID childId, String setId) {
        return findWithLockByChildIdAndSetIdAndCompletedTrue(childId, setId);
    }
}
