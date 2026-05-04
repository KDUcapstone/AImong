package com.aimong.backend.domain.gacha.repository;

import com.aimong.backend.domain.gacha.entity.Ticket;
import com.aimong.backend.domain.gacha.entity.TicketType;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    long countByChildIdAndTicketTypeAndUsedAtIsNull(UUID childId, TicketType ticketType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Ticket> findFirstByChildIdAndTicketTypeAndUsedAtIsNullOrderByCreatedAtAsc(UUID childId, TicketType ticketType);
}
