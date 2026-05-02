package com.ticketing.event.eventing_management.service.impl;

import com.ticketing.event.eventing_management.dto.AttendanceDTO;
import com.ticketing.event.eventing_management.dto.CapacityDTO;
import com.ticketing.event.eventing_management.dto.EventDTO;
import com.ticketing.event.eventing_management.dto.EventFiltersDTO;
import com.ticketing.event.eventing_management.dto.EventListResponseDTO;
import com.ticketing.event.eventing_management.dto.PaginationDTO;
import com.ticketing.event.eventing_management.entity.Event;
import com.ticketing.event.eventing_management.entity.Ticket;
import com.ticketing.event.eventing_management.exception.ResourceNotFoundException;
import com.ticketing.event.eventing_management.repository.EventRepository;
import com.ticketing.event.eventing_management.repository.TicketRepository;
import com.ticketing.event.eventing_management.service.EventService;
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
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

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
            value = "events_v2",
            key = "'events_' + (#category != null ? #category : 'all') + '_' + (#date != null ? #date : 'all') + '_' + #includePast + '_' + #includeInactive + '_' + #pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort"
    )
    public EventListResponseDTO getEvents(String category, String date, boolean includePast, boolean includeInactive, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();
        YearMonth yearMonth = parseYearMonth(date);
        Pageable safePageable = sanitizePageable(pageable);
        Page<Event> events = eventRepository.findAll(buildEventSpecification(category, yearMonth, includePast, includeInactive, now), safePageable);
        Page<EventDTO> eventPage = events.map(this::mapToDto);
        return buildEventListResponse(eventPage, category, date, includePast, includeInactive);
    }

    @Override
    public CapacityDTO getEventCapacity(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con id: " + eventId));

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
    public AttendanceDTO getEventAttendance(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con id: " + eventId));

        long active = ticketRepository.countByEventIdAndStatus(eventId, Ticket.TicketStatus.ACTIVE);
        long used = ticketRepository.countByEventIdAndStatus(eventId, Ticket.TicketStatus.USED);
        long sold = active + used;
        long available = Math.max(0, event.getCapacity() - sold);
        double occupancyPercentage = event.getCapacity() == 0
                ? 0.0
                : (used * 100.0) / event.getCapacity();

        return AttendanceDTO.builder()
                .eventId(eventId)
                .eventName(event.getTitle())
                .sold(sold)
                .validated(used)
                .available(available)
                .total(event.getCapacity())
                .occupancyPercentage(occupancyPercentage)
                .build();
    }

    @Override
    public EventDTO getEventById(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado con id: " + id));
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

    private Specification<Event> buildEventSpecification(String category, YearMonth date, boolean includePast, boolean includeInactive, LocalDateTime now) {
        return (root, query, criteriaBuilder) -> {
            var predicates = new ArrayList<Predicate>();

            if (category != null && !category.isBlank()) {
                predicates.add(criteriaBuilder.equal(criteriaBuilder.lower(root.get("category")), category.trim().toLowerCase()));
            }

            if (date != null) {
                LocalDateTime from = date.atDay(1).atStartOfDay();
                LocalDateTime to = date.plusMonths(1).atDay(1).atStartOfDay();
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("eventDate"), from));
                predicates.add(criteriaBuilder.lessThan(root.get("eventDate"), to));
            }

            if (!includeInactive) {
                predicates.add(criteriaBuilder.isTrue(root.get("active")));
            }

            // Regla intuitiva:
            // - Si viene filtro por mes (date), se respeta ese mes completo.
            // - Si no viene date, includePast controla si se excluyen eventos pasados.
            if (date == null && !includePast) {
                predicates.add(criteriaBuilder.greaterThan(root.get("eventDate"), now));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private EventListResponseDTO buildEventListResponse(Page<EventDTO> eventPage, String category, String date, boolean includePast, boolean includeInactive) {
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
                        .date(date)
                        .includePast(includePast)
                        .includeInactive(includeInactive)
                        .build())
                .build();
    }

    private YearMonth parseYearMonth(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }

        try {
            return YearMonth.parse(date.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Formato de date invalido. Use YYYY-MM, por ejemplo 2026-03");
        }
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
