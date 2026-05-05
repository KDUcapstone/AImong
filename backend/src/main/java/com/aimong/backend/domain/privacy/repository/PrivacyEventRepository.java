package com.aimong.backend.domain.privacy.repository;

import com.aimong.backend.domain.privacy.entity.PrivacyEvent;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivacyEventRepository extends JpaRepository<PrivacyEvent, UUID> {

    Page<PrivacyEvent> findByChildId(UUID childId, Pageable pageable);

    long countByChildIdAndDetectedAtGreaterThanEqual(UUID childId, Instant detectedAt);
}
