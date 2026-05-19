package com.ticketing.event.eventing_management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Listado paginado de eventos con filtros aplicados")
public class EventListResponseDTO {
    private List<EventDTO> events;
    private PaginationDTO pagination;
    private EventFiltersDTO filters;
}
