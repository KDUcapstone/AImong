package com.aimong.backend.domain.gacha.repository;

import com.aimong.backend.domain.gacha.entity.Ticket;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<Ticket> findWithLockByChildId(UUID childId);
}
