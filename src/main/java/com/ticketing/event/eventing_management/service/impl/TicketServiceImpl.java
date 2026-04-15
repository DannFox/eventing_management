package com.ticketing.event.eventing_management.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketing.event.eventing_management.config.RabbitMQConfig;
import com.ticketing.event.eventing_management.dto.WooCommerceOrderDTO;
import com.ticketing.event.eventing_management.entity.Event;
import com.ticketing.event.eventing_management.entity.Ticket;
import com.ticketing.event.eventing_management.repository.EventRepository;
import com.ticketing.event.eventing_management.repository.TicketRepository;
import com.ticketing.event.eventing_management.service.TicketService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

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
        String email = order.getBilling().getEmail();
        String name = order.getBilling().getFirst_name() + " " + order.getBilling().getLast_name();

        Event event = eventRepository.findByWooProductId(wooProductId)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado para producto ID: " + wooProductId));

        // Control de aforo
        String key = SOLD_COUNTER_KEY_PREFIX + event.getId();
        String soldStr = redisTemplate.opsForValue().get(key);
        long sold = (soldStr != null) ? Long.parseLong(soldStr) : ticketRepository.countByEventIdAndStatus(event.getId(), Ticket.TicketStatus.ACTIVE) +
                ticketRepository.countByEventIdAndStatus(event.getId(), Ticket.TicketStatus.USED);

        if (sold >= event.getCapacity()) {
            throw new IllegalStateException("Aforo completo para el evento: " + event.getTitle());
        }

        Ticket ticket = Ticket.builder()
                .event(event)
                .attendeeEmail(email)
                .attendeeName(name)
                .status(Ticket.TicketStatus.ACTIVE)
                .build();

        ticket = ticketRepository.save(ticket);
        String qrBase64 = Base64.getEncoder().encodeToString(ticket.getId().toString().getBytes());
        ticket.setQrCode(qrBase64);
        ticketRepository.save(ticket);

        redisTemplate.opsForValue().set(key, String.valueOf(sold + item.getQuantity()));

        // Publicar a RabbitMQ
        Map<String, Object> msg = new HashMap<>();
        msg.put("ticketId", ticket.getId().toString());
        msg.put("attendeeEmail", email);
        msg.put("attendeeName", name);
        msg.put("eventId", event.getId().toString());
        msg.put("eventName", event.getTitle());
        msg.put("eventDate", event.getEventDate().toString());
        msg.put("venue", event.getVenue());
        msg.put("qrCode", qrBase64);
        msg.put("seatInfo", ticket.getSeatInfo());

        rabbitTemplate.convertAndSend(RabbitMQConfig.TICKET_EXCHANGE, RabbitMQConfig.TICKET_SOLD_ROUTING_KEY, objectMapper.writeValueAsString(msg));

        log.info("Ticket {} creado para evento {}", ticket.getId(), event.getTitle());
        return ticket.getId().toString();
    }
}
