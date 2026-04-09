package com.ticketing.event.eventing_management.service.impl;

import com.ticketing.event.eventing_management.dto.CapacityDto;
import com.ticketing.event.eventing_management.dto.EventDto;
import com.ticketing.event.eventing_management.entity.Event;
import com.ticketing.event.eventing_management.entity.Ticket;
import com.ticketing.event.eventing_management.repository.EventRepository;
import com.ticketing.event.eventing_management.repository.TicketRepository;
import com.ticketing.event.eventing_management.service.EventService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private static final String SOLD_COUNTER_KEY_PREFIX = "event:sold:";

    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final RedisTemplate<String, Long> redisTemplate;

    @Override
    @Cacheable(value = "events", key = "'active_' + (#category != null ? #category : 'all') + '_' + #pageable.pageNumber")
    public Page<EventDto> getActiveEvents(String category, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        Page<Event> events;

        if (category != null && !category.isEmpty()) {
            events = eventRepository.findByActiveTrueAndEventDateAfterAndCategory(now, category, pageable);
        } else {
            events = eventRepository.findByActiveTrueAndEventDateAfter(now, pageable);
        }

        return events.map(this::mapToDto);
    }

    @Override
    public CapacityDto getEventCapacity(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Evento no encontrado con id: " + eventId));

        String key = SOLD_COUNTER_KEY_PREFIX + eventId;
        Long sold = redisTemplate.opsForValue().get(key);

        if (sold == null) {
            sold = ticketRepository.countByEventIdAndStatus(eventId, Ticket.TicketStatus.ACTIVE)
                    + ticketRepository.countByEventIdAndStatus(eventId, Ticket.TicketStatus.USED);
            redisTemplate.opsForValue().set(key, sold);
        }

        long available = event.getCapacity() - sold;
        if (available < 0) {
            available = 0;
        }

        return CapacityDto.builder()
                .eventId(eventId)
                .sold(sold)
                .available(available)
                .total(event.getCapacity())
                .build();
    }

    @Override
    public EventDto getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Evento no encontrado con id: " + id));
        return mapToDto(event);
    }

    private EventDto mapToDto(Event event) {
        return EventDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .venue(event.getVenue())
                .capacity(event.getCapacity())
                .category(event.getCategory())
                .imageUrl(event.getImageUrl())
                .active(event.getActive())
                .build();
    }
}
