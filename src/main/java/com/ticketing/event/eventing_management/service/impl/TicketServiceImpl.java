package com.ticketing.event.eventing_management.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketing.event.eventing_management.config.RabbitMQConfig;
import com.ticketing.event.eventing_management.dto.TicketValidationRequestDTO;
import com.ticketing.event.eventing_management.dto.TicketValidationResponseDTO;
import com.ticketing.event.eventing_management.dto.WooCommerceOrderDTO;
import com.ticketing.event.eventing_management.entity.Event;
import com.ticketing.event.eventing_management.entity.Ticket;
import com.ticketing.event.eventing_management.exception.ResourceNotFoundException;
import com.ticketing.event.eventing_management.repository.EventRepository;
import com.ticketing.event.eventing_management.repository.TicketRepository;
import com.ticketing.event.eventing_management.service.TicketService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketServiceImpl implements TicketService {

    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    private static final String SOLD_COUNTER_KEY_PREFIX = "event:sold:";

    @Override
    @Transactional
    public String processTicketPurchase(WooCommerceOrderDTO order) throws Exception {
        if (order.getLine_items() == null || order.getLine_items().isEmpty()) {
            throw new IllegalArgumentException("La orden no contiene productos");
        }

        WooCommerceOrderDTO.LineItem item = order.getLine_items().get(0);
        Long wooProductId = item.getProduct_id();
        int quantity = item.getQuantity() == null ? 1 : item.getQuantity();
        if (quantity <= 0) {
            throw new IllegalArgumentException("La cantidad de tickets debe ser mayor a cero");
        }

        if (order.getBilling() == null || order.getBilling().getEmail() == null || order.getBilling().getEmail().isBlank()) {
            throw new IllegalArgumentException("La orden no contiene email de facturacion");
        }

        String email = order.getBilling().getEmail().trim();
        UUID attendeeId = resolveAttendeeId(order, email);
        String firstName = order.getBilling().getFirst_name() == null ? "" : order.getBilling().getFirst_name().trim();
        String lastName = order.getBilling().getLast_name() == null ? "" : order.getBilling().getLast_name().trim();
        String name = (firstName + " " + lastName).trim();
        if (name.isBlank()) {
            name = "Attendee";
        }

        Event event = eventRepository.findByWooProductId(wooProductId)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado para producto ID: " + wooProductId));

        // Control de aforo
        String key = SOLD_COUNTER_KEY_PREFIX + event.getId();
        String soldStr = redisTemplate.opsForValue().get(key);
        long sold = (soldStr != null) ? Long.parseLong(soldStr) : ticketRepository.countByEventIdAndStatus(event.getId(), Ticket.TicketStatus.ACTIVE) +
                ticketRepository.countByEventIdAndStatus(event.getId(), Ticket.TicketStatus.USED);

        if (sold + quantity > event.getCapacity()) {
            throw new IllegalStateException("Aforo completo para el evento: " + event.getTitle());
        }

        String firstTicketId = null;
        for (int i = 0; i < quantity; i++) {
            Ticket ticket = Ticket.builder()
                    .event(event)
                    .attendeeId(attendeeId)
                    .attendeeEmail(email)
                    .attendeeName(name)
                    .status(Ticket.TicketStatus.ACTIVE)
                    .build();

            ticket = ticketRepository.save(ticket);
            String qrBase64 = Base64.getEncoder().encodeToString(ticket.getId().toString().getBytes(StandardCharsets.UTF_8));
            ticket.setQrCode(qrBase64);
            ticketRepository.save(ticket);

            if (firstTicketId == null) {
                firstTicketId = ticket.getId().toString();
            }

            Map<String, Object> msg = new HashMap<>();
            msg.put("ticketId", ticket.getId().toString());
            msg.put("attendeeId", attendeeId.toString());
            msg.put("attendeeEmail", email);
            msg.put("attendeeName", name);
            msg.put("eventId", event.getId().toString());
            msg.put("eventName", event.getTitle());
            msg.put("eventDate", event.getEventDate().toString());
            msg.put("venue", event.getVenue());
            msg.put("qrCode", qrBase64);
            msg.put("seatInfo", ticket.getSeatInfo());

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.TICKET_EXCHANGE,
                    RabbitMQConfig.TICKET_SOLD_ROUTING_KEY,
                    objectMapper.writeValueAsString(msg)
            );
        }

        redisTemplate.opsForValue().set(key, String.valueOf(sold + quantity));

        log.info("{} ticket(s) creados para evento {}", quantity, event.getTitle());
        return firstTicketId;
    }

    private UUID resolveAttendeeId(WooCommerceOrderDTO order, String email) {
        if (order.getCustomer_id() != null) {
            return UUID.nameUUIDFromBytes(("wc:" + order.getCustomer_id()).getBytes(StandardCharsets.UTF_8));
        }
        return UUID.nameUUIDFromBytes(("email:" + email.toLowerCase(Locale.ROOT)).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Ticket getTicketById(UUID ticketId) {
        return ticketRepository.findByIdWithEvent(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado con id: " + ticketId));
    }

    @Override
    @Transactional
    public TicketValidationResponseDTO validateTicket(TicketValidationRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud de validacion no puede ser nula");
        }

        UUID ticketId = resolveTicketId(request);
        Ticket ticket = ticketRepository.findByIdWithEvent(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado con id: " + ticketId));

        if (request.getEventId() != null && !request.getEventId().equals(ticket.getEvent().getId())) {
            throw new IllegalArgumentException("El ticket no pertenece al evento indicado");
        }

        if (ticket.getStatus() == Ticket.TicketStatus.CANCELLED) {
            throw new IllegalStateException("El ticket esta cancelado y no puede validarse");
        }

        if (ticket.getStatus() == Ticket.TicketStatus.USED) {
            return TicketValidationResponseDTO.builder()
                    .ticketId(ticket.getId())
                    .eventId(ticket.getEvent().getId())
                    .status(ticket.getStatus().name())
                    .validatedAt(ticket.getValidatedAt())
                    .message("El ticket ya habia sido validado previamente")
                    .build();
        }

        LocalDateTime now = LocalDateTime.now();
        ticket.setStatus(Ticket.TicketStatus.USED);
        ticket.setValidatedAt(now);
        ticketRepository.save(ticket);

        publishTicketValidatedEvent(ticket, now);

        return TicketValidationResponseDTO.builder()
                .ticketId(ticket.getId())
                .eventId(ticket.getEvent().getId())
                .status(ticket.getStatus().name())
                .validatedAt(ticket.getValidatedAt())
                .message("Ticket validado correctamente")
                .build();
    }

    private UUID resolveTicketId(TicketValidationRequestDTO request) {
        if (request.getTicketId() != null) {
            return request.getTicketId();
        }

        String qrCode = request.getQrCode();
        if (qrCode == null || qrCode.isBlank()) {
            throw new IllegalArgumentException("Debe enviar ticketId o qrCode para validar");
        }

        try {
            String decoded = new String(Base64.getDecoder().decode(qrCode.trim()), StandardCharsets.UTF_8);
            return UUID.fromString(decoded);
        } catch (Exception ex) {
            throw new IllegalArgumentException("qrCode invalido. Debe ser base64 de un UUID");
        }
    }

    private void publishTicketValidatedEvent(Ticket ticket, LocalDateTime validatedAt) {
        try {
            Map<String, Object> msg = new HashMap<>();
            msg.put("ticketId", ticket.getId().toString());
            msg.put("eventId", ticket.getEvent().getId().toString());
            msg.put("validatedAt", validatedAt.toString());

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.TICKET_EXCHANGE,
                    RabbitMQConfig.TICKET_VALIDATED_ROUTING_KEY,
                    objectMapper.writeValueAsString(msg)
            );
        } catch (Exception ex) {
            log.error("No se pudo publicar ticket.validated para ticket {}", ticket.getId(), ex);
        }
    }
}
