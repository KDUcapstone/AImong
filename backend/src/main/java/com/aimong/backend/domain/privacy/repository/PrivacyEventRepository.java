package com.aimong.backend.domain.privacy.repository;

import com.aimong.backend.domain.privacy.entity.PrivacyEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivacyEventRepository extends JpaRepository<PrivacyEvent, UUID> {
}
