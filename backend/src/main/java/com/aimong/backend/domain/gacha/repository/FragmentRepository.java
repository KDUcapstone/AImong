package com.aimong.backend.domain.gacha.repository;

import com.aimong.backend.domain.gacha.entity.Fragment;
import com.aimong.backend.domain.gacha.entity.FragmentId;
import com.aimong.backend.domain.pet.entity.PetGrade;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface FragmentRepository extends JpaRepository<Fragment, FragmentId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Fragment> findWithLockByChildIdAndGrade(UUID childId, PetGrade grade);
}
