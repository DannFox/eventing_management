package com.ticketing.event.eventing_management.repository;

import com.ticketing.event.eventing_management.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {
    long countByEventIdAndStatus(UUID eventId, Ticket.TicketStatus status);
    Optional<Ticket> findByQrCode(String qrCode);

    @Query("select t from Ticket t join fetch t.event where t.id = :ticketId")
    Optional<Ticket> findByIdWithEvent(UUID ticketId);
}
