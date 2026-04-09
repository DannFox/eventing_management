package com.ticketing.event.eventing_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CapacityDto {
    private Long eventId;
    private Long sold;
    private Long available;
    private Integer total;
}
