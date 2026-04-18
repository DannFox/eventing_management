package com.ticketing.event.eventing_management.service;

import com.ticketing.event.eventing_management.dto.CapacityDTO;
import com.ticketing.event.eventing_management.dto.EventDTO;
import com.ticketing.event.eventing_management.dto.EventListResponseDTO;
import org.springframework.data.domain.Pageable;

public interface EventService {
    EventListResponseDTO getEvents(String category, boolean includePast, boolean includeInactive, Pageable pageable);
    CapacityDTO getEventCapacity(Long eventId);
    EventDTO getEventById(Long id);
}
