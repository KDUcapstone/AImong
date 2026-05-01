package com.aimong.backend.domain.gacha.repository;

import com.aimong.backend.domain.gacha.entity.Fragment;
import com.aimong.backend.domain.gacha.entity.FragmentId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FragmentRepository extends JpaRepository<Fragment, FragmentId> {
}
