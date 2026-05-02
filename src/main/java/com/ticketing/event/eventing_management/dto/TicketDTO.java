package com.ticketing.event.eventing_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketDTO {
    private UUID ticketId;
    private UUID eventId;
    private String eventName;
    private UUID attendeeId;
    private String attendeeEmail;
    private String attendeeName;
    private String status;
    private String qrCode;
    private String seatInfo;
    private LocalDateTime purchasedAt;
    private LocalDateTime validatedAt;
}
