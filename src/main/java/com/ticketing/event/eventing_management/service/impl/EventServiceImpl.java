package com.ticketing.event.eventing_management.service.impl;

import com.ticketing.event.eventing_management.dto.CapacityDTO;
import com.ticketing.event.eventing_management.dto.EventDTO;
import com.ticketing.event.eventing_management.entity.Event;
import com.ticketing.event.eventing_management.entity.Ticket;
import com.ticketing.event.eventing_management.repository.EventRepository;
import com.ticketing.event.eventing_management.repository.TicketRepository;
import com.ticketing.event.eventing_management.service.EventService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private static final String SOLD_COUNTER_KEY_PREFIX = "event:sold:";
    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id",
            "title",
            "eventDate",
            "venue",
            "capacity",
            "category",
            "createdAt",
            "updatedAt"
    );

    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Cacheable(
            value = "events",
            key = "'active_' + (#category != null ? #category : 'all') + '_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort"
    )
    public Page<EventDTO> getActiveEvents(String category, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        Pageable safePageable = sanitizePageable(pageable);
        Page<Event> events;

        if (category != null && !category.isEmpty()) {
            events = eventRepository.findByActiveTrueAndEventDateAfterAndCategory(now, category, safePageable);
        } else {
            events = eventRepository.findByActiveTrueAndEventDateAfter(now, safePageable);
        }

        return events.map(this::mapToDto);
    }

    @Override
    public CapacityDTO getEventCapacity(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Evento no encontrado con id: " + eventId));

        String key = SOLD_COUNTER_KEY_PREFIX + eventId;
        String soldStr = redisTemplate.opsForValue().get(key);
        Long sold;

        if (soldStr != null) {
            sold = Long.parseLong(soldStr);
        } else {
            sold = ticketRepository.countByEventIdAndStatus(eventId, Ticket.TicketStatus.ACTIVE)
                    + ticketRepository.countByEventIdAndStatus(eventId, Ticket.TicketStatus.USED);
            redisTemplate.opsForValue().set(key, String.valueOf(sold));
        }

        long available = event.getCapacity() - sold;
        if (available < 0) {
            available = 0;
        }

        return CapacityDTO.builder()
                .eventId(eventId)
                .sold(sold)
                .available(available)
                .total(event.getCapacity())
                .build();
    }

    @Override
    public EventDTO getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Evento no encontrado con id: " + id));
        return mapToDto(event);
    }

    private EventDTO mapToDto(Event event) {
        return EventDTO.builder()
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

    private Pageable sanitizePageable(Pageable pageable) {
        Sort safeSort = pageable.getSort().stream()
                .filter(order -> ALLOWED_SORT_PROPERTIES.contains(order.getProperty()))
                .collect(
                        () -> Sort.by(Sort.Order.asc("eventDate")),
                        Sort::and,
                        Sort::and
                );

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), safeSort);
    }
}
