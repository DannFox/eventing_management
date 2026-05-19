package com.ticketing.event.eventing_management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Resultado de validacion en puerta (primera vez o ya validado)")
public class TicketValidationResponseDTO {
    private UUID ticketId;
    private UUID eventId;
    private String status;
    private LocalDateTime validatedAt;
    private String message;
}
