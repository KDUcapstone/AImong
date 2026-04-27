package com.aimong.backend.domain.streak.repository;

import com.aimong.backend.domain.streak.entity.FriendStreak;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FriendStreakRepository extends JpaRepository<FriendStreak, UUID> {
}
