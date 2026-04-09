package com.ticketing.event.eventing_management.service;

import com.ticketing.event.eventing_management.dto.CapacityDto;
import com.ticketing.event.eventing_management.dto.EventDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EventService {
    Page<EventDto> getActiveEvents(String category, Pageable pageable);
    CapacityDto getEventCapacity(Long eventId);
    EventDto getEventById(Long id);
}
