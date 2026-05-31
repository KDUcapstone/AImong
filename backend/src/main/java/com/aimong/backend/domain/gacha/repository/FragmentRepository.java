package com.aimong.backend.domain.gacha.repository;

import com.aimong.backend.domain.gacha.entity.Fragment;
import com.aimong.backend.domain.pet.entity.PetGrade;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface FragmentRepository extends JpaRepository<Fragment, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Fragment> findWithLockByChildIdAndGrade(UUID childId, PetGrade grade);

    Optional<Fragment> findByChildIdAndGrade(UUID childId, PetGrade grade);

    List<Fragment> findByChildId(UUID childId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Fragment> findWithLockByChildId(UUID childId);
}
