package com.ticketing.event.eventing_management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Modelo de documentacion OpenAPI; coincide con el JSON emitido por {@link com.ticketing.event.eventing_management.exception.GlobalExceptionHandler}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ApiErrorResponse", description = "Cuerpo estandar ante errores HTTP (timestamp, status, error, message)")
public class ApiErrorResponseDTO {

    @Schema(example = "2026-05-09T14:30:00")
    private String timestamp;

    @Schema(example = "404")
    private Integer status;

    @Schema(example = "Recurso no encontrado")
    private String error;

    @Schema(example = "Detalle legible del problema")
    private String message;
}
