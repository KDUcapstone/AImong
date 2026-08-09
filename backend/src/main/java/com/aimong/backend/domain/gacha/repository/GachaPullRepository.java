package com.aimong.backend.domain.gacha.repository;

import com.aimong.backend.domain.gacha.entity.GachaPull;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GachaPullRepository extends JpaRepository<GachaPull, UUID> {
}
