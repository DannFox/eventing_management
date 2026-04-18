package com.ticketing.event.eventing_management.service.impl;

import com.ticketing.event.eventing_management.dto.CapacityDTO;
import com.ticketing.event.eventing_management.dto.EventDTO;
import com.ticketing.event.eventing_management.dto.EventFiltersDTO;
import com.ticketing.event.eventing_management.dto.EventListResponseDTO;
import com.ticketing.event.eventing_management.dto.PaginationDTO;
import com.ticketing.event.eventing_management.entity.Event;
import com.ticketing.event.eventing_management.entity.Ticket;
import com.ticketing.event.eventing_management.repository.EventRepository;
import com.ticketing.event.eventing_management.repository.TicketRepository;
import com.ticketing.event.eventing_management.service.EventService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
            key = "'events_' + (#category != null ? #category : 'all') + '_' + #includePast + '_' + #includeInactive + '_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort"
    )
    public EventListResponseDTO getEvents(String category, boolean includePast, boolean includeInactive, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        Pageable safePageable = sanitizePageable(pageable);
        Page<Event> events = eventRepository.findAll(buildEventSpecification(category, includePast, includeInactive, now), safePageable);
        Page<EventDTO> eventPage = events.map(this::mapToDto);
        return buildEventListResponse(eventPage, category, includePast, includeInactive);
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
                .status(resolveStatus(event))
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

    private Specification<Event> buildEventSpecification(String category, boolean includePast, boolean includeInactive, LocalDateTime now) {
        return (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<Predicate>();

            if (category != null && !category.isBlank()) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("category")), category.trim().toLowerCase()));
            }

            if (!includeInactive) {
                predicates.add(criteriaBuilder.isTrue(root.get("active")));
            }

            if (!includePast) {
                predicates.add(criteriaBuilder.greaterThan(root.get("eventDate"), now));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private EventListResponseDTO buildEventListResponse(Page<EventDTO> eventPage, String category, boolean includePast, boolean includeInactive) {
        return EventListResponseDTO.builder()
                .events(eventPage.getContent())
                .pagination(PaginationDTO.builder()
                        .page(eventPage.getNumber())
                        .size(eventPage.getSize())
                        .totalElements(eventPage.getTotalElements())
                        .totalPages(eventPage.getTotalPages())
                        .elementsOnPage(eventPage.getNumberOfElements())
                        .first(eventPage.isFirst())
                        .last(eventPage.isLast())
                        .sort(eventPage.getSort().toString())
                        .build())
                .filters(EventFiltersDTO.builder()
                        .category(category)
                        .includePast(includePast)
                        .includeInactive(includeInactive)
                        .build())
                .build();
    }

    private String resolveStatus(Event event) {
        if (!Boolean.TRUE.equals(event.getActive())) {
            return "INACTIVE";
        }

        if (event.getEventDate() != null && event.getEventDate().isBefore(LocalDateTime.now())) {
            return "PAST";
        }

        return "UPCOMING";
    }
}
