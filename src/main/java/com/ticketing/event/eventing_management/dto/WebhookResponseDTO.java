package com.ticketing.event.eventing_management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta del webhook WooCommerce (exito, ignorado o error en payload)")
public class WebhookResponseDTO {
    private String status;
    private String message;
    private String ticketId;
}
