package com.ticketing.event.eventing_management.service;

import com.ticketing.event.eventing_management.dto.AttendanceDTO;
import com.ticketing.event.eventing_management.dto.CapacityDTO;
import com.ticketing.event.eventing_management.dto.EventDTO;
import com.ticketing.event.eventing_management.dto.EventListResponseDTO;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface EventService {
    EventListResponseDTO getEvents(String category, String date, boolean includePast, boolean includeInactive, Pageable pageable);
    CapacityDTO getEventCapacity(UUID eventId);
    AttendanceDTO getEventAttendance(UUID eventId);
    EventDTO getEventById(UUID id);
}
