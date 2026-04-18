package com.ticketing.event.eventing_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventFiltersDTO {
    private String category;
    private boolean includePast;
    private boolean includeInactive;
}
