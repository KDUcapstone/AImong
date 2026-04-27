package com.aimong.backend.domain.pet.repository;

import com.aimong.backend.domain.pet.entity.Pet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface PetRepository extends JpaRepository<Pet, UUID> {

    List<Pet> findByChildId(UUID childId);

    boolean existsByChildIdAndPetType(UUID childId, String petType);

    Optional<Pet> findByIdAndChildId(UUID id, UUID childId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Pet> findWithLockByIdAndChildId(UUID id, UUID childId);
}
