package com.ticketing.event.eventing_management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Aforo: vendidos, disponibles y total (Grupo B / app asistente)")
public class CapacityDTO {
    private UUID eventId;
    private Long sold;
    private Long available;
    private Integer total;
}
