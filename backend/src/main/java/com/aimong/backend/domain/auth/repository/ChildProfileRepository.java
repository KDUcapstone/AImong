package com.aimong.backend.domain.auth.repository;

import com.aimong.backend.domain.auth.entity.ChildProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChildProfileRepository extends JpaRepository<ChildProfile, UUID> {

    Optional<ChildProfile> findByCode(String code);

    boolean existsByCode(String code);
}
