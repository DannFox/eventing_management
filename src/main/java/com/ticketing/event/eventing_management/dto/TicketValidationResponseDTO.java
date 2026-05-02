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
public class TicketValidationResponseDTO {
    private UUID ticketId;
    private UUID eventId;
    private String status;
    private LocalDateTime validatedAt;
    private String message;
}
