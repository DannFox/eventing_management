package com.ticketing.event.eventing_management.service;

import com.ticketing.event.eventing_management.dto.CapacityDTO;
import com.ticketing.event.eventing_management.dto.EventDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EventService {
    Page<EventDTO> getActiveEvents(String category, Pageable pageable);
    CapacityDTO getEventCapacity(Long eventId);
    EventDTO getEventById(Long id);
}
