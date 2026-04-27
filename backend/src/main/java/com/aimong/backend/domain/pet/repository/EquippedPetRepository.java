package com.aimong.backend.domain.pet.repository;

import com.aimong.backend.domain.pet.entity.EquippedPet;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface EquippedPetRepository extends JpaRepository<EquippedPet, UUID> {

    Optional<EquippedPet> findByChildId(UUID childId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EquippedPet> findWithLockByChildId(UUID childId);
}
