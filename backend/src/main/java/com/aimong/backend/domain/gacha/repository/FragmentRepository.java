package com.aimong.backend.domain.gacha.repository;

import com.aimong.backend.domain.gacha.entity.Fragment;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface FragmentRepository extends JpaRepository<Fragment, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Fragment> findWithLockByChildId(UUID childId);

    Optional<Fragment> findByChildId(UUID childId);
}
