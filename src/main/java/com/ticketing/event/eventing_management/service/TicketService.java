package com.ticketing.event.eventing_management.service;

import com.ticketing.event.eventing_management.dto.TicketValidationRequestDTO;
import com.ticketing.event.eventing_management.dto.TicketValidationResponseDTO;
import com.ticketing.event.eventing_management.dto.WooCommerceOrderDTO;
import com.ticketing.event.eventing_management.entity.Ticket;

import java.util.UUID;

public interface TicketService {
    String processTicketPurchase(WooCommerceOrderDTO order) throws Exception;
    Ticket getTicketById(UUID ticketId);
    TicketValidationResponseDTO validateTicket(TicketValidationRequestDTO request);
}
