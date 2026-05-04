package com.aimong.backend.domain.chat.repository;

import com.aimong.backend.domain.chat.entity.ChatUsage;
import com.aimong.backend.domain.chat.entity.ChatUsageId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatUsageRepository extends JpaRepository<ChatUsage, ChatUsageId> {
}
