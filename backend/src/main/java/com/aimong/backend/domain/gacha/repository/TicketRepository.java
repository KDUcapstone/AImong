package com.aimong.backend.domain.gacha.repository;

import com.aimong.backend.domain.gacha.entity.Ticket;
import com.aimong.backend.domain.gacha.entity.TicketType;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    long countByChildIdAndTicketTypeAndUsedAtIsNull(UUID childId, TicketType ticketType);
}
