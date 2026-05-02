package com.ticketing.event.eventing_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceDTO {
    private UUID eventId;
    private String eventName;
    private Long sold;
    private Long validated;
    private Long available;
    private Integer total;
    private double occupancyPercentage;
}
