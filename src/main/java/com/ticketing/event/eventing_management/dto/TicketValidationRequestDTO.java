package com.ticketing.event.eventing_management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Validar ingreso: enviar ticketId o qrCode (base64 del UUID). eventId opcional para comprobar evento.")
public class TicketValidationRequestDTO {
    private UUID ticketId;
    @Schema(description = "Base64 del UUID del ticket, alternativa a ticketId")
    private String qrCode;
    @Schema(description = "Si se envia, debe coincidir con el evento del ticket")
    private UUID eventId;
}
