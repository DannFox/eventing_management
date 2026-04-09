package com.ticketing.event.eventing_management.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false)
    private String attendeeEmail;

    private String attendeeName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    @Column(unique = true, nullable = false)
    private String qrCode; // Base64 del UUID

    private String seatInfo;

    @Column(updatable = false)
    private LocalDateTime purchasedAt;

    private LocalDateTime validatedAt;

    @PrePersist
    protected void onCreate() {
        purchasedAt = LocalDateTime.now();
        status = TicketStatus.ACTIVE;
        qrCode = id.toString(); // Base64 real se genera en el servicio
    }

    public enum TicketStatus {
        ACTIVE, USED, CANCELLED
    }
}
