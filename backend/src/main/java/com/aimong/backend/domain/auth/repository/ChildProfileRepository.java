package com.aimong.backend.domain.auth.repository;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface ChildProfileRepository extends JpaRepository<ChildProfile, UUID> {

    Optional<ChildProfile> findByCode(String code);

    boolean existsByCode(String code);

    List<ChildProfile> findAllByParentAccountIdOrderByCreatedAtAsc(UUID parentAccountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ChildProfile> findWithLockById(UUID id);
}
