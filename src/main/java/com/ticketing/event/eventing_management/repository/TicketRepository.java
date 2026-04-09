package com.ticketing.event.eventing_management.repository;

import com.ticketing.event.eventing_management.enity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    long countByEventIdAndStatus(Long eventId, Ticket.TicketStatus status);
}
