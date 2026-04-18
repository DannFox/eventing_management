package com.ticketing.event.eventing_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventListResponseDTO {
    private List<EventDTO> events;
    private PaginationDTO pagination;
    private EventFiltersDTO filters;
}
