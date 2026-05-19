package com.ticketing.event.eventing_management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Eco de filtros usados en GET /api/events")
public class EventFiltersDTO {
    private String category;
    private String date;
    private boolean includePast;
    private boolean includeInactive;
}
