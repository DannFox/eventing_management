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
@Schema(description = "Detalle de ticket para wallet y consultas Grupo B")
public class TicketDTO {
    @Schema(description = "UUID del ticket")
    private UUID ticketId;
    @Schema(description = "UUID del evento")
    private UUID eventId;
    private String eventName;
    private UUID attendeeId;
    private String attendeeEmail;
    private String attendeeName;
    @Schema(description = "ACTIVE, USED o CANCELLED", example = "ACTIVE")
    private String status;
    @Schema(description = "QR: base64 del UUID del ticket (contrato compartido)")
    private String qrCode;
    private String seatInfo;
    private LocalDateTime purchasedAt;
    private LocalDateTime validatedAt;
}
